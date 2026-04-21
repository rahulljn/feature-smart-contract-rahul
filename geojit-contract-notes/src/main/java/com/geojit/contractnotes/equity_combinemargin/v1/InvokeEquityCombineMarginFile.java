package com.geojit.contractnotes.equity_combinemargin.v1;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import com.geojit.contractnotes.equity_combinemargin.v1.Model.CHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.CustomerModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.DHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.DealingOfficeAddress;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.FooterModelV2;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.MHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.OHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.PHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SCapitalHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SFuturesHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.SSHeaderTypeModel;
import com.geojit.contractnotes.equity_combinemargin.v1.Model.STTHeaderTypeModel;
import org.json.JSONObject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.revinate.guava.util.concurrent.RateLimiter;

public class InvokeEquityCombineMarginFile implements RequestHandler<Object, String> {

    // ── constants ────────────────────────────────────────────────────
    private static final String CHUNKS_BUCKET   = "chunks-s3-geojit";
    private static final String JSON_BUCKET     = "json-s3-geojit";
    private static final String REPORT_BUCKET   = "geojit-report-files-s3";
    private static final String SQS_QUEUE_NAME  = "geojit-map-split-processing-queue.fifo";
    private static final int    PAYLOAD_LIMIT   = 256_000;   // 256 KB in bytes (AWS Lambda sync limit)
    private static final int    RATE_LIMIT      = 118;       // CreatePdf invocations / second

    // ── instance state ───────────────────────────────────────────────
    private AmazonS3 s3Client;

    // per-party DTO lists  –  MUST be instance-level and cleared on every H
    private List<CustomerModel>              customerList              = new ArrayList<>();
    private List<DealingOfficeAddress>       dealingOfficeAddressList  = new ArrayList<>();
    private List<DHeaderTypeModel>           dHeaderTypeList           = new ArrayList<>();
    private List<OHeaderTypeModel>           oHeaderTypeList           = new ArrayList<>();
    private List<FooterModelV2>              footerList                = new ArrayList<>();
    private List<SCapitalHeaderTypeModel>    sCapitalHeaderTypeList    = new ArrayList<>();
    private List<SFuturesHeaderTypeModel>    sFuturesHeaderTypeList    = new ArrayList<>();
    private List<SSHeaderTypeModel>          ssHeaderTypeList          = new ArrayList<>();
    private List<STTHeaderTypeModel>         sttHeaderTypeList         = new ArrayList<>();
    private List<MHeaderTypeModel>           mHeaderTypeList           = new ArrayList<>();
    private List<PHeaderTypeModel>           pHeaderTypeList           = new ArrayList<>();
    private List<CHeaderTypeModel>           cHeaderTypeList           = new ArrayList<>();

    // ── entry point ──────────────────────────────────────────────────
    @Override
    public String handleRequest(Object input, Context context) {

        String  bucketName = "";
        String  s3FileName = "";
        boolean sqsFlag    = true;       // true while we are still draining the queue

        // overflow file: lines that arrive after the time-window closes
        File   remainingFile   = null;
        String remainingSuffix = null;

        try {
            // ── bootstrap overflow file ──────────────────────────
            remainingSuffix = generateFileSuffix(".txt");
            remainingFile   = new File("/tmp/" + remainingSuffix);

            // ── time-window: 11 min from now ─────────────────────
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 11);
            Date nextDate = cal.getTime();

            // ── S3 client ────────────────────────────────────────
            ClientConfiguration config = new ClientConfiguration();
            config.setConnectionTimeout(900_000);
            config.setSocketTimeout(900_000);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withClientConfiguration(config).build();

            // ══════════════════════════════════════════════════════
            //  PATH A  –  poll SQS
            // ══════════════════════════════════════════════════════
            AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
            String    queueUrl  = sqsClient.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();

            List<Message> messages = sqsClient
                    .receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(1))
                    .getMessages();

            if (!messages.isEmpty()) {
                // ── extract bucket + key from the message ────────
                Message msg  = messages.get(0);
                JSONObject json = new JSONObject(msg.getBody());
                bucketName   = json.getString("bucketName");
                s3FileName   = json.getString("s3Key");
                sqsClient.deleteMessage(queueUrl, msg.getReceiptHandle());
                sqsClient.shutdown();

                System.out.println("[Invoke] processing chunk  " + bucketName + "/" + s3FileName);

                // ── download chunk to /tmp ───────────────────────
                S3Object s3Obj = s3Client.getObject(new GetObjectRequest(bucketName, s3FileName));
                File localChunk = new File("/tmp/" + s3FileName);
                try (InputStream is   = s3Obj.getObjectContent();
                     FileOutputStream fos = new FileOutputStream(localChunk)) {
                    byte[] buf = new byte[1024];
                    int   n;
                    while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                }

                // ── parse + invoke PDF (or spill) ────────────────
                processChunk(new FileInputStream(localChunk), nextDate, remainingFile);
                localChunk.delete();

            } else {
                // ══════════════════════════════════════════════════
                //  PATH B  –  queue empty  →  end-time log + throttle
                // ══════════════════════════════════════════════════
                sqsClient.shutdown();
                sqsFlag = false;

                // append end-time row to the report CSV
                ObjectMapper mapper = new ObjectMapper();
                JSONObject inputJson = new JSONObject(mapper.writeValueAsString(input));
                bucketName   = inputJson.getString("bucketName");
                s3FileName   = inputJson.getString("s3Key");

                appendEndTimeToReport(bucketName, s3FileName);

                // kick off throttling lambda
//                JSONObject throttlePayload = new JSONObject();
//                throttlePayload.put("pdftype", "equity");
//                AWSLambda lambdaClient = AWSLambdaAsyncClient.builder().build();
//                lambdaClient.invoke(new InvokeRequest()
//                        .withFunctionName("create-pdf-geojit")
//                        .withInvocationType("Event")
//                        .withPayload(throttlePayload.toString()));
//                lambdaClient.shutdown();
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            // ── upload any overflow lines back to S3 + SQS ──────
            if (remainingFile != null && remainingFile.exists() && remainingFile.length() > 0) {
                String s3Key = "ContractNoteSubfile_" + remainingSuffix;
                s3Client.putObject(new PutObjectRequest(CHUNKS_BUCKET, s3Key, remainingFile));

                JSONObject json = new JSONObject();
                json.put("bucketName", CHUNKS_BUCKET);
                json.put("s3Key", s3Key);

                AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
                try {
                    String url = sqs.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();
                    SendMessageRequest req = new SendMessageRequest(url, json.toString());
                    req.setMessageGroupId(s3Key);
                    sqs.sendMessage(req);
                } finally {
                    sqs.shutdown();
                }
                remainingFile.delete();
            }

            // ── re-invoke self if we came from SQS (chain next message) ──
            if (sqsFlag) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    AWSLambda self = AWSLambdaAsyncClient.builder().build();
                    self.invoke(new InvokeRequest()
                            .withFunctionName("invoke-lambda-geojit")
                            .withInvocationType("Event")
                            .withPayload(mapper.writeValueAsString(input)));
                    self.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (s3Client != null) s3Client.shutdown();
        }

        return "SUCCESS";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CHUNK PROCESSOR  –  reads raw lines, populates DTOs, dispatches
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Iterates every line in the chunk.  When the time-window expires
     * (nextDate has passed), remaining lines are written to the overflow
     * file instead of being parsed.  On every H record the DTO lists are
     * cleared and the previous party's payload is dispatched.
     */
    private void processChunk(InputStream input, Date nextDate, File overflowFile)
            throws IOException {

        BufferedReader reader   = new BufferedReader(new InputStreamReader(input));
        RateLimiter   limiter   = RateLimiter.create(RATE_LIMIT);
        boolean       timeUp    = false;
        String        line;

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;

            // ── time-window check ──────────────────────────────
            if (timeUp) {
                // all remaining lines go straight to the overflow file
                writeLineTo(overflowFile, line);
                continue;
            }

            String[] f = line.split("\\|");
            if (f.length < 2) continue;

            String type = f[1];

            // ── H record  =  party boundary ────────────────────
            if ("H".equals(type)) {

                // check whether the time-window just closed
                if (nextDate.before(new Date())) {
                    timeUp = true;
                    // dispatch whatever is already buffered before switching to overflow
                    dispatchPayload(limiter);
                    clearAllLists();
                    writeLineTo(overflowFile, line);
                    continue;
                }

                // dispatch the PREVIOUS party's payload (if any)
                dispatchPayload(limiter);

                // clear every list  ──  THIS IS THE BUG FIX
                clearAllLists();

                // parse the H record itself
                if (f.length == 15) {
                    customerList.add(mapH(f));
                }
                continue;
            }

            // ── all other record types ─────────────────────────
            switch (type) {
                case "A":
                    if (f.length >= 6)  dealingOfficeAddressList.add(mapA(f));
                    break;
                case "D":
                    if (f.length >= 23) dHeaderTypeList.add(mapD(f));
                    break;
                case "O":
                    if (f.length >= 13) oHeaderTypeList.add(mapO(f));
                    break;
                case "F":
                    if (f.length >= 16) footerList.add(mapF(f));
                    break;
                case "S":
                    if (f.length >= 3) {
                        if ("CAPITAL".equals(f[2])                          && f.length >= 17)
                            sCapitalHeaderTypeList.add(mapSCapital(f));
                        else if (("FUTURES".equals(f[2]) || "OPTIONS".equals(f[2])) && f.length >= 13)
                            sFuturesHeaderTypeList.add(mapSFutures(f));
                    }
                    break;
                // ── supplementary types ──────────────────────────
                case "SS":
                    if (f.length >= 11) ssHeaderTypeList.add(mapSS(f));
                    break;
                case "STT":
                    if (f.length >= 10) sttHeaderTypeList.add(mapSTT(f));
                    break;
                case "M":
                    if (f.length >= 18) mHeaderTypeList.add(mapM(f));
                    break;
                case "P":
                    if (f.length >= 7)  pHeaderTypeList.add(mapP(f));
                    break;
                case "C":
                    if (f.length >= 6)  cHeaderTypeList.add(mapC(f));
                    break;
                default:
                    break;
            }
        }

        reader.close();

        // ── flush the very last party (if time didn't expire) ─
        if (!timeUp) {
            dispatchPayload(limiter);
            clearAllLists();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PAYLOAD DISPATCH  –  size gate  →  direct invoke  OR  S3 spill
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Builds the JSON payload, measures its wire size, and either invokes
     * geojit-CreatePdfLambda directly (< 256 KB) or writes it to
     * geojit-json-s3 so that GetEquityJsonFromS3V10 picks it up via S3 event.
     */
    private void dispatchPayload(RateLimiter limiter) {
        // nothing to dispatch if no customer was parsed
        if (customerList.isEmpty()) return;

        JSONObject payload = buildPayload();
        String payloadStr = payload.toString();

        limiter.acquire();   // rate-limit every invocation

        // ── ALWAYS upload to S3 (triggers create-pdf-geojit via S3 event) ──
        System.out.println("[Invoke] uploading to S3, size=" + payloadStr.length());
        String jsonSuffix = generateFileSuffix(".json");
        String jsonKey = "cn-big-files/ContractNoteSubfile_" + jsonSuffix;
        File jsonFile = new File("/tmp/ContractNoteSubfile_" + jsonSuffix);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonFile))) {
            bw.write(payloadStr);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        s3Client.putObject(new PutObjectRequest(JSON_BUCKET, jsonKey, jsonFile));
        jsonFile.delete();

        System.out.println("[Invoke] uploaded to: " + JSON_BUCKET + "/" + jsonKey);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PAYLOAD BUILDER
    // ═══════════════════════════════════════════════════════════════════

    private JSONObject buildPayload() {
        JSONObject j = new JSONObject();
        // core lists (required by CreateEquityPdfV10)
        j.put("customerList",              customerList);
        j.put("dealingOfficeAddressList",  dealingOfficeAddressList);
        j.put("dHeaderTypeList",           dHeaderTypeList);
        j.put("oHeaderTypeList",           oHeaderTypeList);
        j.put("footerList",                footerList);
        j.put("sCapitalHeaderTypeList",    sCapitalHeaderTypeList);
        j.put("sFuturesHeaderTypeList",    sFuturesHeaderTypeList);
        // supplementary lists (future-proof; PDF ignores if not needed)
        j.put("ssHeaderTypeList",          ssHeaderTypeList);
        j.put("sttHeaderTypeList",         sttHeaderTypeList);
        j.put("mHeaderTypeList",           mHeaderTypeList);
        j.put("pHeaderTypeList",           pHeaderTypeList);
        j.put("cHeaderTypeList",           cHeaderTypeList);
        return j;
    }

    /** Clears every DTO list.  Called on every H-record boundary. */
    private void clearAllLists() {
        customerList.clear();
        dealingOfficeAddressList.clear();
        dHeaderTypeList.clear();
        oHeaderTypeList.clear();
        footerList.clear();
        sCapitalHeaderTypeList.clear();
        sFuturesHeaderTypeList.clear();
        ssHeaderTypeList.clear();
        sttHeaderTypeList.clear();
        mHeaderTypeList.clear();
        pHeaderTypeList.clear();
        cHeaderTypeList.clear();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  END-TIME REPORT APPEND
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Downloads the start-time CSV, appends an end-time row, re-uploads.
     * Mirrors the Angel One end-time logic exactly.
     */
    private void appendEndTimeToReport(String bucket, String key) throws IOException {
        // download
        S3Object obj = s3Client.getObject(new GetObjectRequest(bucket, key));
        String[] parts = key.split("\\/");
        String localName = parts[parts.length - 1];
        File local = new File("/tmp/" + localName);

        try (InputStream is  = obj.getObjectContent();
             FileOutputStream fos = new FileOutputStream(local)) {
            byte[] buf = new byte[1024]; int n;
            while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
        }

        // append end-time row
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("IST"));
        try (CSVWriter w = new CSVWriter(new FileWriter(local, true))) {
            w.writeNext(new String[]{"equity-EndTime", sdf.format(new Date())});
        }

        // re-upload
        s3Client.putObject(new PutObjectRequest(bucket, key, local));
        local.delete();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DTO MAPPERS  –  Geojit field layout, NEW DTO STRUCTURE
    // ═══════════════════════════════════════════════════════════════════

    /** H  – 15 fields */
    private static CustomerModel mapH(String[] f) {
        CustomerModel m = new CustomerModel();
        m.setPartycode(f[0].trim());
        m.setHeaderType(f[1]);
        m.setClientCode(f[2]);
        m.setName(f[3]);
        m.setAddress1(f[4]);
        m.setAddress2(f[5]);
        m.setAddress3(f[6]);
        m.setContractNo(f[7]);
        m.setPanNo(f[8]);
        m.setTransactionDate(f[9]);
        m.setEmail(f[10]);
        m.setMobileNo(f[11]);
        m.setGstNo(f[12]);
        m.setIrn(f[13]);
        m.setBuinessType(f[14]);
        return m;
    }

    /** A  – 6 fields */
    private static DealingOfficeAddress mapA(String[] f) {
        DealingOfficeAddress m = new DealingOfficeAddress();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setDealingAddress(f[2]);
        m.setGstLocation(f[3]);
        m.setDealingOfficeNo(f[4]);
        m.setGstNo(f[5]);
        return m;
    }

    /** D  – 23 fields */
    private static DHeaderTypeModel mapD(String[] f) {
        DHeaderTypeModel m = new DHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setExchange(f[2]);
        m.setSegment(f[3]);
        m.setOrderno(f[4]);
        m.setOrder_time(f[5]);
        m.setTrade_no(f[6]);
        m.setTrade_time(f[7]);
        m.setSecurity_contract_description(f[8]);
        m.setBuy_sell(f[9]);
        m.setQty(f[10]);
        m.setGross_rate_fc(f[11]);
        m.setMarket_rate(f[12]);
        m.setBrokerage(f[13]);
        m.setNet_rate(f[14]);
        m.setClosing_rate(f[15]);
        m.setNet_total(f[16]);
        m.setRemark(f[17]);
        m.setExchangeID(f[18]);
        m.setSettlementNo(f[19]);
        m.setSettlementdate(f[20]);
        m.setSegment2(f[21]);
        m.setBroker_code(f[22]);
        return m;
    }

    /**
     * O  – 13 fields
     * NEW DTO STRUCTURE - Updated field names
     */
    private static OHeaderTypeModel mapO(String[] f) {
        OHeaderTypeModel m = new OHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setSegment(f[2]);
        m.setSecurityDescription(f[3]);     // CHANGED: was security_contract_description
        m.setSegment2(f[4]);
        m.setBuyQty(f[5]);                  // CHANGED: was buy_qty
        m.setBuyRate(f[6]);                 // CHANGED: was buy_rate
        m.setSellQty(f[7]);                 // CHANGED: was sell_qty
        m.setSellRate(f[8]);                // CHANGED: was sell_rate
        m.setNetQty(f[9]);                  // CHANGED: was net_qty
        m.setNetRate(f[10]);                // CHANGED: was net_rate
        m.setAmount(f[11]);
        m.setStt(f[12]);                    // CHANGED: was security_txn_tax
        return m;
    }

    /** F  – 16 fields  (defaults already set in FooterModelV2 for PDF-only fields) */
    private static FooterModelV2 mapF(String[] f) {
        FooterModelV2 m = new FooterModelV2();
        m.setPartycode(f[0].trim());
        m.setFooter_type(f[1]);
        m.setExchange(f[2]);
        m.setSegment(f[3]);
        m.setInstrument(f[4]);
        m.setPay_in_pay_out_obligation(f[5]);
        m.setSecurities_transaction_tax(f[6]);
        m.setSgst(f[7]);
        m.setCgst(f[8]);
        m.setIgst(f[9]);
        m.setTds(f[10]);
        m.setExchange_transaction_charges(f[11]);
        m.setSebi_fee(f[12]);
        m.setAdd_cess(f[13]);
        m.setStampduty(f[14]);
        m.setNet_amount(f[15]);
        return m;
    }

    /**
     * S CAPITAL  – 17 fields
     * NEW DTO STRUCTURE - Updated field names with camelCase
     */
    private static SCapitalHeaderTypeModel mapSCapital(String[] f) {
        SCapitalHeaderTypeModel m = new SCapitalHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setSegment(f[2]);
        m.setIsin(f[3]);
        m.setSecurityDescription(f[4]);           // CHANGED: was symbol
        m.setBuyQty(f[5]);                        // CHANGED: was buy_qty
        m.setBuyWap(f[6]);                        // CHANGED: was buy_wap
        m.setBuyBrokerage(f[7]);                  // CHANGED: was buy_brok
        m.setBuyWapAfterBrokerage(f[8]);          // CHANGED: was wap_across_exc_buy
        m.setBuyValue(f[9]);                      // CHANGED: was buy_val
        m.setSellQty(f[10]);                      // CHANGED: was sell_qty
        m.setSellWap(f[11]);                      // CHANGED: was sell_wap
        m.setSellBrokerage(f[12]);                // CHANGED: was sell_brok
        m.setSellWapAfterBrokerage(f[13]);        // CHANGED: was wap_across_exc_sell
        m.setSellValue(f[14]);                    // CHANGED: was sell_val
        m.setNetQty(f[15]);                       // CHANGED: was net_qty
        m.setNetObligationIsin(f[16]);            // CHANGED: was net_obligation
        return m;
    }

    /**
     * S FUTURES / OPTIONS  – 13 fields
     * NEW DTO STRUCTURE - Updated field names with 'trade' prefix
     */
    private static SFuturesHeaderTypeModel mapSFutures(String[] f) {
        SFuturesHeaderTypeModel m = new SFuturesHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setSegment(f[2]);
        m.setContractDesc(f[3]);                  // CHANGED: was desc
        m.setTradeType(f[4]);                     // CHANGED: was buyOrSell
        m.setTradeQty(f[5]);                      // CHANGED: was qty
        m.setTradeWapFc(f[6]);                    // CHANGED: was wap_foreign
        m.setTradeWap(f[7]);                      // CHANGED: was wap
        m.setTradeBrokerage(f[8]);                // CHANGED: was brokerage
        m.setTradeWapAfterBrokerage(f[9]);        // CHANGED: was wap_brok
        m.setTradeClosingRate(f[10]);             // CHANGED: was closing_rate
        m.setTradeNetTotal(f[11]);                // CHANGED: was total
        m.setRemarks(f[12]);
        return m;
    }

    // ── supplementary mappers ──────────────────────────────────────────

    /**
     * SS  – 11 fields
     * NEW DTO STRUCTURE - Updated field names
     */
    private static SSHeaderTypeModel mapSS(String[] f) {
        SSHeaderTypeModel m = new SSHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setSecurityDescription(f[2]);           // CHANGED: was scripName
        m.setTradeType(f[3]);                     // CHANGED: was buyOrSell
        m.setTradeQty(f[4]);                      // CHANGED: was qty
        m.setGrossRate(f[5]);                     // CHANGED: was wap
        m.setGrossTotal(f[6]);                    // CHANGED: was totalValue
        m.setGrossBrokerage(f[7]);                // CHANGED: was brokerage
        m.setBrokerage(f[8]);                     // CHANGED: was netBrokerage
        m.setNetRate(f[9]);                       // CHANGED: was wap_brok
        m.setNetAmount(f[10]);                    // CHANGED: was total
        return m;
    }

    /**
     * STT  – 10 fields for NEW simplified structure
     * OLD structure had 14 fields (equity) or 18 fields (derivatives)
     * NEW structure: partycode, STT, securityDescription, exchange, segment,
     *                expDate, futureSale, futureStt, optionSale, optionStt, totalStt
     */
    private static STTHeaderTypeModel mapSTT(String[] f) {
        STTHeaderTypeModel m = new STTHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setSecurityDescription(f[2]);           // CHANGED: was scripName
        m.setExchange(f[3]);
        m.setSegment(f[4]);
        m.setExpDate(f[5]);                       // CHANGED: was qtyOrExpiry
        m.setFutureSale(f[6]);                    // CHANGED: was buyRate
        m.setFutureStt(f[7]);                     // CHANGED: was buySTT
        m.setOptionSale(f[8]);                    // NEW field
        m.setOptionStt(f[9]);                     // NEW field

        // Handle optional totalStt field
        if (f.length > 10) {
            m.setTotalStt(f[10]);                 // CHANGED: was totalSTT
        }

        return m;
    }

    /** M  – 18 fields */
    private static MHeaderTypeModel mapM(String[] f) {
        MHeaderTypeModel m = new MHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setExchange(f[2]);
        m.setSegment(f[3]);
        m.setDate(f[4]);
        m.setPayInPayOut(f[5]);
        m.setMtmProfit(f[6]);
        m.setPremium(f[7]);
        m.setOptionValue(f[8]);
        m.setAssignmentValue(f[9]);
        m.setTotal(f[10]);
        m.setDebits(f[11]);
        m.setTdsFund(f[12]);
        m.setOtherDebits(f[13]);
        m.setTotalDebits(f[14]);
        m.setBalance(f[15]);
        m.setPreviousBalance(f[16]);
        m.setNetBalance(f[17]);
        return m;
    }

    /**
     * P  – 7 fields
     * NEW DTO STRUCTURE - Updated field names
     */
    private static PHeaderTypeModel mapP(String[] f) {
        PHeaderTypeModel m = new PHeaderTypeModel();
        m.setPartycode(f[0].trim());
        m.setHeadertype(f[1]);
        m.setSecurityDescription(f[2]);           // CHANGED: was scripName
        m.setQty(f[3]);
        m.setTotalValue(f[4]);                    // CHANGED: was value
        m.setHaircutValue(f[5]);                  // CHANGED: was purchaseValue
        m.setBalanceAmount(f[6]);                 // CHANGED: was unrealizedGain
        return m;
    }

    /**
     * C  – two variants: 6 fields (SEBI) or 12 fields (MTM).
     * chargeType is f[2] which distinguishes them.
     */
    private static CHeaderTypeModel mapC(String[] f) {
        CHeaderTypeModel m = new CHeaderTypeModel();

        // basic
        if (f.length > 0) m.setPartycode(f[0].trim());
        if (f.length > 1) m.setHeadertype(f[1].trim());
        if (f.length > 4) m.setSegment(f[4].trim());
        if (f.length > 5) m.setScrip_name(f[5].trim());

        // buy side
        if (f.length > 6) m.setBuy_qty(f[6].trim());
        if (f.length > 7) m.setBuy_market_rate(f[7].trim());

        // sell side
        if (f.length > 8) m.setSell_qty(f[8].trim());
        if (f.length > 9) m.setSell_market_rate(f[9].trim());

        // charges
        if (f.length > 10) m.setBrokerage(f[10].trim());
        if (f.length > 11) m.setStt(f[11].trim());
        if (f.length > 12) m.setOther_charges(f[12].trim());
        if (f.length > 13) m.setGrand_total(f[13].trim());

        return m;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ═══════════════════════════════════════════════════════════════════

    private static void writeLineTo(File f, String line) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
            bw.write(line);
            bw.newLine();
        }
    }

    private static String generateFileSuffix(String ext) {
        int rand = new Random().nextInt(1000);
        return new SimpleDateFormat(rand + "_yyyyMMddHHmmssSSS'" + ext + "'").format(new Date());
    }
}
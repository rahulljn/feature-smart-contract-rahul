//package com.geojit.contractnotes.geojit_ContractNote.V2;
//
//import com.amazonaws.AmazonServiceException;
//import com.amazonaws.ClientConfiguration;
//import com.amazonaws.SdkClientException;
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.lambda.AWSLambda;
//import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
//import com.amazonaws.services.lambda.model.InvokeRequest;
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import com.amazonaws.services.s3.model.S3Object;
//import com.amazonaws.services.s3.model.S3ObjectInputStream;
//import com.amazonaws.services.sqs.AmazonSQS;
//import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
//import com.amazonaws.services.sqs.model.Message;
//import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
//import com.amazonaws.services.sqs.model.SendMessageRequest;
//import com.amazonaws.services.sqs.model.SendMessageResult;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.geojit.contractnotes.DTO.GeojitStatementDTO;
//import com.geojit.contractnotes.Model.*;
//import com.revinate.guava.util.concurrent.RateLimiter;
//import org.json.JSONObject;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.security.SecureRandom;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//public class Invoke2 implements RequestHandler<Object, String> {
//    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
//    private static final String CHUNKS_BUCKET = "chunks-s3-geojit-prod";
//    private static final String BIG_FILES_PREFIX = "cn-big-files/";
//    private static final String SQS_QUEUE_NAME = "geojit-map-split-processing-queue.fifo";
//    private static final String PDF_LAMBDA_NAME = "create-pdf-geojit";
//    private static final String GET_JSON_LAMBDA_NAME = "json-lambda-geojit";
//    private static final Regions AWS_REGION = Regions.AP_SOUTH_1;
//    private static final String DELIMITER = "~";
//
//    private List<HeaderDto> headerList = new ArrayList<>();
//    private List<ExchangeClearingDto> exchangeList = new ArrayList<>();
//    private List<EquitySegmentDto> positionList = new ArrayList<>();
//    private List<DerivativeSegmentDto> vList = new ArrayList<>();
//    private List<NameClearingCorporationDto> detailList = new ArrayList<>();
//    private List<PayInPayOutDto> obligationList = new ArrayList<>();
//    private List<DatePlaceDto> footerList = new ArrayList<>();
//    private List<NameAndExchangeTotalDto> noteList = new ArrayList<>();
//    private List<NetObligationDto> marginList = new ArrayList<>();
//    private List<NetObligationTotalDto> totalList = new ArrayList<>();
//    private List<ScripSummaryDto> uList = new ArrayList<>();
//    private List<SecurityTransactionDto> contractList = new ArrayList<>();
//    private List<SecurityTransactionTotalDto> roundedTotalList = new ArrayList<>();
//    private List<CashSegmentTotalDto> amountList = new ArrayList<>();
//    private List<CashSegmentDto> lotList = new ArrayList<>();
//    private List<DailyMarginDto> generalList = new ArrayList<>();
//    private List<DailyMarginTotalDto> journalList = new ArrayList<>();
//    private List<MarginPledgeDto> securityList = new ArrayList<>();
//    private List<MarginPledgeTotalDto> quantityList = new ArrayList<>();
//
////    private Integer fileCount = 0;
//    private String s3fileName = null;
//
//    AmazonS3 s3Client;
//    String fileSuffix = "";
//    String filePath = "";
//    File remainingRecordsFile = null;
//
//
//    private static final int H_LEN = 13;
//    private static final int E_LEN = 7;
//    private static final int P_LEN = 16;
//    private static final int V_LEN = 12;
//    private static final int O_LEN = 14;
//    private static final int F_LEN = 4;
//    private static final int N_LEN = 14;
//    private static final int D_LEN = 16;
//    private static final int M_LEN = 12;
//    private static final int T_LEN = 5;
//    private static final int U_LEN = 11;
//    private static final int C_LEN = 18;
//    private static final int R_LEN = 3;
//    private static final int A_LEN = 5;
//    private static final int L_LEN = 10;
//    private static final int G_LEN = 17;
//    private static final int J_LEN = 15;
//    private static final int K_LEN = 7;
//    private static final int Q_LEN = 6;
//
//    @Override
//    public String handleRequest(Object object, Context context) {
//        S3Object fullObject = null;
//        String bucketName = "";
//        Boolean sqsFlag = true;
//
//        try {
//            int randNumber = SECURE_RANDOM.nextInt(1000);
//            fileSuffix = new SimpleDateFormat(randNumber + "_yyyyMMddHHmmssSSS'.txt'").format(new Date());
//            filePath = "/tmp/" + fileSuffix;
//            remainingRecordsFile = new File(filePath);   // always initialised before finally
//
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(new Date());
//            calendar.add(Calendar.MINUTE, 11);
//            Date nextDate = calendar.getTime();
//
//            final AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(AWS_REGION).build();
//            String sqsQueueUrl = sqsClient.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();
//            final ReceiveMessageRequest receiveMessageRequest =
//                    new ReceiveMessageRequest(sqsQueueUrl).withMaxNumberOfMessages(1);
//            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).getMessages();
//
//            if (messages.size() > 0) {
//                for (final Message message : messages) {
//                    JSONObject json = new JSONObject(message.getBody());
//                    bucketName = json.getString("bucketName");
//                    s3fileName = json.getString("s3Key");
//                    sqsClient.deleteMessage(sqsQueueUrl, message.getReceiptHandle());
//                }
//                sqsClient.shutdown();
//
//                ClientConfiguration config = new ClientConfiguration();
//                config.setConnectionTimeout(900000);
//                config.setSocketTimeout(900000);
//
//                s3Client = AmazonS3ClientBuilder.standard()
//                        .withClientConfiguration(config).withRegion(AWS_REGION).build();
//
//                fullObject = s3Client.getObject(new GetObjectRequest(bucketName, s3fileName));
//                S3ObjectInputStream s3is = fullObject.getObjectContent();
//                String downloadFilePath = "/tmp/" + s3fileName;
//                FileOutputStream fos = new FileOutputStream(new File(downloadFilePath));
//                byte[] read_buf = new byte[1024];
//                int read_len;
//                while ((read_len = s3is.read(read_buf)) > 0) {
//                    fos.write(read_buf, 0, read_len);
//                }
//                fos.close();
//                s3is.close();
//
//                File newfile = new File(downloadFilePath);
//                InputStream targetStream = new FileInputStream(newfile);
//                processStatementFile(targetStream, nextDate);
//                if (!newfile.delete()) {
//                    System.err.println("Failed to delete: " + newfile.getAbsolutePath());
//                }
//
//            } else {
//                sqsFlag = false;
//                sqsClient.shutdown();
//                try {
//                    JSONObject pullPayload = new JSONObject();
//                    pullPayload.put("type", "geojit");
//
//                    AWSLambda pullClient = AWSLambdaAsyncClient.builder()
//                            .withRegion(AWS_REGION)
//                            .build();
//
//                    InvokeRequest pullDeliveryRequest = new InvokeRequest()
//                            .withFunctionName("pull-delivery-geojit")
//                            .withInvocationType("Event")
//                            .withPayload(pullPayload.toString());
//                    pullClient.invoke(pullDeliveryRequest);
//
//                    InvokeRequest pullBounceRequest = new InvokeRequest()
//                            .withFunctionName("pull-bounce-geojit")
//                            .withInvocationType("Event")
//                            .withPayload(pullPayload.toString());
//                    pullClient.invoke(pullBounceRequest);
//
//                    pullClient.shutdown();
//                    System.out.println("PullDelivery and PullBounce triggered successfully");
//
//                } catch (Exception e) {
//                    System.err.println("Error triggering Pull lambdas: " + e.getMessage());
//                }
//            }
//
//
//        } catch (AmazonServiceException e) {
//            System.err.println("AWS Service Exception: " + e.getMessage());
//            e.printStackTrace();
//        } catch (SdkClientException e) {
//            System.err.println("AWS SDK Exception: " + e.getMessage());
//            e.printStackTrace();
//        } catch (IOException e) {
//            System.err.println("IO Exception: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            JSONObject jsonObject = new JSONObject();
//
//            // FIX: guard both remainingRecordsFile null AND s3Client null
//            if (remainingRecordsFile != null && remainingRecordsFile.exists()
//                    && remainingRecordsFile.length() > 0 && s3Client != null) {
//
//                String subFileSuffix = "StatementSubfile_" + fileSuffix;
//                s3Client.putObject(new PutObjectRequest(CHUNKS_BUCKET, subFileSuffix, remainingRecordsFile));
//
//                jsonObject.put("bucketName", CHUNKS_BUCKET);
//                jsonObject.put("s3Key", subFileSuffix);
//
//                final AmazonSQS sqsClient2 = AmazonSQSClientBuilder.standard().withRegion(AWS_REGION).build();
//                final String sqsQueueUrl2 = sqsClient2.getQueueUrl(SQS_QUEUE_NAME).getQueueUrl();
//                final SendMessageRequest sendMessageRequest = new SendMessageRequest(sqsQueueUrl2, jsonObject.toString());
//                sendMessageRequest.setMessageGroupId(subFileSuffix);
//                // FIX: FIFO queue requires MessageDeduplicationId
//                sendMessageRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
//                final SendMessageResult sendMessageResult = sqsClient2.sendMessage(sendMessageRequest);
//                System.out.println("SendMessage succeed with messageId " + sendMessageResult.getMessageId()
//                        + ", sequence number " + sendMessageResult.getSequenceNumber());
//                sqsClient2.shutdown();
//                if (!remainingRecordsFile.delete()) {
//                    System.err.println("Warning: Could not delete remaining records file: "
//                            + remainingRecordsFile.getAbsolutePath());
//                }
//            }
//
//            // FIX: only invoke self if we actually processed a file (sqsFlag true) AND s3Client is not null
//            if (sqsFlag && s3Client != null) {
//                jsonObject.put("key", "next call");
//                AWSLambda client = AWSLambdaAsyncClient.builder().withRegion(AWS_REGION).build();
//                InvokeRequest request = new InvokeRequest()
//                        .withFunctionName("invoke-lambda-geojit")
//                        .withInvocationType("Event")
//                        .withPayload(jsonObject.toString());
//                client.invoke(request);
//                client.shutdown();
////                fileCount = 0;
//                s3Client.shutdown();
//            }
//
//            if (fullObject != null) {
//                try {
//                    fullObject.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("Geojit Invoke Lambda Completed");
//        }
//        return "successful";
//    }
//
//
//    private void processStatementFile(InputStream targetStream, Date nextDate) {
//        try {
//            Integer count256kb = 0;
//            Integer count256kbPlus = 0;
//
//            Boolean allOKFlag = true, flagH = true, flagE = true, flagP = true, flagV = true, flagD = true,
//                    flagO = true, flagF = true, flagN = true, flagM = true, flagT = true, flagU = true,
//                    flagC = true, flagR = true, flagA = true, flagL = true, flagG = true, flagJ = true,
//                    flagK = true, flagQ = true;
//
//            List<String> lineArraylist = new ArrayList<>();
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(targetStream, StandardCharsets.UTF_8));
//            RateLimiter rateLimiter = RateLimiter.create(80);
//            String line;
//            boolean beforeNextDate = true;
//
//            while ((line = reader.readLine()) != null) {
//
//                if (beforeNextDate) {
//
//                    String[] strFields = line.split(DELIMITER, -1);
//
//                    // ================= HEADER =================
//                    if (strFields.length >= 2 && strFields[1].equals("H")) {
//
//                        if (!lineArraylist.isEmpty()) {
//
//                            if (allOKFlag) {
//                                count256kb = invokePdfLambdaWithSizeCheck(rateLimiter, count256kb, count256kbPlus);
//                            } else {
//                                for (String lineStr : lineArraylist) {
//                                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
//                                            new FileOutputStream(new File("/tmp/Failurefile.txt"), true), StandardCharsets.UTF_8));
//                                    bw.write(lineStr);
//                                    bw.newLine();
//                                    bw.close();
//                                }
//                            }
//                        }
//
//                        if (!nextDate.after(new Date())) {
//                            beforeNextDate = false;
//                            writeToRemainingFile(line);
//                            continue;
//                        }
//
//                        if (strFields.length == H_LEN) {
//                            allOKFlag = true;
//                            resetCustomerData();
//                            lineArraylist.clear();
//
//                            flagH = flagE = flagP = flagV = flagD = flagO = flagF = flagN =
//                                    flagM = flagT = flagU = flagC = flagR = flagA =
//                                            flagL = flagG = flagJ = flagK = flagQ = true;
//
//                            HeaderDto h = insertHeader(strFields);
//                            if (h != null) headerList.add(h);
//                        } else {
//                            flagH = false;
//                        }
//                    }
//
//                    // ================= RECORD TYPES =================
//                    if (strFields.length >= 2 && strFields[1].equals("E") && flagE) {
//                        if (strFields.length == E_LEN) {
//                            ExchangeClearingDto e = insertExchange(strFields);
//                            if (e != null) exchangeList.add(e);
//                        } else flagE = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("P") && flagP) {
//                        if (strFields.length == P_LEN) {
//                            EquitySegmentDto p = insertPosition(strFields);
//                            if (p != null) positionList.add(p);
//                        } else flagP = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("V") && flagV) {
//                        if (strFields.length == V_LEN) {
//                            DerivativeSegmentDto v = insertV(strFields);
//                            if (v != null) vList.add(v);
//                        } else flagV = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("D") && flagD) {
//                        if (strFields.length == D_LEN) {
//                            NameClearingCorporationDto d = insertDetail(strFields);
//                            if (d != null) detailList.add(d);
//                        } else flagD = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("O") && flagO) {
//                        if (strFields.length == O_LEN) {
//                            PayInPayOutDto o = insertObligation(strFields);
//                            if (o != null) obligationList.add(o);
//                        } else flagO = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("F") && flagF) {
//                        if (strFields.length == F_LEN) {
//                            DatePlaceDto ft = insertFooter(strFields);
//                            if (ft != null) footerList.add(ft);
//                        } else flagF = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("N") && flagN) {
//                        if (strFields.length == N_LEN) {
//                            NameAndExchangeTotalDto n = insertNote(strFields);
//                            if (n != null) noteList.add(n);
//                        } else flagN = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("M") && flagM) {
//                        if (strFields.length == M_LEN) {
//                            NetObligationDto m = insertMargin(strFields);
//                            if (m != null) marginList.add(m);
//                        } else flagM = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("T") && flagT) {
//                        if (strFields.length == T_LEN) {
//                            NetObligationTotalDto t = insertTotal(strFields);
//                            if (t != null) totalList.add(t);
//                        } else flagT = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("U") && flagU) {
//                        if (strFields.length == U_LEN) {
//                            ScripSummaryDto u = insertU(strFields);
//                            if (u != null) uList.add(u);
//                        } else flagU = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("C") && flagC) {
//                        if (strFields.length == C_LEN) {
//                            SecurityTransactionDto c = insertContract(strFields);
//                            if (c != null) contractList.add(c);
//                        } else flagC = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("R") && flagR) {
//                        if (strFields.length == R_LEN) {
//                            SecurityTransactionTotalDto r = insertRoundedTotal(strFields);
//                            if (r != null) roundedTotalList.add(r);
//                        } else flagR = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("A") && flagA) {
//                        if (strFields.length == A_LEN) {
//                            CashSegmentTotalDto a = insertAmount(strFields);
//                            if (a != null) amountList.add(a);
//                        } else flagA = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("L") && flagL) {
//                        if (strFields.length == L_LEN) {
//                            CashSegmentDto l = insertLot(strFields);
//                            if (l != null) lotList.add(l);
//                        } else flagL = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("G") && flagG) {
//                        if (strFields.length == G_LEN) {
//                            DailyMarginDto g = insertGeneral(strFields);
//                            if (g != null) generalList.add(g);
//                        } else flagG = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("J") && flagJ) {
//                        if (strFields.length == J_LEN) {
//                            DailyMarginTotalDto j = insertJournal(strFields);
//                            if (j != null) journalList.add(j);
//                        } else flagJ = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("K") && flagK) {
//                        if (strFields.length == K_LEN) {
//                            MarginPledgeDto s = insertSecurity(strFields);
//                            if (s != null) securityList.add(s);
//                        } else flagK = false;
//                    }
//
//                    if (strFields.length >= 2 && strFields[1].equals("Q") && flagQ) {
//                        if (strFields.length == Q_LEN) {
//                            MarginPledgeTotalDto q = insertQuantity(strFields);
//                            if (q != null) quantityList.add(q);
//                        } else flagQ = false;
//                    }
//
//                    if (flagH && flagE && flagP && flagV && flagD && flagO && flagF && flagN &&
//                            flagM && flagT && flagU && flagC && flagR && flagA && flagL &&
//                            flagG && flagJ && flagK && flagQ) {
//                        lineArraylist.add(line);
//                    } else {
//                        allOKFlag = false;
//                        lineArraylist.add(line);
//                    }
//
//                } else {
//                    writeToRemainingFile(line);
//                }
//            }
//
//            reader.close();
//            targetStream.close();
//
//            // ============ LAST CUSTOMER ============
//            if (!lineArraylist.isEmpty() && beforeNextDate) {
//
//                if (allOKFlag) {
//                    count256kb = invokePdfLambdaWithSizeCheck(rateLimiter, count256kb, count256kbPlus);
//                    resetCustomerData();
//                } else {
//                    for (String lineStr : lineArraylist) {
//                        BufferedWriter bw = new BufferedWriter(
//                                new OutputStreamWriter(
//                                        new FileOutputStream(new File("/tmp/Failurefile.txt"), true),
//                                        StandardCharsets.UTF_8));
//                        bw.write(lineStr);
//                        bw.newLine();
//                        bw.close();
//                    }
//                }
//            }
//
//            System.out.println("Invoke end time: " + new Date());
//
//        } catch (IOException e) {
//            System.err.println("IO Error in processStatementFile: " + e.getMessage());
//            e.printStackTrace();
//        } catch (RuntimeException e) {
//            System.err.println("Runtime Error in processStatementFile: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private Integer invokePdfLambdaWithSizeCheck(RateLimiter rateLimiter, Integer count256kb, Integer count256kbPlus) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            GeojitStatementDTO dto = createStatementDTO();
//            resetCustomerData();
//            String payload = mapper.writeValueAsString(dto);
//            Integer payloadLength = 36 + payload.length() * 2;
//            rateLimiter.acquire();
//            if (payloadLength < 256000) {
//                count256kb++;
//                final String fp = payload;
//                AWSLambda cl = AWSLambdaAsyncClient.builder()
//                        .withRegion(AWS_REGION)
//                        .build();
//
//                try {
//                    cl.invoke(new InvokeRequest()
//                            .withFunctionName(PDF_LAMBDA_NAME)
//                            .withInvocationType("Event")
//                            .withPayload(payload));
//                } catch (Exception e) {
//                    System.err.println("Error invoking PDF Lambda: " + e.getMessage());
//                } finally {
//                    cl.shutdown();
//                }
//            } else {
//                count256kbPlus++;
//                System.out.println("S3-based invocation #" + count256kbPlus + " (payload: " + payloadLength + " bytes)");
//
//                String jsonFileSuffix = "StatementSubfile_" + new SimpleDateFormat(SECURE_RANDOM.nextInt(1000) + "_yyyyMMddHHmmssSSS'.json'").format(new Date());
//                File jsonFile = new File("/tmp/" + jsonFileSuffix);
//                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonFile, true), StandardCharsets.UTF_8));
//                bw.write(payload);
//                bw.close();
//                s3Client.putObject(new PutObjectRequest(CHUNKS_BUCKET, BIG_FILES_PREFIX + jsonFileSuffix, jsonFile));
//                boolean jsonDeleted = jsonFile.delete();
//                if (!jsonDeleted) {
//                    System.err.println("Warning: Could not delete file: " + jsonFile.getName());
//                }
//
//                // ✅ YE NAYA CODE ADD KARO — GetJsonLambda ko trigger karo
//                JSONObject getJsonPayload = new JSONObject();
//                getJsonPayload.put("bucketName", CHUNKS_BUCKET);
//                getJsonPayload.put("s3Key", BIG_FILES_PREFIX + jsonFileSuffix);
//
//                AWSLambda getJsonClient = AWSLambdaAsyncClient.builder()
//                        .withRegion(AWS_REGION)
//                        .build();
//                getJsonClient.invoke(new InvokeRequest()
////                        ← apna actual Lambda function name yahan
//                        .withFunctionName(GET_JSON_LAMBDA_NAME)
//                        .withInvocationType("Event")
//                        .withPayload(getJsonPayload.toString()));
//                getJsonClient.shutdown();
//
//            }
//
//        } catch (IOException e) {
//            System.err.println("IO Error in invokePdfLambdaWithSizeCheck: " + e.getMessage());
//            e.printStackTrace();
//        } catch (RuntimeException e) {
//            System.err.println("Runtime Error in invokePdfLambdaWithSizeCheck: " + e.getMessage());
//            e.printStackTrace();
//        }
//        return count256kb;
//    }
//
//    private GeojitStatementDTO createStatementDTO() {
//        GeojitStatementDTO dto = new GeojitStatementDTO();
//        if (!headerList.isEmpty()) dto.setHeader(headerList.get(0));
//        dto.setExchanges(new ArrayList<>(exchangeList));
//        dto.setPositions(new ArrayList<>(positionList));
//        dto.setVRecords(new ArrayList<>(vList));
//        dto.setDetails(new ArrayList<>(detailList));
//        dto.setObligations(new ArrayList<>(obligationList));
//        if (!footerList.isEmpty()) dto.setFooter(footerList.get(0));
//        dto.setNotes(new ArrayList<>(noteList));
//        dto.setMargins(new ArrayList<>(marginList));
//        if (!totalList.isEmpty()) dto.setTotal(totalList.get(0));
//        dto.setURecords(new ArrayList<>(uList));
//        dto.setContracts(new ArrayList<>(contractList));
//        if (!roundedTotalList.isEmpty()) dto.setRoundedTotal(roundedTotalList.get(0));
//        dto.setAmounts(new ArrayList<>(amountList));
//        dto.setLots(new ArrayList<>(lotList));
//        dto.setGenerals(new ArrayList<>(generalList));
//        if (!journalList.isEmpty()) dto.setJournal(journalList.get(0));
//        dto.setSecurities(new ArrayList<>(securityList));
//        if (!quantityList.isEmpty()) dto.setQuantity(quantityList.get(0));
//        return dto;
//    }
//
//    private void resetCustomerData() {
//        headerList.clear();
//        exchangeList.clear();
//        positionList.clear();
//        vList.clear();
//        detailList.clear();
//        obligationList.clear();
//        footerList.clear();
//        noteList.clear();
//        marginList.clear();
//        totalList.clear();
//        uList.clear();
//        contractList.clear();
//        roundedTotalList.clear();
//        amountList.clear();
//        lotList.clear();
//        generalList.clear();
//        journalList.clear();
//        securityList.clear();
//        quantityList.clear();
//    }
//
//    private void writeToRemainingFile(String line) throws IOException {
//        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
//                new FileOutputStream(remainingRecordsFile, true), StandardCharsets.UTF_8));
//        bw.write(line);
//        bw.newLine();
//        bw.close();
//    }
//
//    // ========== DATA INSERTION METHODS ==========
//    private HeaderDto insertHeader(String[] f) {
//        try {
//            HeaderDto h = new HeaderDto();
//            h.setUniqueId(f[0]);
//            h.setRecordType(f[1]);
//            h.setContractNoteNo(f[2]);
//            h.setTradeDate(f[3]);
//            h.setNameOfClient(f[4]);
//            h.setAddress(f[5]);
//            h.setPhoneNo(f[6]);
//            h.setTradeCode(f[7]);
//            h.setPlaceOfSupply(f[8]);
//            h.setInvoiceReferenceNumber(f[9]);
//            h.setGstIdentificationNo(f[10]);
//            h.setPanOfClient(f[11]);
//            h.setEmail(f[12]);
//            return h;
//        } catch (Exception e) {
//            System.err.println("#Exception HeaderDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private ExchangeClearingDto insertExchange(String[] f) {
//        try {
//            ExchangeClearingDto e = new ExchangeClearingDto();
//            e.setUniqueId(f[0]);
//            e.setRecordType(f[1]);
//            e.setExchangeClearingCorporation(f[2]);
//            e.setSegment(f[3]);
//            e.setSettlementNo(f[4]);
//            e.setSettlementDate(f[5]);
//            e.setUcCode(f[6]);
//            return e;
//        } catch (Exception ex) {
//            System.err.println("#Exception ExchangeDto: " + ex.getMessage());
//            return null;
//        }
//    }
//
//    private EquitySegmentDto insertPosition(String[] f) {
//        try {
//            EquitySegmentDto p = new EquitySegmentDto();
//            p.setUniqueId(f[0]);
//            p.setRecordType(f[1]);
//            p.setIsin(f[2]);
//            p.setSecurityNameSymbol(f[3]);
//            p.setBuyQuantity(f[4]);
//            p.setBuyWAP(f[5]);
//            p.setBuyBrokeragePerShare(f[6]);
//            p.setBuyWAPAfterBrokerage(f[7]);
//            p.setTotalBuy(f[8]);
//            p.setSellQuantity(f[9]);
//            p.setSellWAP(f[10]);
//            p.setSellBrokeragePerShare(f[11]);
//            p.setSellWAPAfterBrokerage(f[12]);
//            p.setTotalSellValueAfterBrokerage(f[13]);
//            p.setNetQuantity(f[14]);
//            p.setNetObligation(f[15]);
//            return p;
//        } catch (Exception e) {
//            System.err.println("#Exception PositionDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private DerivativeSegmentDto insertV(String[] f) {
//        try {
//            DerivativeSegmentDto v = new DerivativeSegmentDto();
//            v.setUniqueId(f[0]);
//            v.setRecordType(f[1]);
//            v.setContractDescription(f[2]);
//            v.setBuySell(f[3]);
//            v.setQuantity(f[4]);
//            v.setWapPerUnitForeignCurrency(f[5]);
//            v.setWapPerUnitRs(f[6]);
//            v.setBrokeragePerUnitRs(f[7]);
//            v.setWapPerUnitAfterBrokerageRs(f[8]);
//            v.setClosingRatePerUnit(f[9]);
//            v.setNetTotal(f[10]);
//            v.setRemarks(f[11]);
//            return v;
//        } catch (Exception e) {
//            System.err.println("#Exception VDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private NameClearingCorporationDto insertDetail(String[] f) {
//        try {
//            NameClearingCorporationDto d = new NameClearingCorporationDto();
//            d.setUniqueId(f[0]);
//            d.setRecordType(f[1]);
//            d.setOrderNo(f[2]);
//            d.setOrderTime(f[3]);
//            d.setTradeNo(f[4]);
//            d.setTradeTime(f[5]);
//            d.setSecurityContractDescription(f[6]);
//            d.setBuySell(f[7]);
//            d.setQuantity(f[8]);
//            d.setGrossRatePricePerUnitForeignCcy(f[9]);
//            d.setGrossRatePricePerUnitRs(f[10]);
//            d.setBrokerageRs(f[11]);
//            d.setNetRatePerUnitRs(f[12]);
//            d.setClosingRatePerUnit(f[13]);
//            d.setNetTotalBeforeLevies(f[14]);
//            d.setRemarks(f[15]);
//            return d;
//        } catch (Exception e) {
//            System.err.println("#Exception DetailDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private PayInPayOutDto insertObligation(String[] f) {
//        try {
//            PayInPayOutDto o = new PayInPayOutDto();
//            o.setUniqueId(f[0]);
//            o.setRecordType(f[1]);
//            o.setNameOfExchangeSegment(f[2]);
//            o.setPayInPayOut(f[3]);
//            o.setSecuritiesTransactions(f[4]);
//            o.setSgst(f[5]);
//            o.setCgst(f[6]);
//            o.setIgst(f[7]);
//            o.setTds(f[8]);
//            o.setExchangeTransactionCharges(f[9]);
//            o.setSebiTurnover(f[10]);
//            o.setAdditionalCess(f[11]);
//            o.setStampDuty(f[12]);
//            o.setNetAmountReceivableByClient(f[13]);
//            return o;
//        } catch (Exception e) {
//            System.err.println("#Exception ObligationDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private DatePlaceDto insertFooter(String[] f) {
//        try {
//            DatePlaceDto ft = new DatePlaceDto();
//            ft.setUniqueId(f[0]);
//            ft.setRecordType(f[1]);
//            ft.setDate(f[2]);
//            ft.setPlace(f[3]);
//            return ft;
//        } catch (Exception e) {
//            System.err.println("#Exception FooterDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private NameAndExchangeTotalDto insertNote(String[] f) {
//        try {
//            NameAndExchangeTotalDto n = new NameAndExchangeTotalDto();
//            n.setUniqueId(f[0]);
//            n.setRecordType(f[1]);
//            n.setNameOfExchange(f[2]);
//            n.setSegment(f[3]);
//            n.setSecurityContractDescription(f[4]);
//            n.setBuySell(f[5]);
//            n.setQuantity(f[6]);
//            n.setGrossRateForeignCurrency(f[7]);
//            n.setGrossRateRs(f[8]);
//            n.setBrokerage(f[9]);
//            n.setNetRate(f[10]);
//            n.setClosingRate(f[11]);
//            n.setNetTotal(f[12]);
//            n.setRemarks(f[13]);
//            return n;
//        } catch (Exception e) {
//            System.err.println("#Exception NoteDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private NetObligationDto insertMargin(String[] f) {
//        try {
//            NetObligationDto m = new NetObligationDto();
//            m.setUniqueId(f[0]);
//            m.setRecordType(f[1]);
//            m.setSlNo(f[2]);
//            m.setSecurity(f[3]);
//            m.setSegment(f[4]);
//            m.setQuantity1(f[5]);
//            m.setRate1(f[6]);
//            m.setQuantity2(f[7]);
//            m.setRate2(f[8]);
//            m.setQuantity3(f[9]);
//            m.setRate3(f[10]);
//            m.setAmount(f[11]);
//            return m;
//        } catch (Exception e) {
//            System.err.println("#Exception MarginDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private NetObligationTotalDto insertTotal(String[] f) {
//        try {
//            NetObligationTotalDto t = new NetObligationTotalDto();
//            t.setUniqueId(f[0]);
//            t.setRecordType(f[1]);
//            t.setTotal(f[2]);
//            t.setSecuritiesTransactionsTax(f[3]);
//            t.setNetAmount(f[4]);
//            return t;
//        } catch (Exception e) {
//            System.err.println("#Exception TotalDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private ScripSummaryDto insertU(String[] f) {
//        try {
//            ScripSummaryDto u = new ScripSummaryDto();
//            u.setUniqueId(f[0]);
//            u.setRecordType(f[1]);
//            u.setSecurityDescription(f[2]);
//            u.setBuySell(f[3]);
//            u.setQuantity(f[4]);
//            u.setGrossRatePerSecurity(f[5]);
//            u.setGrossTotalRs(f[6]);
//            u.setGrossBrokeragePerSecurity(f[7]);
//            u.setBrokerageTotal(f[8]);
//            u.setNetRateRs(f[9]);
//            u.setNetTotalAmountRs(f[10]);
//            return u;
//        } catch (Exception e) {
//            System.err.println("#Exception UDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private SecurityTransactionDto insertContract(String[] f) {
//        try {
//            SecurityTransactionDto c = new SecurityTransactionDto();
//            c.setUniqueId(f[0]);
//            c.setRecordType(f[1]);
//            c.setSlNo(f[2]);
//            c.setSecurity(f[3]);
//            c.setSegment(f[4]);
//            c.setQuantity1(f[5]);
//            c.setPrice1(f[6]);
//            c.setValue1(f[7]);
//            c.setStt1(f[8]);
//            c.setQuantity2(f[9]);
//            c.setPrice2(f[10]);
//            c.setValue2(f[11]);
//            c.setStt2(f[12]);
//            c.setQuantity3(f[13]);
//            c.setPrice3(f[14]);
//            c.setValue3(f[15]);
//            c.setStt3(f[16]);
//            c.setTotalSttRs(f[17]);
//            return c;
//        } catch (Exception e) {
//            System.err.println("#Exception ContractDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private SecurityTransactionTotalDto insertRoundedTotal(String[] f) {
//        try {
//            SecurityTransactionTotalDto r = new SecurityTransactionTotalDto();
//            r.setUniqueId(f[0]);
//            r.setRecordType(f[1]);
//            r.setTotalRoundedToNearestRupee(f[2]);
//            return r;
//        } catch (Exception e) {
//            System.err.println("#Exception RoundedTotalDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private CashSegmentTotalDto insertAmount(String[] f) {
//        try {
//            CashSegmentTotalDto a = new CashSegmentTotalDto();
//            a.setUniqueId(f[0]);
//            a.setRecordType(f[1]);
//            a.setNameOfExchange(f[2]);
//            a.setSegment(f[3]);
//            a.setTotalRoundedToNearestRupee(f[4]);
//            return a;
//        } catch (Exception e) {
//            System.err.println("#Exception AmountDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private CashSegmentDto insertLot(String[] f) {
//        try {
//            CashSegmentDto l = new CashSegmentDto();
//            l.setUniqueId(f[0]);
//            l.setRecordType(f[1]);
//            l.setSlNo(f[2]);
//            l.setSecurity(f[3]);
//            l.setExpiryDate(f[4]);
//            l.setSale1(f[5]);
//            l.setStt1(f[6]);
//            l.setSale2(f[7]);
//            l.setStt2(f[8]);
//            l.setTotalSttRs(f[9]);
//            return l;
//        } catch (Exception e) {
//            System.err.println("#Exception LotDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private DailyMarginDto insertGeneral(String[] f) {
//        try {
//            DailyMarginDto g = new DailyMarginDto();
//            g.setUniqueId(f[0]);
//            g.setRecordType(f[1]);
//            g.setSeq(f[2]);
//            g.setTradeDay(f[3]);
//            g.setFunds(f[4]);
//            g.setValueOfSecurities(f[5]);
//            g.setValueOfMarginPledgeSecurities(f[6]);
//            g.setBankGuaranteesFdr(f[7]);
//            g.setAnyOtherApprovedFormOfMargins(f[8]);
//            g.setTotalMarginsAvailable(f[9]);
//            g.setTotalUpfrontMargin(f[10]);
//            g.setConsolidatedCrystallised(f[11]);
//            g.setDeliveryMargin(f[12]);
//            g.setTotalRequirement(f[13]);
//            g.setExcessShortfall(f[14]);
//            g.setAdditionalMarginsRequiredByMemberAsPerRMS(f[15]);
//            g.setMarginStatusBalanceWithMember(f[16]);
//            return g;
//        } catch (Exception e) {
//            System.err.println("#Exception GeneralDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private DailyMarginTotalDto insertJournal(String[] f) {
//        try {
//            DailyMarginTotalDto j = new DailyMarginTotalDto();
//            j.setUniqueId(f[0]);
//            j.setRecordType(f[1]);
//            j.setFunds(f[2]);
//            j.setValueOfSecurities(f[3]);
//            j.setValueOfMarginPledgeSecurities(f[4]);
//            j.setBankGuaranteesFdr(f[5]);
//            j.setAnyOtherApprovedFormOfMargins(f[6]);
//            j.setTotalMarginsAvailable(f[7]);
//            j.setTotalUpfrontMargin(f[8]);
//            j.setConsolidatedCrystallised(f[9]);
//            j.setDeliveryMargin(f[10]);
//            j.setTotalRequirement(f[11]);
//            j.setExcessShortfall(f[12]);
//            j.setAdditionalMarginsRequiredByMemberAsPerRMS(f[13]);
//            j.setMarginStatusBalanceWithMember(f[14]);
//            return j;
//        } catch (Exception e) {
//            System.err.println("#Exception JournalDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private MarginPledgeDto insertSecurity(String[] f) {
//        try {
//            MarginPledgeDto s = new MarginPledgeDto();
//            s.setUniqueId(f[0]);
//            s.setRecordType(f[1]);
//            s.setSecurity(f[2]);
//            s.setQty(f[3]);
//            s.setTotalValue(f[4]);
//            s.setHairCutValue(f[5]);
//            s.setBalanceAmount(f[6]);
//            return s;
//        } catch (Exception e) {
//            System.err.println("#Exception SecurityDto: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private MarginPledgeTotalDto insertQuantity(String[] f) {
//        try {
//            MarginPledgeTotalDto q = new MarginPledgeTotalDto();
//            q.setUniqueId(f[0]);
//            q.setRecordType(f[1]);
//            q.setQty(f[2]);
//            q.setTotalValue(f[3]);
//            q.setHairCutValue(f[4]);
//            q.setBalanceAmount(f[5]);
//            return q;
//        } catch (Exception e) {
//            System.err.println("#Exception QuantityDto: " + e.getMessage());
//            return null;
//        }
//    }
//}
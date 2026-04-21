package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class EmailNotification implements RequestHandler<S3Event, String> {
    private static String s3fileName = null;
    private static final String STATUS_QUEUE_NAME = "geojit-pipeline-status-queue.fifo";
    String bucketName = "";
    private static final Logger logger = LoggerFactory.getLogger(EmailNotification.class);

    // ── Template cache (survives across warm Lambda invocations) ─────────
    private static final String TEMPLATE_BUCKET  = "geojit-email-templates";
    private static final String TEMPLATE_KEY     = "active/contract-note.html";
    private static final long   CACHE_TTL_MS     = 15 * 60 * 1000L; // 15 minutes
    private static volatile String cachedHtml    = null;
    private static volatile long   cacheExpiry   = 0;


    @Override
    public String handleRequest(S3Event s3event, Context context) {

        SimpleDateFormat istFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        istFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        logger.info("EmailNotification Started | startTime={}", istFormat.format(new Date()));

        try {
            S3EventNotificationRecord record = s3event.getRecords().get(0);
            bucketName = record.getS3().getBucket().getName();
            s3fileName = record.getS3().getObject().getUrlDecodedKey();
            String[] fileName = s3fileName.split("\\/");
            // create s3 client
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object s3PdfFile = s3Client.getObject(new GetObjectRequest(bucketName, s3fileName));

            // get metadata of s3 file object
            ObjectMetadata objectMetadata = s3PdfFile.getObjectMetadata();
            Map<String, String> userMetadataMap = objectMetadata.getUserMetadata();
            S3ObjectInputStream s3is = s3PdfFile.getObjectContent();
            String filePath = "/tmp/" + fileName[fileName.length - 1];
            FileOutputStream fos = new FileOutputStream(new File(filePath));
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            fos.close();
            s3is.close();

            String geojitflag = userMetadataMap.get("geojitflag");

            // call send mail function to send mail
            if (geojitflag != null && geojitflag.equals("false")) {
                File newfile = new File(filePath);
                newfile.delete();
                s3Client.shutdown();
            } else {

                sendMailSES(filePath, userMetadataMap, fileName[fileName.length - 1]);
                File newfile = new File(filePath);
                newfile.delete();
                s3Client.shutdown();
            }

        } catch (Exception e) {
            logger.error("EmailNotification ERROR | file={} | error={} | endTime={}",
                    s3fileName, e.getMessage(), istFormat.format(new Date()), e);
        } finally {
            // ADD THIS at the end in finally
            logger.info("EmailNotification Completed | file={} | endTime={}",
                    s3fileName, istFormat.format(new Date()));
        }
        return null;

    }

    private void sendMailSES(String filePath, Map<String, String> userMetadataMap, String string) {
        // TODO Auto-generated method stub

        String json = null;
        JSONObject metadataJson = null;
        try {

            long startDateMilli = (new Date()).getTime(); //
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("IST"));
            String activityDate = dateFormatter.format(startDateMilli);
            // Read meta data of PDF file
            ObjectMapper objectMapper = new ObjectMapper();
            json = objectMapper.writeValueAsString(userMetadataMap);
            metadataJson = new JSONObject(json);
            metadataJson.put("activityDate", activityDate);
            String email = metadataJson.optString("email");
            String partycode = metadataJson.optString("partycode");
            String jobid = userMetadataMap.get("jobid");  // read jobId from S3 metadata
            if (jobid != null) metadataJson.put("jobid", jobid);  // propagate to metajson
            String[] arrOfStr = email.split("@");

            String htmlTemplate = "";
            String subject = " ";

            // put value in json which required for report
            metadataJson.put("type", "geojit");
            String documentNo = "GCNGEOJIT" + activityDate + partycode;
            metadataJson.put("documentNo", documentNo);
            metadataJson.put("documentType", "Contract Note");
            metadataJson.put("exchange", "EQUITY-COMBINEMARGIN");
            metadataJson.put("activitydate", activityDate);
            metadataJson.put("fileFormat", ".pdf");
            metadataJson.put("logineUser", "Admin");
            metadataJson.put("sourceFile", s3fileName);
            metadataJson.put("dematlogId", "");
            metadataJson.put("clientType", "1");

            htmlTemplate = resolveTemplate(metadataJson.optString("name"), s3Client);
            subject = "Contract Note - Geojit Investments Ltd";

            // sending mail

            if (email != null && !(email.isEmpty()) && (!arrOfStr[1].equalsIgnoreCase("geojit.co.in"))) {
                AmazonSES amazonSES = new AmazonSES();

                String result = amazonSES.sendMailWithHeader(email, subject, htmlTemplate, filePath, metadataJson);

                // Send EMAIL_SENT status event
                sendEmailStatusEvent(jobid, partycode, result, null);

            } else {
                // push to SQS when email not found
                metadataJson.put("documentState", "BOUNCED");
                metadataJson.put("boReason", "email not found");
                metadataJson.put("emailStatus", "failed");

                final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
                String sqsQueueUrl = sqsClient.getQueueUrl("geojit-sent-email-queue.fifo").getQueueUrl();
                final SendMessageRequest sendMessageRequest = new SendMessageRequest(sqsQueueUrl,
                        metadataJson.toString());
                sendMessageRequest.setMessageGroupId(partycode);
                final SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);
                final String sequenceNumber = sendMessageResult.getSequenceNumber();
                final String messageId = sendMessageResult.getMessageId();
//                System.out.println("SendMessage succeed with messageId " + messageId + ", sequence number "
//                        + sequenceNumber + "\n");
                sqsClient.shutdown();
            }

        } catch (Exception e) {
            e.printStackTrace();

            // Send EMAIL_FAILED status event
            String jobid = userMetadataMap != null ? userMetadataMap.get("jobid") : null;
            String partycode = metadataJson != null ? metadataJson.optString("partycode") : null;
            sendEmailStatusEvent(jobid, partycode, null, e.getMessage());

            // Exception fallback: push to SQS
            if (metadataJson != null) {
                try {
                    long fallbackMilli = (new Date()).getTime();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    df.setTimeZone(TimeZone.getTimeZone("IST"));
                    String fallbackDate = df.format(fallbackMilli);
                    partycode = metadataJson.optString("partycode");

                    metadataJson.put("type", "geojit");
                    metadataJson.put("documentNo", "GCNGEOJIT" + fallbackDate + partycode);
                    metadataJson.put("documentType", "Contract Note");
                    metadataJson.put("exchange", "EQUITY-COMBINEMARGIN");
                    metadataJson.put("activitydate", fallbackDate);
                    metadataJson.put("fileFormat", ".pdf");
                    metadataJson.put("logineUser", "Admin");
                    metadataJson.put("sourceFile", s3fileName);
                    metadataJson.put("dematlogId", "");
                    metadataJson.put("clientType", "1");
                    metadataJson.put("emailStatus", "failed");
                    metadataJson.put("boReason", e.getMessage());
                    metadataJson.put("reason", e.getMessage());

                    final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
                    String sqsQueueUrl = sqsClient.getQueueUrl("geojit-sent-email-queue.fifo").getQueueUrl();
                    final SendMessageRequest sendMessageRequest = new SendMessageRequest(sqsQueueUrl,
                            metadataJson.toString());
                    sendMessageRequest.setMessageGroupId(partycode);
                    final SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);
                    final String sequenceNumber = sendMessageResult.getSequenceNumber();
                    final String messageId = sendMessageResult.getMessageId();
//                    System.out.println("SendMessage succeed with messageId " + messageId + ", sequence number "
//                            + sequenceNumber + "\n");
                    sqsClient.shutdown();
                } catch (Exception sqsEx) {
                    logger.error("fallback SQS push failed: " + sqsEx.getMessage());
                }
            }
        }
    }

    /**
     * Fetches the email HTML template from S3 with a 15-minute in-memory cache.
     * Each Lambda container performs at most one S3 read per 15 minutes, regardless
     * of how many emails it processes. The [NAME] placeholder is substituted with
     * the actual customer name at call time.
     *
     * Falls back to the hardcoded template if S3 is unreachable.
     */
    private String resolveTemplate(String name, AmazonS3 s3Client) {
        long now = System.currentTimeMillis();
        if (cachedHtml == null || now > cacheExpiry) {
            try {
                S3Object obj = s3Client.getObject(TEMPLATE_BUCKET, TEMPLATE_KEY);
                S3ObjectInputStream content = obj.getObjectContent();
                cachedHtml = new String(content.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                content.close();
                obj.close();
                cacheExpiry = now + CACHE_TTL_MS;
                logger.info("Template loaded from S3 | bucket={} | key={}", TEMPLATE_BUCKET, TEMPLATE_KEY);
            } catch (Exception e) {
                logger.warn("S3 template fetch failed, falling back to hardcoded template: {}", e.getMessage());
                return getHtmlTemplate(name);
            }
        }
        String customerName = (name != null && !name.isBlank()) ? name : "Customer";
        return cachedHtml.replace("[NAME]", customerName);
    }

    public static String getHtmlTemplate(String name) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n"
                + "</head>\n"
                + "<body style=\"font-family: Arial, sans-serif; font-size: 10pt; color: #333;\">\n"
                + "Dear " + name + ",\n"
                + "<br/><br/>\n"
                + "Warm Greetings from Geojit Investments Ltd !\n"
                + "<br/><br/>\n"
                + "We hope your experience with Geojit Investments Ltd has been pleasant.\n"
                + "We are herewith sending you your digitally signed contract note (PDF Document).\n"
                + "<br/><br/>\n"
                + "To open your attachment you require Adobe Acrobat Reader 6.0 or above or Foxit Reader.\n"
                + "<br/><br/>\n"
                + "<strong>Instructions for Opening the attachment:-</strong>\n"
                + "<br/><br/>\n"
                + "1. Click on the attachment provided with this mail. "
                + "If you are prompted for a password, please follow the below steps.\n"
                + "<br/><br/>\n"
                + "<strong>INDIVIDUAL</strong> clients may enter the first four characters of your PAN "
                + "(in CAPITAL letters) followed by first four characters of your DATE OF BIRTH (DOB) "
                + "[in DDMM format] as the password for the PDF attachment.\n"
                + "<br/>\n"
                + "For Eg. PAN: BDPBV2015Z and DOB: 31.01.1979 then password will be <strong>BDPB3101</strong>\n"
                + "<br/><br/>\n"
                + "For <strong>NON-INDIVIDUAL</strong> clients you may enter your PAN "
                + "(in CAPITAL letters) as the password for the PDF attachment.\n"
                + "<br/>\n"
                + "For Eg. PAN: BDPBV2015Z then password will be <strong>BDPBV2015Z</strong>\n"
                + "<br/><br/>\n"
                + "2. To view details regarding the digital signature, please click on the icon of a pen, "
                + "on the left hand side frame of Adobe acrobat.\n"
                + "<br/><br/>\n"
                + "<strong>Security Notice:</strong> We will never ask for your login ID, password, or OTP. "
                + "Please refrain from sharing this information with anyone. "
                + "Your security is our top priority.\n"
                + "<br/><br/>\n"
                + "To download Adobe Reader, please visit "
                + "<a href=\"http://get.adobe.com/reader/otherversions\">"
                + "http://get.adobe.com/reader/otherversions</a>\n"
                + "<br/>\n"
                + "To download Foxit Reader, please visit "
                + "<a href=\"http://www.foxitsoftware.com/downloads/\">"
                + "http://www.foxitsoftware.com/downloads/</a>\n"
                + "<br/><br/>\n"
                + "For all queries, kindly contact "
                + "<a href=\"mailto:customercare@geojit.com\">customercare@geojit.com</a>.\n"
                + "<br/>\n"
                + "Toll Free No: 1800-571-5501, 1800-103-5501. Paid Line: +91-484-3911777\n"
                + "<br/><br/>\n"
                + "<pre style=\"font-family: monospace; font-size: 9pt; color: #666;\">"
                + "---------------------------------------------------------------------------\n"
                + "The information contained in this electronic message and its attachments (the \"message\")\n"
                + "is intended solely for the addressees and is confidential and privileged.\n"
                + "If you are not the intended recipient, please notify the sender by reply e-mail\n"
                + "and then destroy the message. Any dissemination, distribution, forwarding, copying,\n"
                + "printing or disclosure, either whole or partial, is prohibited and may be unlawful.\n"
                + "Equity/Mutual Fund investments are subject to market risks.\n"
                + "Past performance does not guarantee future returns.\n"
                + "We do not offer any product which gives guaranteed returns.\n"
                + "WARNING: Computer viruses can be transmitted via email. The recipient should check\n"
                + "this email and any attachments for the presence of viruses. The company accepts no\n"
                + "liability for any damage caused by any virus transmitted by this email.\n"
                + "-----------------------------------------------------------------------"
                + "</pre>\n"
                + "</body>\n"
                + "</html>\n";
    }

    private void sendEmailStatusEvent(String jobid, String partycode, String sesMessageId, String error) {
        if (jobid == null) return;
        try {
            boolean isSuccess = (error == null);
            JSONObject payload = new JSONObject();
            if (isSuccess) {
                payload.put("sesMessageId", sesMessageId);
            } else {
                payload.put("error", error);
            }

            JSONObject event = new JSONObject();
            event.put("jobId", jobid);
            event.put("eventType", isSuccess ? "EMAIL_SENT" : "EMAIL_FAILED");
            event.put("lambdaName", "email-notification-geojit");
            event.put("partyCode", partycode);
            event.put("payload", payload);
            event.put("eventTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));

            AmazonSQS sqsClient = AmazonSQSClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
            String queueUrl = sqsClient.getQueueUrl(STATUS_QUEUE_NAME).getQueueUrl();
            SendMessageRequest msgRequest = new SendMessageRequest(queueUrl, event.toString());
            msgRequest.setMessageGroupId("status-" + jobid);
            msgRequest.setMessageDeduplicationId(UUID.randomUUID().toString());
            sqsClient.sendMessage(msgRequest);
            sqsClient.shutdown();

            logger.info("{} event sent | jobId={} | partyCode={}", isSuccess ? "EMAIL_SENT" : "EMAIL_FAILED", jobid, partycode);
        } catch (Exception e) {
            logger.error("Error sending email status event: {}", e.getMessage(), e);
        }
    }

}
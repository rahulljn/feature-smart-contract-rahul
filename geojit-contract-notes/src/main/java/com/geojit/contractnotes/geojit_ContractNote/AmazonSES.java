package com.geojit.contractnotes.geojit_ContractNote;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.AmazonSimpleEmailServiceException;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class AmazonSES {

    static final String FROM     = "noreply@geojit.co.in";
    static final String FROMNAME = "GEOJIT";
    static String CONFIGSET      = "geojit-config-set";

    private static final Logger logger = LoggerFactory.getLogger(AmazonSES.class);

    static final String TEXTBODY = "This email was sent through Amazon SES "
            + "using the AWS SDK for Java.";

    // ═══════════════════════════════════════════════════════════════════════
    //  SEND MAIL (basic — no metadata header)
    // ═══════════════════════════════════════════════════════════════════════
    public String sendMail(String emailId, String subject, String body, String filePath) {
        try {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);
            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(FROM, FROMNAME));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(emailId));

            MimeMultipart msg_body = new MimeMultipart("alternative");
            MimeBodyPart wrap      = new MimeBodyPart();
            MimeBodyPart htmlPart  = new MimeBodyPart();
            htmlPart.setContent(body, "text/html; charset=UTF-8");
            msg_body.addBodyPart(htmlPart);
            wrap.setContent(msg_body);

            MimeMultipart msg = new MimeMultipart("mixed");
            message.setContent(msg);
            msg.addBodyPart(wrap);

            if (!filePath.equals("")) {
                MimeBodyPart att = new MimeBodyPart();
                DataSource fds   = new FileDataSource(filePath);
                att.setDataHandler(new DataHandler(fds));
                att.setFileName(fds.getName());
                msg.addBodyPart(att);
            }

            AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder
                    .standard().withRegion(Regions.AP_SOUTH_1).build();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)
                    .withConfigurationSetName(CONFIGSET);
            sesClient.sendRawEmail(rawEmailRequest);

            logger.info("Email sent | email={}", emailId);
            sesClient.shutdown();
            return "successful";

        } catch (AmazonSimpleEmailServiceException ex) {
            logger.error("Email not sent | email={} | error={}", emailId, ex.getMessage(), ex);
            return ex.getMessage();
        } catch (Exception ex) {
            logger.error("Email not sent | email={} | error={}", emailId, ex.getMessage(), ex);
            return ex.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SEND MAIL WITH HEADER (main method — used by EmailNotification)
    // ═══════════════════════════════════════════════════════════════════════
    public String sendMailWithHeader(String emailId, String subject, String body,
                                     String filePath, JSONObject metadataJson) {
        try {
            Session session  = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);

            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(FROM, FROMNAME));
            message.setRecipients(RecipientType.TO,
                    InternetAddress.parse(emailId.replaceAll("\\s+", "")));

            String bcc = metadataJson.optString("bcc");
            String cc  = metadataJson.optString("cc");

            // ── Config set selection ──────────────────────────────────────
            if (metadataJson.optString("type").equals("geojit")) {
                CONFIGSET = "geojit-config-set";
            } else if (metadataJson.optString("pdftype").equals("commodity")) {
                CONFIGSET = "commodity-contract-note-set";
            } else if (metadataJson.optString("pdftype").equals("commcn")) {
                CONFIGSET = "commcn-contract-note-set";
            } else if (metadataJson.optString("pdftype").equals("bill")) {
                CONFIGSET = "bill-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("dpledger")) {
                CONFIGSET = "dpledger-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("agts")) {
                CONFIGSET = "agts-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("dpholdingyearly")) {
                CONFIGSET = "dpholdingyearly-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("stt")) {
                CONFIGSET = "stt-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("pnl")) {
                CONFIGSET = "pnl-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("dptrade")) {
                CONFIGSET = "dptrade-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("ros")) {
                CONFIGSET = "ros-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("qsledger")) {
                CONFIGSET = "qsledger-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("qsretention")) {
                CONFIGSET = "qsretention-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("dpholding")) {
                CONFIGSET = "dpholding-contract-note-set";
            } else if (metadataJson.optString("exchange").equals("dmr")) {
                CONFIGSET = "dmr-contract-note-set";
            }

            if (!bcc.isEmpty()) {
                message.setRecipients(RecipientType.BCC, InternetAddress.parse(bcc));
            }
            if (!cc.isEmpty()) {
                message.setRecipients(RecipientType.CC, InternetAddress.parse(cc));
            }
            message.setHeader("metajson", metadataJson.toString());

            MimeMultipart msg_body = new MimeMultipart("alternative");
            MimeBodyPart wrap      = new MimeBodyPart();
            MimeBodyPart htmlPart  = new MimeBodyPart();
            htmlPart.setContent(body, "text/html; charset=UTF-8");
            msg_body.addBodyPart(htmlPart);
            wrap.setContent(msg_body);

            MimeMultipart msg = new MimeMultipart("mixed");
            message.setContent(msg);
            msg.addBodyPart(wrap);

            if (!filePath.equals("")) {
                MimeBodyPart att = new MimeBodyPart();
                DataSource fds   = new FileDataSource(filePath);
                att.setDataHandler(new DataHandler(fds));
                att.setFileName(fds.getName());
                msg.addBodyPart(att);
            }

            AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder
                    .standard().withRegion(Regions.AP_SOUTH_1).build();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)
                    .withConfigurationSetName(CONFIGSET);
            sesClient.sendRawEmail(rawEmailRequest);

            // FIX: was logger.error("Email sent!") — wrong level, no context
            // This is the ONLY line to search in CSV for exact successful email count
            logger.info("Email sent for customer | partyCode={} | email={}",
                    metadataJson.optString("partycode"), emailId);

            sesClient.shutdown();
            return "successful";

        } catch (Exception ex) {
            String partyCode = metadataJson.optString("partycode");

            if (ex.getMessage() != null && (
                    ex.getMessage().contains("Illegal character in domain")
                            || ex.getMessage().contains("Illegal character in local name")
                            || ex.getMessage().contains("Domain starts with dot")
                            || ex.getMessage().contains("Domain ends with dot")
                            || ex.getMessage().contains("Invalid domain name")
                            || ex.getMessage().contains("Domain contains illegal character")
                            || ex.getMessage().contains("Domain contains dot-dot"))) {

                // Invalid email address — log and return, no SQS push
                logger.error("Email not sent | invalid email address | partyCode={} | email={} | error={}",
                        partyCode, emailId, ex.getMessage(), ex);
                return ex.getMessage();

            } else {
                // All other failures — push to SQS bounce queue
                logger.error("Email not sent | partyCode={} | email={} | error={}",
                        partyCode, emailId, ex.getMessage(), ex);

                try {
                    long startDateMilli = new Date().getTime();
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    dateFormatter.setTimeZone(TimeZone.getTimeZone("IST"));
                    String activityDate = dateFormatter.format(startDateMilli);

                    metadataJson.put("activityDate", activityDate);
                    metadataJson.put("boReason",     ex.getMessage());
                    metadataJson.put("emailStatus",  "failed");

                    final AmazonSQS sqsClient = AmazonSQSClientBuilder.defaultClient();
                    String sqsQueueUrl;

                    if (metadataJson.optString("pdftype").equals("commodity")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("commodity-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("pdftype").equals("commcn")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("common-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("pdftype").equals("bill")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("bill-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("dpledger")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("dp-weekly-ledger-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("agts")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("agts-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("dpholdingyearly")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("dpholdingyearly-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("stt")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("stt-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("pnl")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("pnl-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("dptrade")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("dptrade-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("ros")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("ros-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("qsledger")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("qs-ledger-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("qsretention")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("qsretention-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("emailer")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("staic-dyanamic-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("exchange").equals("dmr")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("dmr-throttling-queue.fifo").getQueueUrl();
                    } else if (metadataJson.optString("type").equals("geojit")) {
                        sqsQueueUrl = sqsClient.getQueueUrl("geojit-sent-email-queue.fifo").getQueueUrl();
                    } else {
                        sqsQueueUrl = sqsClient.getQueueUrl("dpholding-throttling-queue.fifo").getQueueUrl();
                    }

                    final SendMessageRequest sendMessageRequest =
                            new SendMessageRequest(sqsQueueUrl, metadataJson.toString());
                    sendMessageRequest.setMessageGroupId(partyCode);
                    final SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);

                    logger.info("Bounce pushed to SQS | partyCode={} | messageId={} | sequenceNumber={}",
                            partyCode,
                            sendMessageResult.getMessageId(),
                            sendMessageResult.getSequenceNumber());

                    sqsClient.shutdown();

                } catch (Exception sqsEx) {
                    logger.error("SQS bounce push FAILED | partyCode={} | error={}",
                            partyCode, sqsEx.getMessage(), sqsEx);
                }

                return ex.getMessage();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SEND MAIL WITH HEADER + CC
    // ═══════════════════════════════════════════════════════════════════════
    public String sendMailWithHeaderwithCC(String emailId, String subject, String body,
                                           String filePath, JSONObject metadataJson) {
        try {
            Session session  = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session);

            message.setSubject(subject, "UTF-8");
            message.setFrom(new InternetAddress(FROM, FROMNAME));
            message.setRecipients(RecipientType.TO, InternetAddress.parse(emailId));
            message.setHeader("metajson", metadataJson.toString());

            MimeMultipart msg_body = new MimeMultipart("alternative");
            MimeBodyPart wrap      = new MimeBodyPart();
            MimeBodyPart htmlPart  = new MimeBodyPart();
            htmlPart.setContent(body, "text/html; charset=UTF-8");
            msg_body.addBodyPart(htmlPart);
            wrap.setContent(msg_body);

            MimeMultipart msg = new MimeMultipart("mixed");
            message.setContent(msg);
            msg.addBodyPart(wrap);

            if (!filePath.equals("")) {
                MimeBodyPart att = new MimeBodyPart();
                DataSource fds   = new FileDataSource(filePath);
                att.setDataHandler(new DataHandler(fds));
                att.setFileName(fds.getName());
                msg.addBodyPart(att);
            }

            AmazonSimpleEmailService sesClient = AmazonSimpleEmailServiceClientBuilder
                    .standard().withRegion(Regions.AP_SOUTH_1).build();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

            SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage)
                    .withConfigurationSetName(CONFIGSET);
            sesClient.sendRawEmail(rawEmailRequest);

            logger.info("Email sent for customer | partyCode={} | email={}",
                    metadataJson.optString("partycode"), emailId);

            sesClient.shutdown();
            return "successful";

        } catch (Exception ex) {
            String partyCode = metadataJson.optString("partycode");
            logger.error("Email not sent | partyCode={} | email={} | error={}",
                    partyCode, emailId, ex.getMessage(), ex);

            try {
                long startDateMilli = new Date().getTime();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormatter.setTimeZone(TimeZone.getTimeZone("IST"));
                String activityDate = dateFormatter.format(startDateMilli);

                metadataJson.put("activityDate", activityDate);
                metadataJson.put("boReason",     ex.getMessage());
                metadataJson.put("emailStatus",  "failed");

                final AmazonSQS sqsClient  = AmazonSQSClientBuilder.defaultClient();
                String sqsQueueUrl = sqsClient.getQueueUrl("dpholding-throttling-queue.fifo").getQueueUrl();

                final SendMessageRequest sendMessageRequest =
                        new SendMessageRequest(sqsQueueUrl, metadataJson.toString());
                sendMessageRequest.setMessageGroupId(partyCode);
                final SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);

                logger.info("Bounce pushed to SQS | partyCode={} | messageId={} | sequenceNumber={}",
                        partyCode,
                        sendMessageResult.getMessageId(),
                        sendMessageResult.getSequenceNumber());

                sqsClient.shutdown();

            } catch (Exception sqsEx) {
                logger.error("SQS bounce push FAILED | partyCode={} | error={}",
                        partyCode, sqsEx.getMessage(), sqsEx);
            }

            return ex.getMessage();
        }
    }
}
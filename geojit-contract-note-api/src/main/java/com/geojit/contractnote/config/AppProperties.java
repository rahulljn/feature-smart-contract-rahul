package com.geojit.contractnote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Aws aws = new Aws();
    private Jwt jwt = new Jwt();

    @Data
    public static class Aws {
        private String region;
        private long presignedUrlExpiryHours = 168;
        private S3 s3 = new S3();
        private Sqs sqs = new Sqs();
        private Lambda lambda = new Lambda();

        @Data
        public static class S3 {
            private String rawBucket;
            private String chunksBucket;
            private String pdfBucket;
            private String reportBucket;
            private String errorBucket;
            private String templateBucket;
        }

        @Data
        public static class Sqs {
            private String pipelineStatusQueue;
            private String mapSplitQueue;
            private String exceptionsQueueUrl;
        }

        @Data
        public static class Lambda {
            private String splitLambda;
        }

        private CloudWatch cloudwatch = new CloudWatch();

        @Data
        public static class CloudWatch {
            /** split-lambda-geojit — splits the raw file into per-customer chunks */
            private String splitLambdaLogGroup = "/aws/lambda/split-lambda-geojit";
            /** invoke-lambda-geojit — orchestrates PDF + email flow per chunk */
            private String invokeLambdaLogGroup = "/aws/lambda/invoke-lambda-geojit";
            /** create-pdf-geojit (DynamicValuePdf) — generates the contract note PDF */
            private String pdfLambdaLogGroup = "/aws/lambda/create-pdf-geojit";
            /** json-lambda-geojit (GetJsonLambda) — extracts JSON data for PDF rendering */
            private String jsonLambdaLogGroup = "/aws/lambda/json-lambda-geojit";
            /** email-notification-geojit — dispatches emails via SES */
            private String emailLambdaLogGroup = "/aws/lambda/email-notification-geojit";
            /** pull-bounce-geojit (PullBounceSQS) — processes SES bounce notifications */
            private String bounceLambdaLogGroup = "/aws/lambda/pull-bounce-geojit";
            /** pull-delivery-geojit (PullDeliverSQS) — processes SES delivery notifications */
            private String deliveryLambdaLogGroup = "/aws/lambda/pull-delivery-geojit";
            /** status-consumer-geojit (StatusConsumerLambda) — bridges pipeline events to API */
            private String statusConsumerLogGroup = "/aws/lambda/status-consumer-geojit";
        }
    }

    @Data
    public static class Jwt {
        private String issuer;
        private long expirationMs;
        private long refreshExpirationMs;
    }
}

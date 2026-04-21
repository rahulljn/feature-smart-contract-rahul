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
        }

        @Data
        public static class Sqs {
            private String pipelineStatusQueue;
            private String mapSplitQueue;
        }

        @Data
        public static class Lambda {
            private String splitLambda;
        }
    }

    @Data
    public static class Jwt {
        private String issuer;
        private long expirationMs;
        private long refreshExpirationMs;
    }
}

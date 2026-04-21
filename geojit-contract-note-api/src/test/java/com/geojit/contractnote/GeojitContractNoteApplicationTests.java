package com.geojit.contractnote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.lambda.AWSLambda;

/**
 * Application context smoke test.
 *
 * Mocks all AWS beans so the context loads without real AWS credentials.
 * H2 in-memory DB is used (see application-test.yml — Flyway disabled, Hibernate create-drop).
 */
@SpringBootTest
@ActiveProfiles("test")
class GeojitContractNoteApplicationTests {

    // Mock AWS SDK beans so context loads in CI without credentials
    @MockBean AmazonS3    amazonS3;
    @MockBean AmazonSQS   amazonSQS;
    @MockBean AWSLambda   awsLambda;

    @Test
    void contextLoads() {
        // Asserts Spring context starts without errors
    }
}

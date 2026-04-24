package com.geojit.contractnote.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * Local development AWS config.
 *
 * Active only when spring.profiles.active=local (the default for local dev).
 * Uses real AWS credentials from ~/.aws/credentials (DefaultAWSCredentialsProviderChain),
 * so file uploads, SQS, and Lambda calls all work against real AWS services.
 *
 * Configure dev bucket/queue names in application-local.yml under app.aws.*
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalMockAwsConfig {

    @Value("${app.aws.region}")
    private String awsRegion;

    @Bean
    public AmazonS3 amazonS3() {
        log.info("✅  LOCAL PROFILE: Using real AmazonS3 client (credentials from ~/.aws/credentials)");
        return AmazonS3ClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public AmazonSQS amazonSQS() {
        log.info("✅  LOCAL PROFILE: Using real AmazonSQS client (credentials from ~/.aws/credentials)");
        return AmazonSQSClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public AWSLambda awsLambda() {
        log.info("✅  LOCAL PROFILE: Using real AWSLambda client (credentials from ~/.aws/credentials)");
        return AWSLambdaClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public AmazonSimpleEmailService amazonSimpleEmailService() {
        log.info("✅  LOCAL PROFILE: Using real AmazonSimpleEmailService client (credentials from ~/.aws/credentials)");
        return AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public SesV2Client sesV2Client() {
        log.info("✅  LOCAL PROFILE: Using real SesV2Client (credentials from ~/.aws/credentials)");
        return SesV2Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public CloudWatchLogsClient cloudWatchLogsClient() {
        log.info("✅  LOCAL PROFILE: Using real CloudWatchLogsClient (credentials from ~/.aws/credentials)");
        return CloudWatchLogsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public AWSSecretsManager awsSecretsManager() {
        log.info("✅  LOCAL PROFILE: Using real AWSSecretsManager client (credentials from ~/.aws/credentials)");
        return AWSSecretsManagerClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }
}

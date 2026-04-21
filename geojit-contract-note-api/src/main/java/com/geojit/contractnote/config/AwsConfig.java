package com.geojit.contractnote.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")   // Real AWS beans only for non-local profiles. Local uses LocalMockAwsConfig.
public class AwsConfig {

    @Value("${app.aws.region}")
    private String awsRegion;

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public AmazonSQS amazonSQS() {
        return AmazonSQSClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public AWSLambda awsLambda() {
        return AWSLambdaClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Bean
    public AmazonSimpleEmailService amazonSimpleEmailService() {
        return AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }
}

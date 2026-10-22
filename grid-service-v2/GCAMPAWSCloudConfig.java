package com.ford.grid.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GCAMPAWSCloudConfig {

    @Value("${gcamp.aws.s3.endpoint}")
    String endPointURL;

    @Value("${gcamp.aws.s3.accessKey}")
    String accessKey;

    @Value("${gcamp.aws.s3.secretKey}")
    String secretKey;

    @Bean("s3BucketClient")
    public AmazonS3 s3BucketClient() {
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration config = new ClientConfiguration().withProtocol(Protocol.HTTPS);
        config.setSignerOverride("S3SignerType");
        EndpointConfiguration endpointConfiguration = new EndpointConfiguration(endPointURL, Regions.DEFAULT_REGION.toString());

        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withClientConfiguration(config).enablePathStyleAccess()
                .withEndpointConfiguration(endpointConfiguration).build();
    }
}
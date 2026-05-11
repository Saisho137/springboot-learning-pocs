package com.parameter_store.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${spring.cloud.aws.credentials.profile.name:default}")
    private String profileName;

    @Bean
    public SsmClient ssmClient() {
        // Usar el ProfileCredentialsProvider para leer credenciales desde ~/.aws/credentials
        return SsmClient.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create(profileName))
                .build();
    }
}

package com.ford.grid.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Configuration
@ConfigurationProperties(prefix = "mongodb.details")
public class MongoDBDetails {

    private String host;
    private String caPem;
    private String connectionParams;
    private String mongoCertPass;
    private String mongoUsername;
    private String mongoKey;
    private String authMechanism;
}

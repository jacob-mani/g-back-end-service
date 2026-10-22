package com.ford.grid.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Google Cloud Storage for the GCAMP workspace.
 * Sets up the Storage bean and configures CORS settings for the specified bucket.
 */
@Configuration
public class GCAMPCloudConfiguration {

    @Value("${gcp.gcamp.project.id}")
    private String gridCloudProjectIdGCAMP;

    @Bean("gcampCloudStorages")
    public Storage getCloudStorage() {
        return StorageOptions.newBuilder().setProjectId(gridCloudProjectIdGCAMP).build().getService();
    }
}
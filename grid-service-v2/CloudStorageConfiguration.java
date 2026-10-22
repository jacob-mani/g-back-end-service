package com.ford.grid.config;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Configuration class for Google Cloud Storage.
 * Sets up the Storage bean and configures CORS settings for the specified bucket.
 */
@Configuration
public class CloudStorageConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudStorageConfiguration.class);

    @Value("${gcp.grid.project.id}")
    private String gridCloudProjectId;

    @Value("${gcp.data.bucket-GCP}")
    private String cloudBucketName;

    @Value("${spring.cloud.gcp.project-id-GDIA}")
    private String gcpGdiaProjectId;

    @Value("${gcp.data.bucket-GDIA}")
    private String gdiaBucketName;

    @Value("${gcp.trigger.bucket-GDIA}")
    private String gdiaTriggerBucketName;

    @Primary
    @Bean("cloudStorage")
    public Storage getCloudStorage() {
        try {
            Storage cloudStorage = buildStorageOptions(gridCloudProjectId);

            // Try to access bucket and configure CORS - may fail locally
            try {
                Bucket bucket = cloudStorage.get(cloudBucketName);
                if (bucket != null) {
                    Cors corsObject = getCorsObject(allowableHTTPMethods(), applicableRequestHeaders());
                    bucket.toBuilder().setCors(List.of(corsObject)).build().update();
                    LOGGER.info("Successfully configured CORS for bucket: {}", cloudBucketName);
                } else {
                    LOGGER.warn("Bucket {} not found. CORS configuration skipped.", cloudBucketName);
                }
            } catch (Exception e) {
                // Don't fail startup if bucket access fails (common in local dev)
                LOGGER.warn("Could not configure bucket CORS (this is normal for local development): {}", e.getMessage());
            }

            return cloudStorage;
        } catch (Exception e) {
            LOGGER.error("Failed to create Cloud Storage client: {}. Creating mock storage for local development.", e.getMessage());
            // Return a storage client anyway - operations will fail individually but app won't crash on startup
            return StorageOptions.newBuilder().setProjectId(gridCloudProjectId).build().getService();
        }
    }

    @Bean("gridGcpGdiaBucket")
    @ConditionalOnProperty(name = "gdia.enable", havingValue = "true")
    public Storage gridGcpGdiaBucket() {
        try {
            LOGGER.info("Creating GDIA Storage bean for project: {}", gcpGdiaProjectId);
            Storage gdiaStorage = buildStorageOptions(gcpGdiaProjectId);

            // Don't try to access the bucket during startup to avoid permission issues
            // CORS configuration will be attempted during first upload if needed
            LOGGER.info("GDIA Storage bean created successfully. Bucket access will be validated during first use.");

            return gdiaStorage;

        } catch (Exception e) {
            LOGGER.error("Failed to create GDIA Storage bean. This may be due to invalid project configuration. Error: {}", e.getMessage());
            throw new RuntimeException("Failed to create GDIA Storage: " + e.getMessage(), e);
        }
    }

    private Storage buildStorageOptions(String projectId) {
        return StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }

    private List<HttpMethod> allowableHTTPMethods() {
        return List.of(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS);
    }

    private List<String> applicableRequestHeaders() {
        return List.of("Access-Control-Allow-Origin", "Content-Type", "x-goog-resumable", "Authorization", "Accept");
    }

    private Cors getCorsObject(List<HttpMethod> methods, List<String> responseHeaders) {
        return Cors.newBuilder()
                .setOrigins(List.of(Cors.Origin.of("*")))
                .setMethods(methods)
                .setResponseHeaders(responseHeaders)
                .setMaxAgeSeconds(3600)
                .build();
    }
}
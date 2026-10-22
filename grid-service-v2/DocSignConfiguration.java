package com.ford.grid.config;

import com.ford.grid.exception.GridException;
import com.ford.grid.util.JWTUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DocuSign configuration — mirrors MongoConfiguration pattern.
 * Reads PEM from DOC_CERT_PATH (disk) or classpath (local dev).
 */
@Profile("!test")
@Configuration
public class DocSignConfiguration {

    static final Logger log = LoggerFactory.getLogger(DocSignConfiguration.class);

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY   = "-----END PRIVATE KEY-----";
    private static final String BEGIN_RSA_KEY     = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String END_RSA_KEY       = "-----END RSA PRIVATE KEY-----";
    private static final String CLASSPATH_DEV_KEY = "DS_KEY_DEV.pem";

    /**
     * Generates DocuSign JWT assertion with comprehensive error reporting.
     *
     * @param docSignDetails Configuration containing clientId, userId, apiBasePath, scope, privateKeyPath
     * @return Signed JWT assertion string
     * @throws GridException with specific error details if generation fails
     */
    public String getAssertionKey(DocSignDetails docSignDetails) throws GridException {
        log.info("=== DocuSign JWT Generation START ===");

        // Step 1: Validate configuration
        validateDocSignDetails(docSignDetails);

        // Step 2: Load PEM content
        String pemContent;
        try {
            pemContent = loadPemContent(docSignDetails);
            log.info("DocuSign: PEM file loaded successfully, content length: {} bytes", pemContent.length());
        } catch (IOException e) {
            log.error("DocuSign: Failed to read PEM file: {}", e.getMessage(), e);
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    String.format("Failed to read PEM file: %s. Error: %s",
                            getKeySourceDescription(docSignDetails), e.getMessage())
            );
        }

        // Step 3: Extract private key block
        String privateKeyPem = extractPrivateKeyValue(pemContent);
        if (privateKeyPem == null) {
            log.error("DocuSign: No private key block found in PEM content");
            logPemContentAnalysis(pemContent);
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    String.format(
                            "No private key block found in PEM file. " +
                                    "Expected '%s' or '%s'. " +
                                    "Source: %s. " +
                                    "Please ensure your PEM file contains a valid RSA private key from DocuSign Admin Console.",
                            BEGIN_PRIVATE_KEY, BEGIN_RSA_KEY, getKeySourceDescription(docSignDetails)
                    )
            );
        }

        log.info("DocuSign: Private key block extracted successfully, length: {} bytes", privateKeyPem.length());

        // Step 4: Generate JWT assertion
        try {
            log.info("DocuSign: Calling JWTUtils.generateJWTAssertionFromByteArray...");
            log.debug("DocuSign JWT params - apiBasePath: {}, clientId: {}, userId: {}, scope: {}",
                    docSignDetails.getApiBasePath(),
                    docSignDetails.getClientId(),
                    docSignDetails.getUserId(),
                    docSignDetails.getScope());

            String assertion = JWTUtils.generateJWTAssertionFromByteArray(
                    privateKeyPem.getBytes(StandardCharsets.UTF_8),
                    docSignDetails.getApiBasePath(),
                    docSignDetails.getClientId(),
                    docSignDetails.getUserId(),
                    3600L,
                    docSignDetails.getScope()
            );

            if (assertion == null || assertion.isBlank()) {
                log.error("DocuSign: JWTUtils returned null or empty assertion");
                throw new GridException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DocuSign Auth Error",
                        "JWT assertion generation returned null. Check JWTUtils implementation and private key format."
                );
            }

            log.info("=== DocuSign JWT Generation SUCCESS === (assertion length: {} chars)", assertion.length());
            return assertion;

        } catch (GridException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("DocuSign: Invalid key format in JWTUtils: {}", e.getMessage(), e);
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Auth Error",
                    String.format(
                            "Invalid RSA private key format: %s. " +
                                    "The private key must be in PKCS#1 (-----BEGIN RSA PRIVATE KEY-----) " +
                                    "or PKCS#8 (-----BEGIN PRIVATE KEY-----) format. " +
                                    "Generate a new key from DocuSign Admin Console if needed.",
                            e.getMessage()
                    )
            );
        } catch (Exception e) {
            log.error("DocuSign: Unexpected error in JWT generation: {}", e.getMessage(), e);
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Auth Error",
                    String.format(
                            "Failed to generate DocuSign JWT assertion. " +
                                    "Error: %s (%s). " +
                                    "Verify that the DOC_CERT_PATH environment variable is set and points to a valid RSA private key PEM file.",
                            e.getMessage(),
                            e.getClass().getSimpleName()
                    )
            );
        }
    }

    /**
     * Validates all required DocSignDetails fields
     */
    private void validateDocSignDetails(DocSignDetails docSignDetails) throws GridException {
        if (docSignDetails == null) {
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    "DocSignDetails is null. Check Spring configuration for 'docusign' properties."
            );
        }

        if (docSignDetails.getClientId() == null || docSignDetails.getClientId().isBlank()) {
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    "docusign.clientId is not configured in application.yml"
            );
        }

        if (docSignDetails.getUserId() == null || docSignDetails.getUserId().isBlank()) {
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    "docusign.userId is not configured in application.yml"
            );
        }

        if (docSignDetails.getApiBasePath() == null || docSignDetails.getApiBasePath().isBlank()) {
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    "docusign.apiBasePath is not configured in application.yml"
            );
        }

        if (docSignDetails.getScope() == null || docSignDetails.getScope().isBlank()) {
            throw new GridException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "DocuSign Configuration Error",
                    "docusign.scope is not configured in application.yml"
            );
        }

        log.info("DocuSign configuration validated successfully");
    }

    /**
     * Load PEM content from disk path (DEPLOYED) or classpath (LOCAL DEV)
     */
    private String loadPemContent(DocSignDetails docSignDetails) throws IOException, GridException {
        String absolutePath = docSignDetails.getPrivateKeyPath();

        // Only use disk path when it is a real absolute path — same as MONGO_CERT_PATH pattern.
        // Empty string, relative path (e.g. DS_KEY_DEV.pem), or unresolved ${...} → classpath fallback.
        boolean isAbsoluteDiskPath = absolutePath != null
                && !absolutePath.isBlank()
                && !absolutePath.startsWith("${")
                && Paths.get(absolutePath).isAbsolute();

        if (isAbsoluteDiskPath) {
            // DEPLOYED: Read from disk path (same as MongoConfiguration)
            log.info("DocuSign: Loading PEM from disk path: {}", absolutePath);
            Path path = Paths.get(absolutePath).normalize();

            if (!Files.exists(path)) {
                log.error("DocuSign: PEM file not found at path: {}", absolutePath);
                throw new GridException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DocuSign Configuration Error",
                        String.format(
                                "PEM file not found at path: %s. " +
                                        "Verify DOC_CERT_PATH environment variable points to the correct file location.",
                                absolutePath
                        )
                );
            }

            if (!Files.isReadable(path)) {
                log.error("DocuSign: PEM file is not readable: {}", absolutePath);
                throw new GridException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DocuSign Configuration Error",
                        String.format(
                                "PEM file is not readable: %s. Check file permissions.",
                                absolutePath
                        )
                );
            }

            long fileSize = Files.size(path);
            log.info("DocuSign: PEM file found, size: {} bytes", fileSize);

            if (fileSize < 100) {
                log.warn("DocuSign: PEM file seems too small ({} bytes) - may be invalid", fileSize);
            }

            if (fileSize > 10000) {
                log.warn("DocuSign: PEM file seems too large ({} bytes) - may contain extra content", fileSize);
            }

            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        } else {
            // LOCAL DEV: Read from classpath (same as MongoConfiguration fallback)
            log.info("DocuSign: DOC_CERT_PATH not set — loading {} from classpath", CLASSPATH_DEV_KEY);

            try (InputStream inputStream = new ClassPathResource(CLASSPATH_DEV_KEY).getInputStream()) {
                String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                log.info("DocuSign: Loaded {} from classpath, size: {} bytes", CLASSPATH_DEV_KEY, content.length());
                return content;
            } catch (IOException e) {
                log.error("DocuSign: Failed to load {} from classpath: {}", CLASSPATH_DEV_KEY, e.getMessage());
                throw new GridException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DocuSign Configuration Error",
                        String.format(
                                "Failed to load %s from classpath. " +
                                        "For local development, ensure %s exists in src/main/resources/. " +
                                        "For deployed environments, set DOC_CERT_PATH environment variable.",
                                CLASSPATH_DEV_KEY, CLASSPATH_DEV_KEY
                        )
                );
            }
        }
    }

    /**
     * Extract private key block from PEM content.
     * Supports both PKCS#8 and PKCS#1 formats.
     */
    public String extractPrivateKeyValue(String pemContent) {
        if (pemContent == null || pemContent.isBlank()) {
            log.warn("DocuSign: PEM content is null or empty");
            return null;
        }

        // PKCS#8: -----BEGIN PRIVATE KEY-----
        int beginIndex = pemContent.indexOf(BEGIN_PRIVATE_KEY);
        int endIndex = pemContent.indexOf(END_PRIVATE_KEY);
        if (beginIndex != -1 && endIndex != -1) {
            log.info("DocuSign: Extracted PKCS#8 private key block (position {}-{})", beginIndex, endIndex);
            return pemContent.substring(beginIndex, endIndex + END_PRIVATE_KEY.length());
        }

        // PKCS#1: -----BEGIN RSA PRIVATE KEY-----
        beginIndex = pemContent.indexOf(BEGIN_RSA_KEY);
        endIndex = pemContent.indexOf(END_RSA_KEY);
        if (beginIndex != -1 && endIndex != -1) {
            log.info("DocuSign: Extracted PKCS#1 RSA private key block (position {}-{})", beginIndex, endIndex);
            return pemContent.substring(beginIndex, endIndex + END_RSA_KEY.length());
        }

        log.warn("DocuSign: No valid private key block found in PEM content");
        return null;
    }

    /**
     * Log detailed PEM content analysis for debugging
     */
    private void logPemContentAnalysis(String pemContent) {
        log.info("=== PEM Content Analysis ===");
        log.info("Total length: {} bytes", pemContent.length());
        log.info("Contains '{}': {}", BEGIN_PRIVATE_KEY, pemContent.contains(BEGIN_PRIVATE_KEY));
        log.info("Contains '{}': {}", END_PRIVATE_KEY, pemContent.contains(END_PRIVATE_KEY));
        log.info("Contains '{}': {}", BEGIN_RSA_KEY, pemContent.contains(BEGIN_RSA_KEY));
        log.info("Contains '{}': {}", END_RSA_KEY, pemContent.contains(END_RSA_KEY));
        log.info("Contains 'BEGIN CERTIFICATE': {}", pemContent.contains("-----BEGIN CERTIFICATE-----"));
        log.info("Contains 'BEGIN PUBLIC KEY': {}", pemContent.contains("-----BEGIN PUBLIC KEY-----"));

        // Show first 200 chars (safe for logs)
        String preview = pemContent.length() > 200
                ? pemContent.substring(0, 200) + "..."
                : pemContent;
        log.info("Content preview: {}", preview);
        log.info("========================");
    }

    /**
     * Get human-readable description of key source for error messages
     */
    private String getKeySourceDescription(DocSignDetails docSignDetails) {
        String path = docSignDetails.getPrivateKeyPath();
        return (path != null && !path.isBlank())
                ? "disk path: " + path
                : "classpath: " + CLASSPATH_DEV_KEY;
    }
}

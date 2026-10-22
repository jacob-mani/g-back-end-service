package com.ford.grid.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DocuSign configuration properties — mirrors MongoDBDetails pattern.
 * Maps all docusign.* properties from application.properties.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Configuration
@ConfigurationProperties(prefix = "docusign")
public class DocSignDetails {

    /** docusign.privateKeyPath = ${DOC_CERT_PATH} */
    String privateKeyPath;

    /** docusign.clientId */
    String clientId;

    /** docusign.userId */
    String userId;

    /** docusign.apiBasePath — e.g. account-d.docusign.com */
    String apiBasePath;

    /** docusign.apiAccountId */
    String apiAccountId;

    /** docusign.apiUrl — DocuSign REST API base URL */
    String apiUrl;

    /** docusign.oauthTokenUrl */
    String oauthTokenUrl;

    /** docusign.scope */
    String scope;
}

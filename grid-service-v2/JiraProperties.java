package com.ford.grid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {
    private String baseUrl;
    private String email;
    private String apiToken;
    private String projectKey;
    private String epicNameFieldId;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }

    public String getEpicNameFieldId() { return epicNameFieldId; }
    public void setEpicNameFieldId(String epicNameFieldId) { this.epicNameFieldId = epicNameFieldId; }
}

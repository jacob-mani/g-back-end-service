package com.ford.grid.config;


import com.ford.grid.constants.AzureAuthConstants;
import com.ford.grid.constants.CommonConstants;
import com.ford.grid.model.dto.AzureTokenResponse;
import com.ford.grid.model.entity.AzureCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;


@Service
public class AzureAdTokenGenerator {

    private final AzureCredentials azureCredentials;
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAdTokenGenerator.class);

    public AzureAdTokenGenerator(AzureCredentials azureCredentials) {
        this.azureCredentials = azureCredentials;
    }

    @Cacheable("dlsToken")
    public String getAzureDLSToken()  {
        return getBearerToken(AzureAuthConstants.DLS_TYPE);
    }

    private String getBearerToken(String type) {
        RestTemplate restTemplate = setProxy();

        HttpHeaders azureHttpHeaders = new HttpHeaders();
        azureHttpHeaders.add(AzureAuthConstants.CONTENT_LENGTH, AzureAuthConstants.HEADER_VALUE);
        azureHttpHeaders.add(AzureAuthConstants.CONTENT_TYPE, AzureAuthConstants.APPLICATION_X_WWW_FORM_URLENCODED);

        MultiValueMap<String, String> azureBody = new LinkedMultiValueMap<>();
        azureBody.add(AzureAuthConstants.GRANT_TYPE, AzureAuthConstants.CLIENT_CREDENTIALS);
        azureBody.add(AzureAuthConstants.CLIENT_ID, azureCredentials.getClientId());
        azureBody.add(AzureAuthConstants.CLIENT_SECRET, azureCredentials.getClientSecret());
        if (AzureAuthConstants.DLS_TYPE.equals(type)) {
            azureBody.add(AzureAuthConstants.SCOPE, azureCredentials.getScopedls());
        } else {
            azureBody.add(AzureAuthConstants.SCOPE, azureCredentials.getScopemdp());
        }

        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(azureBody, azureHttpHeaders);
        ResponseEntity<AzureTokenResponse> responseEntity =
                restTemplate.postForEntity(AzureAuthConstants.AZURE_URL, httpEntity, AzureTokenResponse.class);

        if (responseEntity.getStatusCodeValue() == 200 && null != responseEntity.getBody()) {
            return AzureAuthConstants.BEARER + responseEntity.getBody().getAccessToken();
        } else {
            LOGGER.error(AzureAuthConstants.TOKEN_GENERATION_FAILED);
            throw new RuntimeException(AzureAuthConstants.TOKEN_GENERATION_FAILED);
        }
    }

    private RestTemplate setProxy() {
        try {
            InetAddress.getByName(AzureAuthConstants.FORD_PROXY_SERVER);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(AzureAuthConstants.FORD_PROXY_SERVER, AzureAuthConstants.FORD_PROXY_PORT));
            SimpleClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
            httpRequestFactory.setProxy(proxy);
            return new RestTemplate(httpRequestFactory);
        } catch (UnknownHostException exception) {
            LOGGER.error(exception.getMessage(), exception);
            throw new RuntimeException(AzureAuthConstants.TOKEN_GENERATION_FAILED);
        }
    }


}

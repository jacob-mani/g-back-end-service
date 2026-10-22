package com.ford.grid.config;

import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import java.util.function.Consumer;
import com.ford.grid.security.AdOAuth2ClientCredentialsGrantRequestEntityConverter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Configuration
public class WebClientConfiguration {

    @Value("${restserviceclient.wiretap.enabled}")
    boolean enableWiretap;

    @Value("${cn.app.proxy-config.host:internet.gcp.ford.com}")
    String proxyHost;

    @Value("${cn.app.proxy-config.port:83}")
    Integer proxyPort;

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    OAuth2AuthorizedClientService authorizedClientService;

    @Value("${deven-cr-refapp.url}")
    String devenRefAppBaseUrl;

    @Bean
    WebClient refAppWebClient(WebClient.Builder webClientBuilder, @Value("${restserviceclient.proxy.enabled}")  boolean proxyEnabled) {

        return webClientBuilder
                .baseUrl(devenRefAppBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(getHttpClient(proxyEnabled)))
                .apply(oAuth2Configuration("restserviceclient"))
                .build();
    }

    private Consumer<WebClient.Builder> oAuth2Configuration(String clientRegistrationId) {

        DefaultClientCredentialsTokenResponseClient clientCredentialsTokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
        clientCredentialsTokenResponseClient.setRequestEntityConverter(new AdOAuth2ClientCredentialsGrantRequestEntityConverter());

        return getOAuth2Configuration(clientRegistrationId, clientCredentialsTokenResponseClient);
    }

    private Consumer<WebClient.Builder> getOAuth2Configuration(String clientRegistrationId, DefaultClientCredentialsTokenResponseClient clientCredentialsTokenResponseClient) {
        OAuth2AuthorizedClientManager oAuth2ClientManager = getOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2ClientManager);
        oauth2.setDefaultClientRegistrationId(clientRegistrationId);

        return oauth2.oauth2Configuration();
    }

    public OAuth2AuthorizedClientManager getOAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    private HttpClient getHttpClient(boolean proxyEnabled) {
        HttpClient httpClient = HttpClient.create();
        maybeWiretap(httpClient);

        if (!proxyEnabled) return httpClient;
        if (!proxyRequired()) return httpClient;

        HttpClient httpClientWithProxy = HttpClient.create().proxyWithSystemProperties();
        maybeWiretap(httpClientWithProxy);

        return httpClientWithProxy;
    }

    private void maybeWiretap(HttpClient httpClient) {
        if (!enableWiretap) return;
        httpClient.wiretap("reactor.netty.http.client.HttpClient",
                LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
    }

    private boolean proxyRequired() {
        try {
            InetAddress.getByName(proxyHost);
            return true;
        } catch (UnknownHostException unknownHostException) {
            return false;
        }
    }
}

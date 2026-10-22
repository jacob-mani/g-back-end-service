package com.ford.grid.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.nimbusds.oauth2.sdk.util.tls.TLSVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

@Profile("!test")
@Configuration
public class MongoConfiguration {

    static final Logger log = LoggerFactory.getLogger(MongoConfiguration.class);
    private static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    @Bean
    public MongoClient mongoClient(MongoDBDetails mongoDBDetails) {
        if (Objects.nonNull(mongoDBDetails.getAuthMechanism()) && mongoDBDetails.getAuthMechanism().equalsIgnoreCase("DEFAULT")) {
            log.info("Mongo DB default authentication mechanism");
            return getDefaultMongoClient(mongoDBDetails);
        } else {
            log.info("Mongo DB X509 authentication mechanism");
            return getX509Client(mongoDBDetails);
        }
    }

    public MongoClient getX509Client(MongoDBDetails mongoDBDetails) {
        var connString = new ConnectionString(mongoDBDetails.getHost() + "&authSource=%24external&authMechanism=MONGODB-X509");
        SSLContext context = null;
        context = getSSLContext(mongoDBDetails);
        SSLContext finalContext = context;
        MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connString)
                .applyToSslSettings(builder -> {
                    builder.context(finalContext);
                    builder.enabled(true);
                }).build();
        return MongoClients.create(settings);
    }

    public MongoClient getDefaultMongoClient(MongoDBDetails mongoDBDetails) {
        var connString = new ConnectionString(mongoDBDetails.getHost());
        MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connString).build();
        return MongoClients.create(settings);
    }

    public SSLContext getSSLContext(MongoDBDetails mongoDBDetails) {
        InputStream inputStream;
        try {
            String absolutePath = mongoDBDetails.getCaPem();
            Path path = Paths.get(absolutePath).normalize();
            inputStream = new ByteArrayInputStream(Files.readAllBytes(path.normalize()));
            log.info("Certificate Value" + inputStream);
            String secretValue = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String certificateValue = extractCertificateValue(secretValue);
            String privateKeyValue = extractPrivateKeyValue(secretValue);
            if (Objects.nonNull(certificateValue) && Objects.nonNull(privateKeyValue)) {
                return getSSLContext(certificateValue, privateKeyValue);
            }
            return null;
        } catch (IOException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException |
                 CertificateException | InvalidKeySpecException | UnrecoverableKeyException | NullPointerException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private SSLContext getSSLContext(String certificateValue, String privateKeyValue) throws NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certificateValue.getBytes()));
        byte[] privateKeyBytes = Base64.getMimeDecoder().decode(privateKeyValue.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace(END_PRIVATE_KEY, "")
                .replace("\n", ""));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("certificate", certificate);
        keyStore.setKeyEntry("privateKey", privateKey, new char[0], new X509Certificate[]{certificate});
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, new char[0]);
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
//        SSLContext sslContext = SSLContext.getInstance("TLS");
        SSLContext sslContext = SSLContext.getInstance(TLSVersion.TLS_1_3.toString());
//        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        sslContext.init(keyManagerFactory.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private String extractCertificateValue(String secretValue) {
        int beginIndex = secretValue.indexOf("-----BEGIN CERTIFICATE-----");
        int endIndex = secretValue.indexOf("-----END CERTIFICATE-----");
        if (beginIndex != -1 && endIndex != -1) {
            return secretValue.substring(beginIndex, endIndex + "-----END CERTIFICATE-----".length());
        } else {
            log.error("Certificate Value is not found");
            return null;
        }
    }

    private String extractPrivateKeyValue(String secretValue) {
        int beginIndex = secretValue.indexOf("-----BEGIN PRIVATE KEY-----");
        int endIndex = secretValue.indexOf(END_PRIVATE_KEY);
        if (beginIndex != -1 && endIndex != -1) {
            return secretValue.substring(beginIndex, endIndex + END_PRIVATE_KEY.length());
        } else {
            log.error("Private Key Value is not found");
            return null;
        }
    }
}

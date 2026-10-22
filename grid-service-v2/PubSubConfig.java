package com.ford.grid.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;


@Configuration
public class PubSubConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubConfig.class);

    @Bean
    public String subscriptionName(@Value("${subscriptionName}") String subscriptionName) {
        LOGGER.info("messageChannel subscriptionName : {}", subscriptionName);
        return subscriptionName;
    }

    @Bean
    public MessageChannel pubsubInputChannel() {
        LOGGER.info("messageChannel pubsubInputChannel : {}");
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate,
            @Qualifier("subscriptionName") String subscriptionName) {
        PubSubInboundChannelAdapter adapter =
            new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
    adapter.setOutputChannel(inputChannel);
    adapter.setAckMode(AckMode.MANUAL);
    adapter.setPayloadType(String.class);
    
    // Log subscription name
    LOGGER.info("messageChannelAdapter subscriptionName : {}", subscriptionName);
    LOGGER.info("PubSubTemplate object: {}", pubSubTemplate);
    
    return adapter;
    }
}

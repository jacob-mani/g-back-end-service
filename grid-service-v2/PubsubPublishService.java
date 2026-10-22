package com.ford.grid.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class PubsubPublishService {

    private final StreamBridge streamBridge;

    @Autowired
    public PubsubPublishService(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public boolean sendMessage(String topic, String message) {
        //  Safely validates inputs before doing any work
        if (topic == null || topic.isBlank()) { return false; }
        if (message == null || message.isBlank()) {  return false; }

        //  Generates a unique tracking ID
        String messageId = UUID.randomUUID().toString();

        //  Logs the action using the ID instead of printing the whole payload
        log.info("Sending FSA payload [messageId={}] topic={}: {}", messageId, topic, message);

        //  Wraps payload and attaches the ID as metadata (headers)
        Message<String> springMessage = MessageBuilder
                .withPayload(message)
                .setHeader("messageId", messageId)
                .build();

        //  Sends the message and explicitly logs an error if the publish fails
        boolean sent = streamBridge.send(topic, springMessage);
        if (!sent) {
            log.error("Publish returned false [messageId={}] topic={}: {}", messageId, topic, message);
        }

        return sent;
    }
}

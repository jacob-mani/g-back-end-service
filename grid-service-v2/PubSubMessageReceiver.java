package com.ford.grid.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ford.grid.service.GvmsService;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class PubSubMessageReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubMessageReceiver.class);

    @Autowired
    GvmsService gvmsService;

    @ServiceActivator(inputChannel = "pubsubInputChannel")
    public void messageReceiver(
            String payload,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        LOGGER.info("Message arrived! Payload: {}", payload);

        if (payload == null || payload.isEmpty()) {
            LOGGER.warn("Received empty or null payload.  Message will not be processed.");
            return; // Exit the method as there's nothing to process
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(payload);
            String localFsa = rootNode.path("localfsa").asText();
            String globalFsa = rootNode.path("globalfsa").asText();

            if (globalFsa == null || globalFsa.isEmpty()) {
                LOGGER.warn("Global Fsa is null or empty in the payload. Message will not be processed.");
                return;
            }

            boolean saveGvms = false;
            try {
                saveGvms = gvmsService.saveGlobalFsaProgramFsaMappingDetails(globalFsa, localFsa, payload);

            } catch (Exception e) {
                LOGGER.error("Error while saving Program Global FSA Mapping Details to GVMS: {}", e.getMessage(), e);

            }

            if (saveGvms) {
                LOGGER.info("Successfully saved Program FSA Mapping Details for Global FSA: {}", globalFsa);
            } else {
                LOGGER.warn("Buckets already Exists or Failed to save Program FSA Mapping Details for Global FSA: {}", globalFsa);
            }
            message.ack();
            LOGGER.info("Message Processed! Global FSA: {} Inserted Buckets - {}", globalFsa, saveGvms);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LOGGER.error("Error parsing JSON payload: {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred while processing the message: {}", e.getMessage(), e);
        }

    }

}
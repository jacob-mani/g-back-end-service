package com.ford.grid.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Configuration class for Hub Coordinator recipients by region.
 */
@Configuration
@ConfigurationProperties(prefix = "coordinator.recipients")
@Getter
@Setter
public class HubCoordinatorConfig {

    private String na;
    private String eu;
    private String gc;
    private String sa;
    private String img;

    /**
     * Get hub coordinators list by region code.
     *
     * @param regionCode the region code (NA, EU, GC, SA, IMG)
     * @return list of hub coordinator CDSIDs
     */
    public List<String> getCoordinatorsByRegion(String regionCode) {
        if (regionCode == null || regionCode.isEmpty()) {
            return Collections.emptyList();
        }

        String coordinators = switch (regionCode.toUpperCase()) {
            case "NA" -> na;
            case "EU" -> eu;
            case "GC" -> gc;
            case "SA" -> sa;
            case "IMG" -> img;
            default -> null;
        };

        if (coordinators == null || coordinators.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(coordinators.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}


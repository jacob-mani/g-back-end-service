package com.ford.grid.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
public class LDAPParams {
    private String authDN;
    private String baseDN;
    private String url;
    private String password;
}
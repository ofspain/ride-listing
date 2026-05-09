package com.ridelist.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.email")
@Component
@Data
public class AppEmailProperties {
    private String sender = "mock";
    private String fromAddress = "noreply@ridelist.ng";
    private String fromName = "RideList";
}

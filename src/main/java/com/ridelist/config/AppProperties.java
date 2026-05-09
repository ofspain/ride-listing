package com.ridelist.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app")
@Component
@Data
public class AppProperties {
    private Frontend frontend = new Frontend();

    @Data
    public static class Frontend {
        private String baseUrl = "http://localhost:8081";
    }
}

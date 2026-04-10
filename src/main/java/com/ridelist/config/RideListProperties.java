package com.ridelist.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ridelist.image")
@Getter
@Setter
public class RideListProperties {

    private int minCount = 1;
    private int maxCount = 10;
    private List<String> allowedTypes;
    private int maxSizeMb = 5;
}

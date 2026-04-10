package com.ridelist.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Getter
@Setter
public class AwsS3Properties {

    private String accessKey;
    private String secretKey;
    private String region;
    private S3Properties s3 = new S3Properties();

    @Getter
    @Setter
    public static class S3Properties {
        private String bucket;
    }

    public String getBucket() {
        return s3.getBucket();
    }
}

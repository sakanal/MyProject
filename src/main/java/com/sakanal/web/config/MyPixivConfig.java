package com.sakanal.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system.pixiv")
public class MyPixivConfig {
    private Map<String, String> requestHeader;
    private String charsetName;
    private String username;
    private String password;



}

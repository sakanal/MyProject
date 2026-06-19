package com.sakanal.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author sakanal
 */
@Data
@Component
@ConfigurationProperties(prefix = "system.thread")
public class ThreadPoolConfigProperties {
    private Integer coreSize = 8;
    private Integer maxSize = 50;
    private Integer keepAliveTime = 30;
}

package com.sakanal.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system")
public class MyWebConfig {
    private boolean openProxy = false;
    private String proxyHost;
    private String proxyPort;

    @Bean
    public void proxyConfiguration(){
        if (openProxy){
            log.info("开启代理");
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
        }
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }


}

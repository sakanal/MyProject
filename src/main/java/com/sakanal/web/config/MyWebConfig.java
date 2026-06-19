package com.sakanal.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web系统配置类
 * 用于配置网络代理、TLS协议等系统级网络参数
 * 配置前缀：system
 *
 * @author sakanal
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system")
public class MyWebConfig {
    /**
     * 是否开启网络代理
     * true-使用代理服务器访问网络
     * false-直接访问网络
     */
    private boolean openProxy = false;
    
    /**
     * 代理服务器主机地址
     */
    private String proxyHost;
    
    /**
     * 代理服务器端口号
     */
    private String proxyPort;

    /**
     * 配置网络代理和TLS协议
     * 当openProxy为true时，设置HTTP/HTTPS代理
     * 同时配置支持的TLS协议版本
     */
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
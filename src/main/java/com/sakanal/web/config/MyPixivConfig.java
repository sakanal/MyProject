package com.sakanal.web.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Pixiv配置类
 * 用于存储Pixiv相关的配置信息，包括请求头、字符集、登录凭证等
 * 配置前缀：system.pixiv
 *
 * @author sakanal
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system.pixiv")
public class MyPixivConfig {
    /**
     * HTTP请求头配置，用于模拟浏览器请求
     * 包含Cookie、User-Agent、Referer等必要信息
     */
    private Map<String, String> requestHeader;
    
    /**
     * 字符集名称，用于解析网络请求响应
     * 默认为UTF-8
     */
    private String charsetName;
    
    /**
     * Pixiv登录用户名（预留，当前未使用）
     */
    private String username;
    
    /**
     * Pixiv登录密码（预留，当前未使用）
     */
    private String password;

}
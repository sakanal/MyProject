package com.sakanal.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池配置属性类
 * 用于配置异步下载任务使用的线程池参数
 * 配置前缀：system.thread
 *
 * @author sakanal
 */
@Data
@Component
@ConfigurationProperties(prefix = "system.thread")
public class ThreadPoolConfigProperties {
    /**
     * 线程池核心线程数
     * 默认值：8
     * 核心线程会一直存活，即使处于空闲状态
     */
    private Integer coreSize = 8;
    
    /**
     * 线程池最大线程数
     * 默认值：50
     * 当任务队列满时，线程池会创建新线程直到达到此值
     */
    private Integer maxSize = 50;
    
    /**
     * 线程空闲存活时间（秒）
     * 默认值：30秒
     * 非核心线程空闲超过此时间会被回收
     */
    private Integer keepAliveTime = 30;
}
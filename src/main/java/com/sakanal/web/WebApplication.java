package com.sakanal.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author sakanal
 */
@EnableScheduling
@SpringBootApplication
public class WebApplication {

    /**
     * 登录获取 cookie
     */
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

}

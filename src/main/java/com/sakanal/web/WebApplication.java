package com.sakanal.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WebApplication {

    //TODO 登录获取cookie
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

}

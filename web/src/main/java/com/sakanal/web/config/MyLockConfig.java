package com.sakanal.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Configuration
public class MyLockConfig {
    @Bean
    public ReentrantLock reentrantLock(){
        return new ReentrantLock();
    }
}

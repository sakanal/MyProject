package com.sakanal.web.aspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 锁机制
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TakeLock {
    String lockName();
}

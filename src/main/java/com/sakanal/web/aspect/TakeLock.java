package com.sakanal.web.aspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 锁机制
 * @author sakanal
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TakeLock {
    String lockName();
}

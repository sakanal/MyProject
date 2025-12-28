package com.sakanal.web.aspect;

import com.sakanal.web.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Aspect
@Component
public class TakeLockAspect {
    @Resource
    private LockService lockService;

    @Around("@annotation(takeLock)")
    public Object around(ProceedingJoinPoint joinPoint, TakeLock takeLock) throws Throwable {
        //判断是否可以上锁
        if (!lockService.checkLock(takeLock.lockName())) {
            try {
                if (lockService.setLock(takeLock.lockName())) {
                    log.info("{}上锁成功", takeLock.lockName());
                    long start = System.currentTimeMillis();
                    Object proceed = joinPoint.proceed();
                    long end = System.currentTimeMillis();
                    log.info("耗时{}秒", (end - start) / 1000);
                    return proceed;
                } else {
                    log.info("{}上锁失败", takeLock.lockName());
                }
            } finally {
                if (lockService.unsetLock(takeLock.lockName())) {
                    log.info("{}解锁成功", takeLock.lockName());
                } else {
                    log.info("{}解锁失败", takeLock.lockName());
                }
            }
        } else {
            log.info("正在进行数据更新，请稍后再试");
        }
        return null;
    }
}

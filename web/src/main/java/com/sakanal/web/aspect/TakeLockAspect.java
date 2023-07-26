package com.sakanal.web.aspect;

import com.sakanal.web.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Aspect
@Component
public class TakeLockAspect {
    @Resource
    private LockService lockService;
    @Value("${system.lock.pixiv}")
    private String pixivLockName;

    @Pointcut("@annotation(TakeLock)")
    public void toLock() {
    }

    @Around("toLock()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //判断是否可以上锁
        if (!lockService.checkLock(pixivLockName)) {
            try {
                if (lockService.setLock(pixivLockName)) {
                    log.info(pixivLockName + "上锁成功");
                    long start = System.currentTimeMillis();
                    Object proceed = joinPoint.proceed();
                    long end = System.currentTimeMillis();
                    log.info("耗时" + ((end - start) / 1000) + "秒");
                    return proceed;
                } else {
                    log.info(pixivLockName + "上锁失败");
                }
            } finally {
                if (lockService.unsetLock(pixivLockName)) {
                    log.info(pixivLockName + "解锁成功");
                } else {
                    log.info(pixivLockName + "解锁失败");
                }
            }
        } else {
            log.info("正在进行数据更新，请稍后");
        }
        return null;
    }
}

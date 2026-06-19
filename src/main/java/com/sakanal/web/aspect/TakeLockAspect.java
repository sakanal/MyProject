package com.sakanal.web.aspect;

import com.sakanal.web.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 分布式锁切面类
 * 通过AOP实现方法级别的分布式锁控制
 * 使用@TakeLock注解标记需要加锁的方法
 * 
 * 工作流程：
 * 1. 检查锁是否已被占用
 * 2. 如果未占用，尝试获取锁
 * 3. 获取锁成功则执行目标方法
 * 4. 方法执行完成后释放锁
 * 5. 如果锁已被占用，直接返回null
 *
 * @author sakanal
 */
@Slf4j
@Aspect
@Component
public class TakeLockAspect {
    @Resource
    private LockService lockService;

    /**
     * 环绕通知，处理分布式锁的获取和释放
     *
     * @param joinPoint 连接点，包含目标方法的信息
     * @param takeLock  锁注解，包含锁名称等配置
     * @return 目标方法的执行结果
     * @throws Throwable 目标方法抛出的异常
     */
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
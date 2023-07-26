package com.sakanal.web.scheduled;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.entity.Lock;
import com.sakanal.web.service.LockService;
import com.sakanal.web.service.PixivService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Component
public class PixivScheduled {
    @Resource
    private PixivService pixivService;
    @Resource
    private LockService lockService;
    @Value("${system.lock.pixiv}")
    private String pixivLockName;

    @TakeLock
    @Scheduled(cron = "0 0 0/3 * * ? ")
    public void upload() {
        log.info("开始进行自动更新");
        pixivService.updateByNow();
        log.info("自动更新完成");
    }

    @TakeLock
    @Scheduled(cron = "0 0 0/8 * * ?")
    public void again() {
        log.info("开始进行补充更新");
        pixivService.againDownload();
        log.info("补充更新完成");
    }

    @TakeLock
    @Scheduled(cron = "0 59 23 * * ?")
    public void cleanInvalidLock(){
        if (lockService.checkLock(pixivLockName)){
            Lock lock = lockService.getById(pixivLockName);
            LocalDateTime availableTime = LocalDateTimeUtil.of(lock.getAvailableTime());
            if (LocalDateTimeUtil.now().isAfter(availableTime)){
                lockService.unsetLock(pixivLockName);
            }
        }
    }
}


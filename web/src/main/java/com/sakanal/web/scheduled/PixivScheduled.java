package com.sakanal.web.scheduled;

import cn.hutool.core.date.LocalDateTimeUtil;
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

    @Scheduled(cron = "0 0 0/3 * * ? ")
    public void upload() {
        if (!lockService.checkLock(pixivLockName)) {
            try {
                if (lockService.setLock(pixivLockName)) {
                    log.info(pixivLockName + "上锁成功--开始更新数据");
                    pixivService.updateByNow();
                } else {
                    log.info(pixivLockName + "上锁失败");
                }
            } finally {
                if (lockService.unsetLock(pixivLockName)) {
                    log.info(pixivLockName + "解锁成功--更新数据完成");
                } else {
                    log.info(pixivLockName + "解锁失败");
                }
            }
        }else {
            log.info("正在更新中");
        }
    }

    @Scheduled(cron = "0 0 20 * * ?")
    public void again() {
        if (!lockService.checkLock(pixivLockName)) {
            try {
                if (lockService.setLock(pixivLockName)) {
                    log.info(pixivLockName + "上锁成功--开始补充下载");
                    pixivService.againDownload();
                } else {
                    log.info(pixivLockName + "上锁失败--补充下载");
                }
            } finally {
                if (lockService.unsetLock(pixivLockName)) {
                    log.info(pixivLockName + "解锁成功--补充下载完成");
                } else {
                    log.info(pixivLockName + "解锁失败");
                }
            }
        } else {
            log.info("正在更新中");
        }
    }

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


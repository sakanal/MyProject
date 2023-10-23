package com.sakanal.web.scheduled;

import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.service.PixivService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class PixivScheduled {
    @Resource
    private PixivService pixivService;

    @TakeLock(lockName = "pixivLock")
    @Scheduled(cron = "0 0 0/3 * * ? ")
    public void upload() {
        log.info("开始进行自动更新");
        pixivService.updateByNow();
        log.info("自动更新完成");
    }

    @TakeLock(lockName = "pixivLock")
    @Scheduled(cron = "30 0 0/8 * * ?")
    public void again() {
        log.info("开始进行补充更新");
        pixivService.againDownload();
        log.info("补充更新完成");
    }

}


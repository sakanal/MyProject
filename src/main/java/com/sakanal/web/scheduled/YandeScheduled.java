package com.sakanal.web.scheduled;

import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.service.YandeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class YandeScheduled {
    @Resource
    private YandeService yandeService;

    @TakeLock(lockName = "yandeLock")
    @Scheduled(cron = "0 0 1 * * ?")
    public void again() {
        log.info("开始进行补充更新");
        yandeService.againDownload();
        log.info("补充更新完成");
    }

    @TakeLock(lockName = "yandeLock")
    @Scheduled(cron = "0 0 2 * * ?")
    public void upload() {
        log.info("开始进行自动更新");
        yandeService.update();
        log.info("自动更新完成");
    }
}


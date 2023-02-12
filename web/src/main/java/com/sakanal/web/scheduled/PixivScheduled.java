package com.sakanal.web.scheduled;

import com.sakanal.web.service.PixivService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class PixivScheduled {
    @Resource
    private PixivService pixivService;
    @Resource
    private ReentrantLock reentrantLock;

    @Scheduled(cron = "0 0 6 * * ?")
    public void upload() {
        if (reentrantLock.tryLock()){
            try {
                log.info("上锁成功--开始更新数据");
                pixivService.update();
            } finally {
                reentrantLock.unlock();
                log.info("解锁成功--更新数据完成");
            }
        }else {
            log.info("上锁失败--更新数据");
        }
    }
    @Scheduled(cron = "0 0 3 * * ?")
    public void again() {
        if (reentrantLock.tryLock()){
            try {
                log.info("上锁成功--开始补充下载");
                pixivService.againDownload();
            } finally {
                reentrantLock.unlock();
                log.info("解锁成功--补充下载完成");
            }
        }else {
            log.info("上锁失败--补充下载");
        }
    }
}


package com.sakanal.web.scheduled;

import com.sakanal.web.service.PixivService;
import com.sakanal.web.service.YandeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class YandeScheduled {
    @Resource
    private YandeService yandeService;
    @Resource
    private ReentrantLock reentrantLock;

    @Scheduled(cron = "0 0 1 * * ?")
    public void again() {
        if (reentrantLock.tryLock()){
            try {
                log.info("上锁成功--开始补充下载");
                yandeService.againDownload();
            } finally {
                reentrantLock.unlock();
                log.info("解锁成功--补充下载完成");
            }
        }else {
            log.info("上锁失败--补充下载");
        }
    }
//    @Scheduled(cron = "0 0 2 * * ?")
//    public void upload() {
//        if (reentrantLock.tryLock()){
//            try {
//                log.info("上锁成功--开始更新数据");
//            } finally {
//                reentrantLock.unlock();
//                log.info("解锁成功--更新数据完成");
//            }
//        }else {
//            log.info("上锁失败--更新数据");
//        }
//    }
}


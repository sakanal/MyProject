package com.sakanal.web.scheduled;

import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.service.PixivService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Pixiv定时任务调度类
 * 负责定时执行Pixiv相关的自动化任务
 * 
 * 定时任务包括：
 * 1. 自动更新最新作品（每3小时执行一次）
 * 2. 补充下载失败的图片（每8小时执行一次）
 * 
 * @author sakanal
 */
@Slf4j
@Component
public class PixivScheduled {
    @Resource
    private PixivService pixivService;

    /**
     * 定时自动更新最新作品
     * 执行频率：每3小时执行一次（0 0 0/3 * * ?）
     * 使用分布式锁防止并发执行
     */
    @TakeLock(lockName = "pixivLock")
    @Scheduled(cron = "0 0 0/3 * * ? ")
    public void upload() {
        log.info("开始进行自动更新");
        pixivService.updateByNow();
        log.info("自动更新完成");
    }

    /**
     * 定时补充下载失败的图片
     * 执行频率：每8小时执行一次（30 0 0/8 * * ?）
     * 使用分布式锁防止并发执行
     */
    @TakeLock(lockName = "pixivLock")
    @Scheduled(cron = "30 0 0/8 * * ?")
    public void again() {
        log.info("开始进行补充更新");
        pixivService.againDownload();
        log.info("补充更新完成");
    }

}
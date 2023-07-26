package com.sakanal.web.service;

import com.sakanal.web.entity.Lock;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sakanal
 * @since 2023-07-21
 */
public interface LockService extends IService<Lock> {
    /**
     * 上锁
     * @param lockName 锁名
     * @return 上锁成功--true，上锁失败--false
     */
    boolean setLock(String lockName);

    /**
     * 解锁
     * @param lockName 锁名
     * @return 解锁成功--true，解锁失败--false
     */
    boolean unsetLock(String lockName);

    /**
     * 检测上锁结果
     * @param lockName 锁名
     * @return 有效上锁需要等待--true，无效上锁可以尝试上锁--false
     */
    boolean checkLock(String lockName);

    /**
     * 检测是否锁定
     * @param lockName 锁名
     * @return 锁定--true，未锁定--false
     */
    boolean isLock(String lockName);
}

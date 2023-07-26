package com.sakanal.web.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakanal.web.entity.Lock;
import com.sakanal.web.mapper.LockMapper;
import com.sakanal.web.service.LockService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sakanal
 * @since 2023-07-21
 */
@Service
public class LockServiceImpl extends ServiceImpl<LockMapper, Lock> implements LockService {

    @Override
    public boolean setLock(String lockName) {
        return this.save(Lock.setLock(lockName));
    }

    @Override
    public boolean unsetLock(String lockName) {
        return this.removeById(lockName);
    }

    @Override
    public boolean checkLock(String lockName) {
        Lock lock = this.getById(lockName);
        if (lock==null){
            // 不存在锁
            return false;
        }
        LocalDateTime availableTime = LocalDateTimeUtil.of(lock.getAvailableTime());
        boolean after = LocalDateTimeUtil.now().isAfter(availableTime);
        if (after){
            unsetLock(lockName);
            return false;
        }else {
            return true;
        }
    }

    @Override
    public boolean isLock(String lockName) {
        return this.checkLock(lockName);
    }
}

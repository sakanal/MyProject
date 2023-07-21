package com.sakanal.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakanal.web.entity.Lock;
import com.sakanal.web.mapper.LockMapper;
import com.sakanal.web.service.LockService;
import org.springframework.stereotype.Service;

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
        return this.updateById(Lock.setLock(lockName));
    }

    @Override
    public boolean unsetLock(String lockName) {
        return this.updateById(Lock.unsetLock(lockName));
    }

    @Override
    public boolean checkLock(String lockName) {
        Lock lock = this.getById(lockName);
        if (lock == null) {
            return !this.save(new Lock(lockName));
        }
        return lock.getIsLock() == 1;
    }

    @Override
    public boolean isLock(String lockName) {
        return this.checkLock(lockName);
    }
}

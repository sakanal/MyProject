package com.sakanal.web.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author sakanal
 * @since 2023-07-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("lock")
public class Lock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("lock_name")
    private String lockName;

    @TableField("is_lock")
    private Integer isLock;

    public Lock(String lockName) {
        this.lockName = lockName;
        this.isLock = 0;
    }

    public static Lock setLock(String lockName) {
        return new Lock(lockName, 1);
    }

    public static Lock unsetLock(String lockName) {
        return new Lock(lockName, 0);
    }


}

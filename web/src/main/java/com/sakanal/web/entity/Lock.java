package com.sakanal.web.entity;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.ZoneOffset;

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
@TableName("my_lock")
public class Lock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("lock_name")
    private String lockName;

    @TableField("available_time")
    private Long availableTime;

    public static Lock setLock(String lockName) {
        return new Lock(lockName, LocalDateTimeUtil.now().plusHours(24).toInstant(ZoneOffset.of("+8")).toEpochMilli());
    }
}

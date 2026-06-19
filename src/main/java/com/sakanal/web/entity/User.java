package com.sakanal.web.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 用户实体类，对应数据库中的 user 表
 * 用于存储画师信息，包括 Pixiv 和 Yande 等平台的画师
 *
 * @author sakanal
 * @since 2023-01-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("sort")
    private String sort;

    @TableField("type")
    private String type;

    @TableField("is_deleted")
    @TableLogic
    private int isDeleted;

    /**
     * 构造用户对象
     *
     * @param userId   平台用户ID
     * @param userName 用户名
     * @param type     来源类型（Pixiv/Yande）
     */
    public User(Long userId, String userName, String type) {
        this.userId = userId;
        this.userName = userName;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return userId.equals(user.userId) && type.equals(user.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, type);
    }
}
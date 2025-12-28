package com.sakanal.web.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author sakanal
 * @since 2023-01-13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("fail_picture")
public class FailPicture implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("picture_id")
    private Long pictureId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("title")
    private String title;

    @TableField("page_count")
    private Integer pageCount;

    @TableField("src")
    private String src;

    @TableField("status")
    private Integer status;

    @TableField("type")
    private String type;

    public FailPicture(String id) {
        this.userId = Long.valueOf(id);
    }


    public FailPicture(Picture picture) {
        BeanUtils.copyProperties(picture,this);
    }

    public void setUserName(String userName) {
        if (userName!=null){
            this.userName = userName.replace("/","·")
                    .replace("\\","·")
                    .replace(":","·")
                    .replace("*","·")
                    .replace("?","·")
                    .replace("\"","·")
                    .replace("<","·")
                    .replace(">","·")
                    .replace("|","·")
                    .replace(" ","_");
        }else {
            this.userName=null;
        }
    }

    public void setTitle(String title) {
        if (title!=null){
            this.title = title.replace("/","·")
                    .replace("\\","·")
                    .replace(":","·")
                    .replace("*","·")
                    .replace("?","·")
                    .replace("\"","·")
                    .replace("<","·")
                    .replace(">","·")
                    .replace("|","·")
                    .replace(" ","_");
        }else {
            this.title = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FailPicture failPicture = (FailPicture) o;
        return pictureId.equals(failPicture.pictureId) && userName.equals(failPicture.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pictureId, userName);
    }


}

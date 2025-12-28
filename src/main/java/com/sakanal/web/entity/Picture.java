package com.sakanal.web.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@TableName("picture")
public class Picture implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("picture_id")
    private Long pictureId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("src")
    private String src;

    @TableField("title")
    private String title;

    @TableField("page_count")
    private Integer pageCount;

    @TableField("status")
    private Integer status;

    @TableField("type")
    private String type;


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
        Picture picture = (Picture) o;
        return Objects.equals(pictureId, picture.pictureId) && Objects.equals(type, picture.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pictureId, type);
    }
}

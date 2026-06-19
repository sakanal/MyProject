package com.sakanal.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sakanal.web.entity.FailPicture;
import com.sakanal.web.entity.Picture;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sakanal
 * @since 2023-01-13
 */
public interface FailPictureService extends IService<FailPicture> {

    /**
     * 批量保存或更新失败图片
     * @param pictureList 图片列表
     * @return 是否成功
     */
    boolean saveOrUpdateBatch(List<Picture> pictureList);
}

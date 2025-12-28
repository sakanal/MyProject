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

    boolean saveOrUpdateBatch(List<Picture> pictureList);
}

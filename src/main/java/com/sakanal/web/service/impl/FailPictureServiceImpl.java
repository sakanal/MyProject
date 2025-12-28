package com.sakanal.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sakanal.web.entity.FailPicture;
import com.sakanal.web.entity.Picture;
import com.sakanal.web.mapper.FailPictureMapper;
import com.sakanal.web.service.FailPictureService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author sakanal
 * @since 2023-01-13
 */
@Service
public class FailPictureServiceImpl extends ServiceImpl<FailPictureMapper, FailPicture> implements FailPictureService {

    @Override
    public boolean saveOrUpdateBatch(List<Picture> pictureList) {
        List<FailPicture> failPictureList = new ArrayList<>(pictureList.size());
        pictureList.forEach(picture -> failPictureList.add(new FailPicture(picture)));
        return this.saveOrUpdateBatch(failPictureList);
    }
}

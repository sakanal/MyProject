package com.sakanal.web.service;

/**
 * @author sakanal
 */
public interface YandeService {
    /**
     * 下载图片
     * @param tags 搜索标签
     */
    void download(String tags);

    /**
     * 重新下载图片
     */
    void againDownload();

    /**
     * 更新图片
     */
    void update();
}

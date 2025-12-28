package com.sakanal.web.service;

public interface PixivService {
    /**
     * 根据作者Id进行完全下载
     *
     * @param userId 作者Id
     */
    void downloadByUserId(Long userId);

    /**
     * 全量更新，根据数据中的数据获取以及下载的作者数据，并以此为基准获取该作者为下载/记录的画作
     */
    void update();

    /**
     * 根据数据库中下载失败的内容重新下载
     */
    void againDownload();

    /**
     * 局部更新，根据最近更新(第一页)内容进行更新
     */
    void updateByNow();

    /**
     * 更改数据库以及本地存储的文件夹中的用户名
     *
     * @param userId   作者Id
     * @param userName 用户名
     * @return 是否成功
     */
    boolean changeUserName(Long userId, String userName);

    /**
     * 保存作者数据到数据中
     *
     * @param userId 作者Id
     * @return 是否成功
     */
    boolean saveUser(Long userId);

    /**
     * 重置画作状态
     *
     * @param pictureId 画作Id
     */
    void resetState(Long pictureId);
}

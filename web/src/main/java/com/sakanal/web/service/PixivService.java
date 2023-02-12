package com.sakanal.web.service;

import org.springframework.scheduling.annotation.Async;

public interface PixivService {
    void download(Long userId);
    void update();

    void againDownload();

    boolean changeUserName(Long userId, String userName);
}

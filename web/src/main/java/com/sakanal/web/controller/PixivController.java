package com.sakanal.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.PixivService;
import com.sakanal.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/web/pixiv")
public class PixivController {
    @Resource
    private PixivService pixivService;
    @Resource
    private UserService userService;

    @RequestMapping("/downloadById/{userId}")
    public String downloadById(@PathVariable("userId") Long userId) {
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
        String msg;
        if (user != null) {
            msg = "该画师已记录，开始尝试更新";
        } else {
            msg = "开始下载";
        }
        pixivService.download(userId);
        return msg;
    }

    @RequestMapping("/update")
    public String update() {
        pixivService.update();
        return "开始更新";
    }

    @RequestMapping("/againDownload")
    public String againDownload() {
        pixivService.againDownload();
        return "开始补充下载";
    }
}

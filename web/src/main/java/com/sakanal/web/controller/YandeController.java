package com.sakanal.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.UserService;
import com.sakanal.web.service.YandeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/web/yande")
public class YandeController {
    @Resource
    private UserService userService;
    @Resource
    private YandeService yandeService;

    @RequestMapping("/downloadByTag/{tag}")
    public String downloadByTag(@PathVariable String tag){
        String msg="开始下载";
        User user = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, tag)
                .eq(User::getType, SourceConstant.YANDE_SOURCE)
                .last("limit 1")
        ).get(0);
        if (user!=null){
            msg="该标签已记录，开始尝试更新";
        }
        yandeService.download(tag);
        return msg;
    }

    @RequestMapping("/upload")
    public String upload(){
        yandeService.againDownload();
        return "开始更新";
    }
}

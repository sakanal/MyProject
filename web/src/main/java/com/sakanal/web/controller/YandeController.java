package com.sakanal.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.UserService;
import com.sakanal.web.service.YandeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/web/yande")
public class YandeController {
    @Resource
    private UserService userService;
    @Resource
    private YandeService yandeService;

    @TakeLock(lockName = "yandeLock")
    @RequestMapping("/downloadByTag/{tag}")
    public String downloadByTag(@PathVariable String tag){
        String msg="完成下载";
        List<User> userList = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, tag)
                .eq(User::getType, SourceConstant.YANDE_SOURCE)
                .last("limit 1")
        );
        if (!userList.isEmpty()){
            msg="该标签已记录，完成更新";
        }
        yandeService.download(tag);
        return msg;
    }

    @TakeLock(lockName = "yandeLock")
    @RequestMapping("/againDownload")
    public String againDownload(){
        yandeService.againDownload();
        return "完成补充更新";
    }

    @TakeLock(lockName = "yandeLock")
    @RequestMapping("/upload")
    public String upload(){
        yandeService.update();
        return "完成更新";
    }
}

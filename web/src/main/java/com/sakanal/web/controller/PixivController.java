package com.sakanal.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.PixivService;
import com.sakanal.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/web/pixiv")
public class PixivController {
    @Resource
    private PixivService pixivService;
    @Resource
    private UserService userService;

    @TakeLock(lockName = "pixivLock")
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

    @TakeLock(lockName = "pixivLock")
    @RequestMapping("/update")
    public String update() {
        pixivService.update();
        return "开始更新";
    }

    @TakeLock(lockName = "pixivLock")
    @RequestMapping("/againDownload")
    public String againDownload() {
        pixivService.againDownload();
        return "开始补充下载";
    }

    @TakeLock(lockName = "pixivLock")
    @RequestMapping("/saveUser/{userId}")
    public String saveUser(@PathVariable("userId")Long userId){
        List<User> userList = userService.list(new LambdaQueryWrapper<User>().eq(User::getUserId, userId).last("LIMIT 1"));
        String msg;
        if (!userList.isEmpty()){
            msg="该画师已记录";
        }else {
            msg = pixivService.saveUser(userId)?"保存成功":"保存失败";
        }
        return msg;
    }

    @TakeLock(lockName = "pixivLock")
    @RequestMapping("/resetState/{pictureId}")
    public String resetState(@PathVariable("pictureId")Long pictureId){
        pixivService.resetState(pictureId);
        return "重置状态完成";
    }
}

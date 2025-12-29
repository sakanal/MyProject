package com.sakanal.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.aspect.TakeLock;
import com.sakanal.web.config.MyPixivConfig;
import com.sakanal.web.dto.PixivConfigUpdateDto;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.PixivService;
import com.sakanal.web.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/web/pixiv")
public class PixivController {
    @Resource
    private PixivService pixivService;
    @Resource
    private UserService userService;
    @Resource
    private MyPixivConfig myPixivConfig;

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
        pixivService.downloadByUserId(userId);
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

    @TakeLock(lockName = "pixivLock")
    @RequestMapping("/updateLoginUserInfo")
    public String updateLoginUserInfo(){
        return "重置状态完成";
    }
    
    /**
     * 更新Pixiv配置信息
     * @param configUpdateDto 配置更新请求参数
     * @return 更新结果
     */
    @TakeLock(lockName = "pixivLock")
    @PostMapping("/updateConfig")
    public String updateConfig(@RequestBody PixivConfigUpdateDto configUpdateDto){
        try {
            // 更新请求头
            if (configUpdateDto.getRequestHeader() != null && !configUpdateDto.getRequestHeader().isEmpty()) {
                Map<String, String> requestHeader = myPixivConfig.getRequestHeader();
                if (requestHeader != null) {
                    // 合并新的请求头到现有请求头中
                    requestHeader.putAll(configUpdateDto.getRequestHeader());
                    log.info("Pixiv请求头更新成功");
                } else {
                    // 如果现有请求头为null，则直接设置新的请求头
                    myPixivConfig.setRequestHeader(configUpdateDto.getRequestHeader());
                    log.info("Pixiv请求头初始化成功");
                }
            }
            
            // 更新字符集名称
            if (configUpdateDto.getCharsetName() != null && !configUpdateDto.getCharsetName().isEmpty()) {
                myPixivConfig.setCharsetName(configUpdateDto.getCharsetName());
                log.info("Pixiv字符集名称更新成功");
            }
            
            // 更新用户名
            if (configUpdateDto.getUsername() != null && !configUpdateDto.getUsername().isEmpty()) {
                myPixivConfig.setUsername(configUpdateDto.getUsername());
                log.info("Pixiv用户名更新成功");
            }
            
            // 更新密码
            if (configUpdateDto.getPassword() != null && !configUpdateDto.getPassword().isEmpty()) {
                myPixivConfig.setPassword(configUpdateDto.getPassword());
                log.info("Pixiv密码更新成功");
            }
            
            return "配置更新成功";
        } catch (Exception e) {
            log.error("更新Pixiv配置失败: {}", e.getMessage(), e);
            return "配置更新失败: " + e.getMessage();
        }
    }
}

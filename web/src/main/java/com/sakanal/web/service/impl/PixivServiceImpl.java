package com.sakanal.web.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.constant.PictureStatusConstant;
import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.FailPicture;
import com.sakanal.web.entity.Picture;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.FailPictureService;
import com.sakanal.web.service.PictureService;
import com.sakanal.web.service.PixivService;
import com.sakanal.web.service.UserService;
import com.sakanal.web.util.PictureUtils;
import com.sakanal.web.util.PixivUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PixivServiceImpl implements PixivService {
    @Resource
    private PixivUtils pixivUtils;
    @Value("${system.baseDownloadDir}")
    private String baseDownloadDir;
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private FailPictureService failPictureService;

    @Resource
    private ThreadPoolExecutor executor;

    /**
     * 根据userId下载该画师的所有画作，保存画师数据到数据库中(user)，会对画作数据进行初始化到数据库(picture)中，图片状态为默认状态
     * 如果数据库中
     * @param userId 用户id
     */
    @Override
    @Async("threadPoolExecutor")
    public void download(Long userId){
        String userName = getUserName(userId);
        if (StringUtils.hasText(userName)){
            long hasUser = userService.count(new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
            if (hasUser==0){
                User user = new User();
                user.setUserId(userId);
                user.setUserName(userName);
                user.setType(SourceConstant.PIXIV_SOURCE);
                boolean save = userService.save(user);
                log.info("画师数据持久化："+(save?"成功":"失败"));
            }
            List<Picture> pictureList = initPictureList(userId, userName);
            if (pictureList!=null && pictureList.size()>0){
                pictureList = getResultPictureList(userId, pictureList);
                if (pictureList!=null && pictureList.size()>0){
                    List<Picture> list = pictureService.list(new LambdaQueryWrapper<Picture>()
                            .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                            .eq(Picture::getUserId, userId));
                    pictureList.removeAll(list);
                    log.info("画师：" + userName + "\tid：" + userId + "\t" + pictureList.size() + "张新画作");
                    if (pictureList.size()>0){
                        boolean saveBatch = pictureService.saveBatch(pictureList);
                        log.info("画作数据持久化："+(saveBatch?"成功":"失败"));
                        downloadPicture(pictureList);
                    }
                }
            }
        }
    }

    /**
     * 更新数据，从数据库中获取以及下载的作者数据，并以此为基准获取该作者为下载/记录的画作
     * 会对画作数据进行初始化到picture数据库中，图片状态为默认状态
     */
    @Override
    @Async("threadPoolExecutor")
    public void update() {
        //从数据库中获取有关的作者id
        List<User> userList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, SourceConstant.PIXIV_SOURCE));
        userList.forEach(user -> {
            List<Picture> pictureList = initPictureList(user.getUserId(), user.getUserName());
            if (pictureList!=null && pictureList.size()>0){
                LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                        .eq(Picture::getUserId, user.getUserId());
                List<Picture> pictures = pictureService.list(lambdaQueryWrapper);
                pictureList.removeAll(pictures);
                if (pictureList.size()>0){
                    pictureList = getResultPictureList(user.getUserId(),pictureList);
                    if (pictureList!=null && pictureList.size()>0){
                        boolean saveBatch = pictureService.saveBatch(pictureList);
                        log.info("画师：" + user.getUserName() + "\tid：" + user.getUserId() + "\t" + pictureList.size() + "张新画作");
                        log.info("图片数据持久化："+(saveBatch?"成功":"失败"));
                        downloadPicture(pictureList);
                    }
                }else {
                    log.info("画师：" + user.getUserName() + "\tid：" + user.getUserId() + "\t暂无新画作");
                }
            }else {
                log.info("图片初始化失败，请检查网站是否更新");
            }
        });
        log.info("更新完成");
    }

    @Override
//    @Async("threadPoolExecutor")
    public void againDownload(){
        List<Picture> pictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                .and(query->query.eq(Picture::getStatus, PictureStatusConstant.DEFAULT_STATUS)
                        .or().eq(Picture::getStatus, PictureStatusConstant.FAIL_STATUS)));
        if (pictureList != null && pictureList.size() > 0) {
            downloadPicture(pictureList);
            failPictureService.saveOrUpdateBatch(pictureList);
            failPictureService.remove(new LambdaQueryWrapper<FailPicture>()
                    .eq(FailPicture::getType, SourceConstant.PIXIV_SOURCE)
                    .and(query -> query.eq(FailPicture::getStatus, PictureStatusConstant.SUCCESS_STATUS)
                            .or().eq(FailPicture::getStatus, PictureStatusConstant.COVER_STATUS)));
        }
    }

    @Override
//    @Async("threadPoolExecutor")
    public void updateByNow() {
        //https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh&version=b461aaba721300d63f4506a979bf1c3e6c11df13
        String version = RandomUtil.randomString(40);
        InputStream inputStream = pixivUtils.getInputStream("https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh&version=" + version);
        String urlResult = pixivUtils.getUrlResult(inputStream);
        Object body = JSONUtil.parseObj(urlResult).get("body");
        Object page = JSONUtil.parseObj(body).get("page");
        Object ids = JSONUtil.parseObj(page).get("ids");

        // 截取所有的图片Id
        Matcher matcher = Pattern.compile("[0-9]+").matcher(ids.toString());
        List<Picture> pictureList = new ArrayList<>();
        while (matcher.find()) {
            String pictureId = matcher.group();
            Picture picture = new Picture();
            picture.setPictureId(Long.valueOf(pictureId));
            picture.setPageCount(1);
            picture.setType(SourceConstant.PIXIV_SOURCE);
            picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
            pictureList.add(picture);
        }
        if (pictureList.size()<=0){
            return;
        }
        //获取数据库中的对应数据，用来排除已下载或正在下载的图片
        List<Long> pictureIdList = pictureList.stream().map(Picture::getPictureId).collect(Collectors.toList());
        List<Picture> dbPictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .in(Picture::getPictureId, pictureIdList)
                .eq(Picture::getType, SourceConstant.PIXIV_SOURCE));
        // 获取最终可能需要进行下载的图片
        pictureList.removeAll(dbPictureList);
        if (pictureList.size()<=0){
            return;
        }

        // 获取数据库中的用户信息
        Map<Long, String> dbUserList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, SourceConstant.PIXIV_SOURCE))
                .stream().collect(Collectors.toMap(User::getUserId, User::getUserName));

        // 获取最新更新中的用户Id
        pictureList = pictureList.stream().map(picture -> pixivUtils.getPictureInfo(picture.getPictureId())).collect(Collectors.toList());
        Set<Long> userIdList = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());

        // 获取最新更新中被标记的画师Id
        userIdList.retainAll(dbUserList.keySet());
        if (userIdList.size()<=0){
            return;
        }

        pictureList = pictureList.stream().map(picture -> {
            boolean flag = false;
            for (Long userId : userIdList) {
                if (userId.equals(picture.getUserId())) {
                    // 替换成数据库中的userName
                    picture.setUserName(dbUserList.get(userId));
                    flag = true;
                    break;
                }
            }
            return flag ? picture : null;
        }).collect(Collectors.toList());
        // 去除null
        pictureList.removeAll(Collections.singleton(null));
        if (pictureList.size()<=0){
            return;
        }

        ArrayList<Picture> tempPictureList = new ArrayList<>(pictureList);
        pictureList = new ArrayList<>();
        for (Picture picture : tempPictureList) {
            if (picture.getPageCount() == 1) {
                pictureList.add(picture);
            } else {
                for (int i = 0; i < picture.getPageCount(); i++) {
                    Picture resultPicture = getResultPicture(i, picture);
                    pictureList.add(resultPicture);
                }
            }
        }

        pictureService.saveBatch(pictureList);

        downloadPicture(pictureList);
    }

    /**
     * 更改作者名，更改数据库中的数据(user/picture)
     *
     * @param userId      用户id
     * @param newUserName 更新后的用户名
     * @return 更改结果
     */
    @Override
    public boolean changeUserName(Long userId, String newUserName) {
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserId, userId)
                .eq(User::getType, SourceConstant.PIXIV_SOURCE));
        String downloadDir = baseDownloadDir+"\\"+SourceConstant.PIXIV_SOURCE+"\\"+user.getUserName()+"-"+user.getUserId();
        File file = new File(downloadDir);
        if (file.exists()){
            downloadDir = downloadDir.replace(user.getUserName(),newUserName);
            boolean renameTo = file.renameTo(new File(downloadDir));
            if (!renameTo){
                return false;
            }
            CompletableFuture<Void> userUpdateFuture = CompletableFuture.runAsync(() -> {
                user.setUserName(newUserName);
                userService.updateById(user);
            }, executor);
            CompletableFuture<Void> pictureUpdateFuture = CompletableFuture.runAsync(() -> {
                List<Picture> pictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getUserId, userId)
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE));
                if(pictureList.size()>0){
                    pictureList.forEach(picture -> picture.setUserName(newUserName));
                    log.info("图片内容更新结果："+(pictureService.updateBatchById(pictureList)?"成功":"失败"));
                }
            }, executor);

            CompletableFuture<Void> failPictureUpdateFuture = CompletableFuture.runAsync(() -> {
                List<FailPicture> failPictureList = failPictureService.list(new LambdaQueryWrapper<FailPicture>()
                        .eq(FailPicture::getUserId, userId)
                        .eq(FailPicture::getType, SourceConstant.PIXIV_SOURCE));
                if (failPictureList.size()>0){
                    failPictureList.forEach(failPicture -> failPicture.setUserName(newUserName));
                    log.info("补充图片更新结果："+(failPictureService.updateBatchById(failPictureList)?"成功":"失败"));
                }
            }, executor);

            CompletableFuture.allOf(userUpdateFuture,pictureUpdateFuture,failPictureUpdateFuture).join();
            return true;
        }else {
            return false;
        }
    }

    /**
     * 获取该画师的所有作品数据，对结果进行转换  解析数据，获取pictureId，设置userId/userName/PictureId/pageCount/type/status
     * @param userId   当前用户id
     * @param userName 当前用户姓名
     * @return List<Picture> userId/userName/PictureId/pageCount/type/status
     */
    private List<Picture> initPictureList(Long userId, String userName) {
        String allPictureAjaxURL = "https://www.pixiv.net/ajax/user/" + userId + "/profile/all";

        InputStream inputStream = pixivUtils.getInputStream(allPictureAjaxURL);
        if (inputStream == null) {
            return null;
        }
        String result = pixivUtils.getUrlResult(inputStream);
        if (!StringUtils.hasText(result)) {
            log.info("获取所有作品数据失败，请检查网络情况");
            return null;
        }

        Object body = JSONUtil.parseObj(result).get("body");
        Object illusts = JSONUtil.parseObj(body).get("illusts");
        Matcher matcher = Pattern.compile("[0-9]+").matcher(illusts.toString());
        List<Picture> pictureList = new ArrayList<>();
        while (matcher.find()) {
            String pictureId = matcher.group();
            Picture picture = new Picture();
            picture.setUserId(userId);
            picture.setUserName(userName);
            picture.setPictureId(Long.valueOf(pictureId));
            picture.setPageCount(1);
            picture.setType(SourceConstant.PIXIV_SOURCE);
            picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
            pictureList.add(picture);
        }
        return pictureList;
    }

    /**
     * 获取作者名称
     * @param userId 作者id
     * @return 作者名称
     */
    private String getUserName(Long userId){
        String ajaxURL="https://www.pixiv.net/ajax/user/"+userId+"/profile/top";

        InputStream inputStream = pixivUtils.getInputStream(ajaxURL);
        if (inputStream == null) {
            return null;
        }
        String result = pixivUtils.getUrlResult(inputStream);
        if (!StringUtils.hasText(result)) {
            log.info("获取作者名称失败，请检查网络情况");
            return null;
        }
        Object body = JSONUtil.parseObj(result).get("body");
        Object extraData = JSONUtil.parseObj(body).get("extraData");
        Object meta = JSONUtil.parseObj(extraData).get("meta");
        return JSONUtil.parseObj(meta).get("title").toString().replace(" - pixiv", "");

    }

    /**
     * 获取最终的图片列表
     * @param userId 用户id
     * @param pictureList 原始图片列表 userId/userName/PictureId/pageCount/type/status
     * @return 最终图片列表 userId/userName/PictureId/title/src/pageCount/type/status
     */
    private List<Picture> getResultPictureList(Long userId,List<Picture> pictureList){
        List<Picture> resultPictureList = new ArrayList<>();

        //数量过多无法获取数据，需要将数据拆分下载
        int offset = 80;
        boolean flag;
        if (pictureList.size()>offset) {
            int count = (int) Math.ceil(pictureList.size() / (double) offset);
            List<Picture> tempPictureList;
            for (int i = 0; i < count; i++) {
                if (i == count - 1) {
                    tempPictureList = pictureList.subList((offset * i), pictureList.size());
                } else {
                    tempPictureList = pictureList.subList((offset * i), (offset * i + offset));
                }
                flag = getPictureList(userId, tempPictureList, resultPictureList);
                if (flag){
                    return null;
                }
            }
        }else {
            flag = getPictureList(userId, pictureList, resultPictureList);
            if (flag){
                return null;
            }
        }
        return resultPictureList;
    }

    /**
     * 如果当前所需下载画作>80，pixiv无法获取，需要将其拆分获取数据
     * @param userId 用户id
     * @param pictureList 图片列表 userId/userName/PictureId/pageCount/type/status
     * @param resultPictureList 80条数据的图片列表
     * @return 拆分为80条数据的图片列表
     */
    private boolean getPictureList(Long userId, List<Picture> pictureList, List<Picture> resultPictureList) {
        StringBuilder builder= new StringBuilder("https://www.pixiv.net/ajax/user/" + userId + "/profile/illusts?");
        for (Picture picture : pictureList) {
            builder.append("ids[]=").append(picture.getPictureId()).append("&");
        }
        builder.append("work_category=illustManga&is_first_page=1&lang=zh");

        InputStream inputStream = pixivUtils.getInputStream(builder.toString());
        if (inputStream==null) return true;
        String pictureResult = pixivUtils.getUrlResult(inputStream);
        if (!StringUtils.hasText(pictureResult)) return true;

        Object body = JSONUtil.parseObj(pictureResult).get("body");
        Object works = JSONUtil.parseObj(body).get("works");
        JSONObject jsonObject = JSONUtil.parseObj(works);
        for (Picture oldPicture : pictureList) {
            Object value = jsonObject.get(oldPicture.getPictureId().toString());
            JSONObject tempJsonObject = JSONUtil.parseObj(value);

            String title = (String) tempJsonObject.get("title");
            oldPicture.setTitle(title);
            Integer pageCount = (Integer) tempJsonObject.get("pageCount");
            String src = pixivUtils.getRealSrc((String) tempJsonObject.get("url"));
            oldPicture.setSrc(src);
            for (int i = 0; i < pageCount; i++) {
                Picture picture = getResultPicture(i,oldPicture);
                resultPictureList.add(picture);
            }
        }
        return false;
    }

    /**
     * 获取图片组
     * @param i 当前图片的count值
     * @param oldPicture 当前pictureId模板
     * @return 最终图片数据，相同的pictureId，不同的pageCount以及src
     */
    private Picture getResultPicture(int i, Picture oldPicture) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(oldPicture,picture);
        picture.setSrc(oldPicture.getSrc().replace("_p0", "_p" + i));
        picture.setPageCount((i+1));
        return picture;
    }

    /**
     * 正式下载图片 会对下载时间进行统计 更新数据库中的图片下载状态，并将下载失败的图片另存数据库
     * @param pictureList 图片列表 userId/userName/PictureId/pageCount/type/status
     */
    private void downloadPicture(List<Picture> pictureList){
        if (pictureList.size()>0){
            log.info("开始下载");
            long start = System.currentTimeMillis();
            int size = pictureList.size();
            AtomicInteger i= new AtomicInteger(0);
            pictureList.forEach(picture -> {
                InputStream inputStream = pixivUtils.getInputStream(picture);
                if (inputStream!=null){
                    String downloadDir = baseDownloadDir + "\\" + SourceConstant.PIXIV_SOURCE + "\\" + picture.getUserName()+"-"+picture.getUserId()+"\\";
                    boolean downloadResult = PictureUtils.downloadPicture(downloadDir, picture, inputStream, SourceConstant.PIXIV_SOURCE);
                    if (downloadResult){
                        System.out.println(picture);
                        System.out.println(picture.getUserName()+"的第"+(i.incrementAndGet())+"张图片下载完成，剩余"+(size- i.get())+"张图片等待下载");
                        picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                    }else {
                        picture.setStatus(PictureStatusConstant.FAIL_STATUS);
                        FailPicture failPicture = new FailPicture(picture);
                        failPictureService.saveOrUpdate(failPicture);
                    }
                }else {
                    picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
                    FailPicture failPicture = new FailPicture(picture);
                    failPictureService.saveOrUpdate(failPicture);
                }
                pictureService.saveOrUpdate(picture);
            });
            long end = System.currentTimeMillis();
            System.out.println("总需要下载" + pictureList.size() + "份图片");
            System.out.println("下载完成，耗时：" + (end - start) / 1000 + "秒");
        }

    }

}

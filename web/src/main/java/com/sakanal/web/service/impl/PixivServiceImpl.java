package com.sakanal.web.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.beans.factory.annotation.Value;
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
     *
     * @param userId 用户id
     */
    @Override
//    @Async("threadPoolExecutor")
    public void download(Long userId) {
        // 获取用户名
        String userName = pixivUtils.getUserName(userId);
        if (StringUtils.hasText(userName)) {
            long hasUser = userService.count(new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
            if (hasUser == 0) {
                // 如果数据库中没有该用户的数据则进行新增数据
                User user = new User();
                user.setUserId(userId);
                user.setUserName(userName);
                user.setType(SourceConstant.PIXIV_SOURCE);
                boolean save = userService.save(user);
                log.info("画师数据持久化：" + (save ? "成功" : "失败"));
                if (!save) {
                    log.error("请重新再试，参数为：{}", userId);
                    return;
                }
            } else {
                log.info("该作者已被标记，正在尝试更新数据");
            }
            // 根据用户ID和用户名进行图片列表初始化，会获取除url和title的所有数据，对于图片组数据未进行处理
            List<Picture> pictureList = pixivUtils.initPictureList(userId, userName);
            if (pictureList != null && !pictureList.isEmpty()) {
                // 获取正式使用的图片列表数据，并对图片组数据完成处理
                pictureList = getResultPictureList(userId, pictureList);
                if (pictureList != null && !pictureList.isEmpty()) {
                    List<Picture> list = pictureService.list(new LambdaQueryWrapper<Picture>()
                            .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                            .eq(Picture::getUserId, userId));
                    // 剔除已完成下载的图片数据
                    pictureList.removeAll(list);
                    log.info("画师：" + userName + "\tid：" + userId + "\t" + pictureList.size() + "张新画作");
                    if (!pictureList.isEmpty()) {
                        // 存在未在数据库中进行数据保存（未下载）的图片数据则进行数据保存并下载
                        boolean saveBatch = pictureService.saveBatch(pictureList);
                        log.info("画作数据持久化：" + (saveBatch ? "成功" : "失败"));
                        if (!saveBatch) {
                            log.error("请重新再试，参数为：{}", userId);
                            return;
                        }
                        downloadPicture(pictureList, true);
                    } else {
                        log.info("暂无画作数据更新");
                    }
                } else {
                    log.error("画作完整数据获取失败");
                }
            } else {
                log.error("画作数据初始化失败");
            }
        }
    }

    /**
     * 更新数据，从数据库中获取以及下载的作者数据，并以此为基准获取该作者为下载/记录的画作
     * 会对画作数据进行初始化到picture数据库中，图片状态为默认状态
     */
    @Override
//    @Async("threadPoolExecutor")
    public void update() {
        //从数据库中获取有关的作者id
        List<User> userList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, SourceConstant.PIXIV_SOURCE));
        userList.forEach(user -> {
            // 针对每一个作者获取所有图片列表数据的基础数据，此时数据不存在url和title，并且未对图片组数据进行处理
            List<Picture> pictureList = pixivUtils.initPictureList(user.getUserId(), user.getUserName());
            if (pictureList != null && !pictureList.isEmpty()) {
                LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                        .eq(Picture::getUserId, user.getUserId());
                List<Picture> pictures = pictureService.list(lambdaQueryWrapper);
                // 剔除已下载的图片数据
                pictureList.removeAll(pictures);
                if (!pictureList.isEmpty()) {
                    // 获取最终所需的图片数据，并对图片组数据进行处理
                    pictureList = getResultPictureList(user.getUserId(), pictureList);
                    if (pictureList != null && !pictureList.isEmpty()) {
                        boolean saveBatch = pictureService.saveBatch(pictureList);
                        log.info("画师：" + user.getUserName() + "\tid：" + user.getUserId() + "\t" + pictureList.size() + "张新画作");
                        log.info("图片数据持久化：" + (saveBatch ? "成功" : "失败"));
                        if (!saveBatch) {
                            log.error("请重新再试，参数为：{}", user);
                            return;
                        }
                        downloadPicture(pictureList,(pictures.size() < 1 || pictureList.size() > 100));
                    }
                } else {
                    log.info("画师：" + user.getUserName() + "\tid：" + user.getUserId() + "\t暂无新画作");
                }
            } else {
                log.info("图片初始化失败，请检查网站是否更新");
            }
        });
        log.info("更新完成");
    }

    @Override
//    @Async("threadPoolExecutor")
    public void againDownload() {
        // 获取未完成下载或下载失败的所有图片数据
        List<Picture> pictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                .and(query -> query.eq(Picture::getStatus, PictureStatusConstant.DEFAULT_STATUS)
                        .or().eq(Picture::getStatus, PictureStatusConstant.FAIL_STATUS)));
        if (pictureList != null && !pictureList.isEmpty()) {
            // 如果存在数据则进行下载
            downloadPicture(pictureList);
            failPictureService.saveOrUpdateBatch(pictureList);
            failPictureService.remove(new LambdaQueryWrapper<FailPicture>()
                    .eq(FailPicture::getType, SourceConstant.PIXIV_SOURCE)
                    .and(query -> query.eq(FailPicture::getStatus, PictureStatusConstant.SUCCESS_STATUS)
                            .or().eq(FailPicture::getStatus, PictureStatusConstant.COVER_STATUS)));
        } else {
            log.info("暂无所需重新下载的画作");
        }
    }

    @Override
//    @Async("threadPoolExecutor")
    public void updateByNow() {
        //https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh&version=b461aaba721300d63f4506a979bf1c3e6c11df13
        String version = RandomUtil.randomString(40);
        InputStream inputStream = pixivUtils.getInputStream("https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh&version=" + version);
        if (inputStream == null) {
            log.error("获取更新数据失败");
            return;
        }
        Matcher matcher;
        try {
            String urlResult = pixivUtils.getUrlResult(inputStream);
            Object body = JSONUtil.parseObj(urlResult).get("body");
            Object page = JSONUtil.parseObj(body).get("page");
            Object ids = JSONUtil.parseObj(page).get("ids");

            // 截取所有的图片Id
            matcher = Pattern.compile("[0-9]+").matcher(ids.toString());
        } catch (Exception e) {
            log.error("未获取到正确的数据,解析失败", e);
            return;
        }
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
        if (pictureList.isEmpty()) {
            log.info("数据异常，没有获取到最新更新的任何相关数据");
            return;
        }
        //获取数据库中的对应数据，用来排除已下载或正在下载的图片
        List<Long> pictureIdList = pictureList.stream().map(Picture::getPictureId).collect(Collectors.toList());
        List<Picture> dbPictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .in(Picture::getPictureId, pictureIdList)
                .eq(Picture::getType, SourceConstant.PIXIV_SOURCE));
        // 获取最终可能需要进行下载的图片
        pictureList.removeAll(dbPictureList);
        if (pictureList.isEmpty()) {
            log.info("此页面所有数据已经完成更新");
            return;
        }
        // 获取最新更新中的用户Id
        pictureList = pictureList.stream()
                .map(picture -> pixivUtils.getPictureInfo(picture.getPictureId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (pictureList.isEmpty()) {
            log.info("获取最新数据为空");
            return;
        }
        Set<Long> userIdList = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());

        // 获取数据库中的用户信息
        Map<Long, String> dbUserList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, SourceConstant.PIXIV_SOURCE))
                .stream().collect(Collectors.toMap(User::getUserId, User::getUserName));

        // 获取最新更新中被标记的画师Id
        userIdList.retainAll(dbUserList.keySet());
        if (userIdList.isEmpty()) {
            log.info("暂无所需更新的数据");
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
        if (pictureList.isEmpty()) {
            log.info("被标记的画师更新已经完成下载");
            return;
        }

        ArrayList<Picture> tempPictureList = new ArrayList<>(pictureList);
        pictureList = new ArrayList<>();
        for (Picture picture : tempPictureList) {
            if (picture.getPageCount() == 1) {
                pictureList.add(picture);
            } else {
                for (int i = 0; i < picture.getPageCount(); i++) {
                    Picture resultPicture = pixivUtils.getResultPicture(i, picture);
                    pictureList.add(resultPicture);
                }
            }
        }

        pictureService.saveBatch(pictureList);

        downloadPicture(pictureList);
        log.info("下载更新完成");
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
        String downloadDir = baseDownloadDir + "\\" + SourceConstant.PIXIV_SOURCE + "\\" + user.getUserName() + "-" + user.getUserId();
        File file = new File(downloadDir);
        if (file.exists()) {
            downloadDir = downloadDir.replace(user.getUserName(), newUserName);
            boolean renameTo = file.renameTo(new File(downloadDir));
            if (!renameTo) {
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
                if (!pictureList.isEmpty()) {
                    pictureList.forEach(picture -> picture.setUserName(newUserName));
                    log.info("图片内容更新结果：" + (pictureService.updateBatchById(pictureList) ? "成功" : "失败"));
                }
            }, executor);

            CompletableFuture<Void> failPictureUpdateFuture = CompletableFuture.runAsync(() -> {
                List<FailPicture> failPictureList = failPictureService.list(new LambdaQueryWrapper<FailPicture>()
                        .eq(FailPicture::getUserId, userId)
                        .eq(FailPicture::getType, SourceConstant.PIXIV_SOURCE));
                if (!failPictureList.isEmpty()) {
                    failPictureList.forEach(failPicture -> failPicture.setUserName(newUserName));
                    log.info("补充图片更新结果：" + (failPictureService.updateBatchById(failPictureList) ? "成功" : "失败"));
                }
            }, executor);

            CompletableFuture.allOf(userUpdateFuture, pictureUpdateFuture, failPictureUpdateFuture).join();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean saveUser(Long userId) {
        String userName = pixivUtils.getUserName(userId);
        boolean save = false;
        if (StringUtils.hasText(userName)) {
            User user = new User();
            user.setUserId(userId);
            user.setUserName(userName);
            user.setType(SourceConstant.PIXIV_SOURCE);
            save = userService.save(user);
        }
        return save;
    }

    @Override
    public void resetState(Long pictureId) {
        pictureService.update(new LambdaUpdateWrapper<Picture>()
                .eq(Picture::getPictureId, pictureId)
                .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                .set(Picture::getStatus, PictureStatusConstant.FAIL_STATUS));
    }


    /**
     * 如果list中的数据超过80个则进行分割，并调用方法获取下载所需最终的图片列表信息
     *
     * @param userId      用户id
     * @param pictureList 原始图片列表 userId/userName/PictureId/pageCount/type/status
     * @return 最终图片列表 userId/userName/PictureId/title/src/pageCount/type/status
     */
    private List<Picture> getResultPictureList(Long userId, List<Picture> pictureList) {
        List<Picture> resultPictureList = new ArrayList<>();

        //数量过多无法获取数据，需要将数据拆分下载
        int offset = 80;
        boolean flag;
        if (pictureList.size() > offset) {
            int count = (int) Math.ceil(pictureList.size() / (double) offset);
            List<Picture> tempPictureList;
            for (int i = 0; i < count; i++) {
                if (i == count - 1) {
                    tempPictureList = pictureList.subList((offset * i), pictureList.size());
                } else {
                    tempPictureList = pictureList.subList((offset * i), (offset * i + offset));
                }
                flag = getPictureList(userId, tempPictureList, resultPictureList);
                if (flag) {
                    return null;
                }
            }
        } else {
            flag = getPictureList(userId, pictureList, resultPictureList);
            if (flag) {
                return null;
            }
        }
        return resultPictureList;
    }

    /**
     * 获取下载图片所需的完整信息，在此方法中list必须<=80，同时在此方法中会对图片组获取所有的对应数据（pageCount）
     *
     * @param userId            用户id
     * @param pictureList       图片列表 userId/userName/PictureId/pageCount/type/status
     * @param resultPictureList 80条数据的图片列表
     * @return 1-80条下载图片所需的相关数据，如果存在图片组数据则有可能会>80条数据
     */
    private boolean getPictureList(Long userId, List<Picture> pictureList, List<Picture> resultPictureList) {
        StringBuilder builder = new StringBuilder("https://www.pixiv.net/ajax/user/" + userId + "/profile/illusts?");
        for (Picture picture : pictureList) {
            builder.append("ids[]=").append(picture.getPictureId()).append("&");
        }
        builder.append("work_category=illustManga&is_first_page=1&lang=zh");
        // 调用url获取页面数据
        InputStream inputStream = pixivUtils.getInputStream(builder.toString());
        if (inputStream == null) return true;
        // 解析页面数据并转成字符串用于后续获取实际有效数据
        String pictureResult = pixivUtils.getUrlResult(inputStream);
        if (!StringUtils.hasText(pictureResult)) return true;

        JSONObject jsonObject;
        try {
            Object body = JSONUtil.parseObj(pictureResult).get("body");
            Object works = JSONUtil.parseObj(body).get("works");
            jsonObject = JSONUtil.parseObj(works);
        } catch (Exception e) {
            log.error("pictureResult = {}", pictureResult, e);
            return true;
        }
        for (Picture oldPicture : pictureList) {
            Object value = jsonObject.get(oldPicture.getPictureId().toString());
            JSONObject tempJsonObject = JSONUtil.parseObj(value);

            String title = (String) tempJsonObject.get("title");
            oldPicture.setTitle(title);
            Integer pageCount = (Integer) tempJsonObject.get("pageCount");
            String src = pixivUtils.getRealSrc((String) tempJsonObject.get("url"));
            oldPicture.setSrc(src);
            // 获取该picture组中的所有相关连接
            for (int i = 0; i < pageCount; i++) {
                Picture picture = pixivUtils.getResultPicture(i, oldPicture);
                resultPictureList.add(picture);
            }
        }
        return false;
    }


    /**
     * 正式下载图片 会对下载时间进行统计 更新数据库中的图片下载状态，并将下载失败的图片另存数据库
     *
     * @param pictureList 图片列表 userId/userName/PictureId/pageCount/type/status
     * @param userFlag  是否需要隔离用户下载
     */
    private void downloadPicture(List<Picture> pictureList, boolean userFlag) {
        if (!pictureList.isEmpty()) {
            log.info("开始下载");
            long start = System.currentTimeMillis();
            int size = pictureList.size();
            AtomicInteger i = new AtomicInteger(0);
            pictureList.forEach(picture -> {
                InputStream inputStream = pixivUtils.getInputStream(picture);
                if (inputStream != null) {
                    String downloadDir = baseDownloadDir + "\\" + SourceConstant.PIXIV_SOURCE + "\\" + picture.getUserName() + "-" + picture.getUserId() + "\\";
                    boolean downloadResult = PictureUtils.downloadPicture(downloadDir, picture, inputStream, SourceConstant.PIXIV_SOURCE, userFlag);
                    if (downloadResult) {
                        System.out.println(picture);
                        System.out.println(picture.getUserName() + "的第" + (i.incrementAndGet()) + "张图片下载完成，剩余" + (size - i.get()) + "张图片等待下载");
                        picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                    } else {
                        picture.setStatus(PictureStatusConstant.FAIL_STATUS);
                        FailPicture failPicture = new FailPicture(picture);
                        failPictureService.saveOrUpdate(failPicture);
                    }
                } else {
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

    /**
     * 正式下载图片 会对下载时间进行统计 更新数据库中的图片下载状态，并将下载失败的图片另存数据库，默认不会隔离用户下载
     * @param pictureList 图片列表 userId/userName/PictureId/pageCount/type/status
     */
    private void downloadPicture(List<Picture> pictureList) {
        downloadPicture(pictureList, false);
    }

}

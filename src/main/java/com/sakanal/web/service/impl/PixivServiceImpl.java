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

/**
 * PixivService接口实现类，负责处理Pixiv相关的业务逻辑
 * <p>
 * 主要功能包括：
 * 1. 根据用户ID下载画师作品
 * 2. 批量更新画师作品
 * 3. 重试下载失败的作品
 * 4. 更新最新作品
 * 5. 更改画师名称
 * 6. 重置作品下载状态
 * </p>
 * 
 * @author sakanal
 */
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
     * 根据userId下载该画师的所有画作
     * <p>
     * 功能流程：
     * 1. 验证并保存画师信息到数据库(user表)
     * 2. 初始化画作列表数据
     * 3. 处理图片组数据
     * 4. 剔除已完成下载的画作
     * 5. 保存新增画作到数据库并下载
     * </p>
     * 
     * @param userId 画师的Pixiv用户ID
     */
    @Override
//    @Async("threadPoolExecutor")
    public void download(Long userId) {
        // 获取用户名
        String artistName = pixivUtils.getUserName(userId);
        if (StringUtils.hasText(artistName)) {
            long existingArtistCount = userService.count(new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
            if (existingArtistCount == 0) {
                // 如果数据库中没有该用户的数据则进行新增数据
                User artist = new User();
                artist.setUserId(userId);
                artist.setUserName(artistName);
                artist.setType(SourceConstant.PIXIV_SOURCE);
                boolean isSaved = userService.save(artist);
                log.info("画师数据持久化：" + (isSaved ? "成功" : "失败"));
                if (!isSaved) {
                    log.error("请重新再试，参数为：{}", userId);
                    return;
                }
            } else {
                log.info("该作者已被标记，正在尝试更新数据");
            }
            // 根据用户ID和用户名进行图片列表初始化，会获取除url和title的所有数据，对于图片组数据未进行处理
            List<Picture> initialPictureList = pixivUtils.initPictureList(userId, artistName);
            if (initialPictureList != null && !initialPictureList.isEmpty()) {
                // 获取正式使用的图片列表数据，并对图片组数据完成处理
                List<Picture> processedPictureList = getResultPictureList(userId, initialPictureList);
                if (processedPictureList != null && !processedPictureList.isEmpty()) {
                    List<Picture> existingPictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                            .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                            .eq(Picture::getUserId, userId));
                    // 剔除已完成下载的图片数据
                    processedPictureList.removeAll(existingPictureList);
                    log.info("画师：" + artistName + "\tid：" + userId + "\t" + processedPictureList.size() + "张新画作");
                    if (!processedPictureList.isEmpty()) {
                        // 存在未在数据库中进行数据保存（未下载）的图片数据则进行数据保存并下载
                        boolean isBatchSaved = pictureService.saveBatch(processedPictureList);
                        log.info("画作数据持久化：" + (isBatchSaved ? "成功" : "失败"));
                        if (!isBatchSaved) {
                            log.error("请重新再试，参数为：{}", userId);
                            return;
                        }
                        downloadPicture(processedPictureList, true);
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
     * 批量更新所有已关注画师的作品
     * <p>
     * 功能流程：
     * 1. 获取数据库中所有Pixiv画师信息
     * 2. 为每个画师获取最新作品列表
     * 3. 剔除已下载的作品
     * 4. 处理图片组数据
     * 5. 保存新增作品到数据库并下载
     * </p>
     */
    @Override
//    @Async("threadPoolExecutor")
    public void update() {
        //从数据库中获取所有关注的Pixiv画师
        List<User> followedArtistList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, SourceConstant.PIXIV_SOURCE));
        followedArtistList.forEach(artist -> {
            // 针对每一个画师获取所有图片列表数据的基础数据，此时数据不存在url和title，并且未对图片组数据进行处理
            List<Picture> initialPictureList = pixivUtils.initPictureList(artist.getUserId(), artist.getUserName());
            if (initialPictureList != null && !initialPictureList.isEmpty()) {
                LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                        .eq(Picture::getUserId, artist.getUserId());
                List<Picture> existingPictureList = pictureService.list(lambdaQueryWrapper);
                // 剔除已下载的图片数据
                List<Picture> newPictureList = new ArrayList<>(initialPictureList);
                newPictureList.removeAll(existingPictureList);
                if (!newPictureList.isEmpty()) {
                    // 获取最终所需的图片数据，并对图片组数据进行处理
                    List<Picture> processedPictureList = getResultPictureList(artist.getUserId(), newPictureList);
                    if (processedPictureList != null && !processedPictureList.isEmpty()) {
                        boolean isBatchSaved = pictureService.saveBatch(processedPictureList);
                        log.info("画师：" + artist.getUserName() + "\tid：" + artist.getUserId() + "\t" + processedPictureList.size() + "张新画作");
                        log.info("图片数据持久化：{}", isBatchSaved ? "成功" : "失败");
                        if (!isBatchSaved) {
                            log.error("请重新再试，参数为：{}", artist);
                            return;
                        }
                        // 如果是新关注的画师（没有已下载图片）或者新画作数量超过100，则使用用户模式下载
                        boolean useUserMode = (existingPictureList.size() < 1 || processedPictureList.size() > 100);
                        downloadPicture(processedPictureList, useUserMode);
                    }
                } else {
                    log.info("画师：{}\tid：{}\t暂无新画作", artist.getUserName(), artist.getUserId());
                }
            } else {
                log.info("图片初始化失败，请检查网站是否更新");
            }
        });
        log.info("更新完成");
    }

    /**
     * 重试下载失败或未完成的图片
     * <p>
     * 功能流程：
     * 1. 获取状态为默认或失败的图片
     * 2. 重新下载这些图片
     * 3. 更新下载成功的图片状态
     * 4. 保存或更新失败图片记录
     * 5. 移除下载成功的失败记录
     * </p>
     */
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

    /**
     * 更新Pixiv最新作品
     * <p>
     * 功能流程：
     * 1. 获取Pixiv最新作品列表
     * 2. 解析获取作品ID列表
     * 3. 剔除已下载的作品
     * 4. 获取作品详细信息
     * 5. 筛选已关注画师的作品
     * 6. 处理图片组数据
     * 7. 保存新增作品到数据库并下载
     * </p>
     */
    @Override
//    @Async("threadPoolExecutor")
    public void updateByNow() {
        // 1. 获取最新作品ID列表
        List<Picture> latestPictureList = getLatestPictureIdList();
        if (latestPictureList == null || latestPictureList.isEmpty()) {
            return;
        }

        // 2. 过滤已下载的作品
        List<Picture> newPictureList = filterDownloadedPictures(latestPictureList);
        if (newPictureList.isEmpty()) {
            log.info("此页面所有数据已经完成更新");
            return;
        }

        // 3. 获取作品详细信息
        List<Picture> detailedPictureList = getDetailedPictureInfo(newPictureList);
        if (detailedPictureList.isEmpty()) {
            log.info("获取最新数据为空");
            return;
        }

        // 4. 筛选已关注画师的作品
        List<Picture> followedArtistPictureList = filterFollowedArtistPictures(detailedPictureList);
        if (followedArtistPictureList.isEmpty()) {
            log.info("被标记的画师更新已经完成下载");
            return;
        }

        // 5. 处理图片组数据
        List<Picture> finalPictureList = processPictureGroups(followedArtistPictureList);
        if (finalPictureList.isEmpty()) {
            log.info("处理图片组后没有需要下载的作品");
            return;
        }

        // 6. 保存并下载作品
        saveAndDownloadPictures(finalPictureList);
        log.info("下载更新完成");
    }

    /**
     * 获取最新作品的ID列表
     * @return 包含作品ID的图片列表
     */
    private List<Picture> getLatestPictureIdList() {
        String version = RandomUtil.randomString(40);
        String url = "https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh&version=" + version;
        InputStream inputStream = pixivUtils.getInputStream(url);
        if (inputStream == null) {
            log.error("获取更新数据失败");
            return null;
        }

        try {
            String urlResult = pixivUtils.getUrlResult(inputStream);
            Object body = JSONUtil.parseObj(urlResult).get("body");
            Object page = JSONUtil.parseObj(body).get("page");
            Object ids = JSONUtil.parseObj(page).get("ids");

            // 截取所有的图片Id
            Matcher matcher = Pattern.compile("[0-9]+").matcher(ids.toString());
            List<Picture> pictureList = new ArrayList<>();
            while (matcher.find()) {
                String pictureIdStr = matcher.group();
                Picture picture = new Picture();
                picture.setPictureId(Long.valueOf(pictureIdStr));
                picture.setPageCount(1);
                picture.setType(SourceConstant.PIXIV_SOURCE);
                picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
                pictureList.add(picture);
            }

            if (pictureList.isEmpty()) {
                log.info("数据异常，没有获取到最新更新的任何相关数据");
            }
            return pictureList;
        } catch (Exception e) {
            log.error("未获取到正确的数据,解析失败", e);
            return null;
        }
    }

    /**
     * 过滤已下载的作品
     * @param pictureList 最新作品列表
     * @return 未下载的作品列表
     */
    private List<Picture> filterDownloadedPictures(List<Picture> pictureList) {
        List<Long> pictureIdList = pictureList.stream()
                .map(Picture::getPictureId)
                .collect(Collectors.toList());
        List<Picture> dbPictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .in(Picture::getPictureId, pictureIdList)
                .eq(Picture::getType, SourceConstant.PIXIV_SOURCE));

        List<Picture> newPictureList = new ArrayList<>(pictureList);
        newPictureList.removeAll(dbPictureList);
        return newPictureList;
    }

    /**
     * 获取作品详细信息
     * @param pictureList 作品列表
     * @return 包含详细信息的作品列表
     */
    private List<Picture> getDetailedPictureInfo(List<Picture> pictureList) {
        return pictureList.stream()
                .map(picture -> pixivUtils.getPictureInfo(picture.getPictureId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 筛选已关注画师的作品
     * @param pictureList 作品列表
     * @return 已关注画师的作品列表
     */
    private List<Picture> filterFollowedArtistPictures(List<Picture> pictureList) {
        // 获取数据库中的用户信息
        Map<Long, String> followedArtistMap = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getType, SourceConstant.PIXIV_SOURCE))
                .stream()
                .collect(Collectors.toMap(User::getUserId, User::getUserName));

        // 获取最新更新中被标记的画师Id
        Set<Long> followedArtistIds = followedArtistMap.keySet();

        return pictureList.stream()
                .filter(picture -> followedArtistIds.contains(picture.getUserId()))
                .peek(picture -> picture.setUserName(followedArtistMap.get(picture.getUserId())))
                .collect(Collectors.toList());
    }

    /**
     * 处理图片组数据
     * @param pictureList 作品列表
     * @return 处理后的作品列表
     */
    private List<Picture> processPictureGroups(List<Picture> pictureList) {
        List<Picture> finalPictureList = new ArrayList<>();
        for (Picture picture : pictureList) {
            if (picture.getPageCount() == 1) {
                finalPictureList.add(picture);
            } else {
                for (int i = 0; i < picture.getPageCount(); i++) {
                    Picture resultPicture = pixivUtils.getResultPicture(i, picture);
                    finalPictureList.add(resultPicture);
                }
            }
        }
        return finalPictureList;
    }

    /**
     * 保存并下载作品
     * @param pictureList 作品列表
     */
    private void saveAndDownloadPictures(List<Picture> pictureList) {
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
     * @param useUserMode 是否需要隔离用户下载
     */
    private void downloadPicture(List<Picture> pictureList, boolean useUserMode) {
        if (!pictureList.isEmpty()) {
            log.info("开始下载");
            long startTime = System.currentTimeMillis();
            int totalCount = pictureList.size();
            AtomicInteger completedCount = new AtomicInteger(0);
            
            pictureList.forEach(picture -> {
                InputStream inputStream = pixivUtils.getInputStream(picture);
                if (inputStream != null) {
                    String downloadDir = baseDownloadDir + "\\" + SourceConstant.PIXIV_SOURCE + "\\" + picture.getUserName() + "-" + picture.getUserId() + "\\";
                    boolean isDownloaded = PictureUtils.downloadPicture(downloadDir, picture, inputStream, SourceConstant.PIXIV_SOURCE, useUserMode);
                    
                    if (isDownloaded) {
                        System.out.println(picture);
                        System.out.println(picture.getUserName() + "的第" + (completedCount.incrementAndGet()) + "张图片下载完成，剩余" + (totalCount - completedCount.get()) + "张图片等待下载");
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
            
            long endTime = System.currentTimeMillis();
            System.out.println("总需要下载" + totalCount + "份图片");
            System.out.println("下载完成，耗时：" + (endTime - startTime) / 1000 + "秒");
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

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
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public void downloadByUserId(Long userId) {
        // 获取用户名
        String artistName = pixivUtils.getUserName(userId);
        if (StringUtils.hasText(artistName)) {
            User existingUser = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserId, userId).last("limit 1"));
            if (existingUser == null) {
                // 如果数据库中没有该用户的数据则进行新增数据
                User artist = new User(userId, artistName, SourceConstant.PIXIV_SOURCE);
                boolean isSaved = userService.save(artist);
                log.info("画师数据持久化：{}", isSaved ? "成功" : "失败");
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
                // 获取已存在的图片列表
                List<Picture> existingPictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                        .eq(Picture::getUserId, userId));
                // 处理新图片
                processNewPictures(userId, artistName, initialPictureList, existingPictureList, true);
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
    public void update() {
        //从数据库中获取所有关注的Pixiv画师
        List<User> followedArtistList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, SourceConstant.PIXIV_SOURCE));
        followedArtistList.forEach(artist -> {
            // 针对每一个画师获取所有图片列表数据的基础数据，此时数据不存在url和title，并且未对图片组数据进行处理
            List<Picture> initialPictureList = pixivUtils.initPictureList(artist.getUserId(), artist.getUserName());
            if (initialPictureList != null && !initialPictureList.isEmpty()) {
                // 获取已存在的图片列表
                List<Picture> existingPictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE)
                        .eq(Picture::getUserId, artist.getUserId()));
                // 处理新图片，根据情况决定是否使用用户模式下载
                boolean useUserMode = existingPictureList.isEmpty();
                processNewPictures(artist.getUserId(), artist.getUserName(), initialPictureList, existingPictureList, useUserMode);
            } else {
                log.info("图片初始化失败，请检查网站是否更新");
            }
        });
        log.info("更新完成");
    }

    /**
     * 处理新图片的公共方法
     * <p>
     * 功能流程：
     * 1. 剔除已下载的图片数据
     * 2. 获取最终所需的图片数据，并对图片组数据进行处理
     * 3. 保存新增作品到数据库
     * 4. 下载图片
     * </p>
     *
     * @param userId              用户ID
     * @param userName            用户名
     * @param initialPictureList  初始图片列表
     * @param existingPictureList 已存在的图片列表
     * @param defaultUseUserMode  默认是否使用用户模式下载
     */
    private void processNewPictures(Long userId, String userName, List<Picture> initialPictureList, List<Picture> existingPictureList, boolean defaultUseUserMode) {
        // 剔除已下载的图片数据
        List<Picture> newPictureList = new ArrayList<>(initialPictureList);
        newPictureList.removeAll(existingPictureList);
        if (!newPictureList.isEmpty()) {
            // 获取最终所需的图片数据，并对图片组数据进行处理
            List<Picture> processedPictureList = getResultPictureList(userId, newPictureList);
            if (!processedPictureList.isEmpty()) {
                boolean isBatchSaved = pictureService.saveBatch(processedPictureList);
                log.info("画师：{}\tid：{}\t{}张新画作", userName, userId, processedPictureList.size());
                log.info("图片数据持久化：{}", isBatchSaved ? "成功" : "失败");
                if (isBatchSaved) {
                    // 根据图片数量决定是否使用用户模式下载
                    boolean useUserMode = defaultUseUserMode || processedPictureList.size() > 100;
                    downloadPicture(processedPictureList, useUserMode);
                } else {
                    log.error("请重新再试，参数为：userId={}, userName={}", userId, userName);
                }
            }
        } else {
            log.info("画师：{}\tid：{}\t暂无新画作", userName, userId);
        }
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
     * 从JSON字符串中按照路径获取值
     *
     * @param jsonStr JSON字符串
     * @param path    路径数组
     * @return 路径对应的值
     */
    private Object getJsonValue(String jsonStr, String... path) {
        if (!StringUtils.hasText(jsonStr) || path == null || path.length == 0) {
            return null;
        }

        try {
            Object current = JSONUtil.parseObj(jsonStr);
            for (String key : path) {
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).get(key);
                } else {
                    return null;
                }
            }
            return current;
        } catch (Exception e) {
            log.error("JSON路径解析失败, jsonStr={}, path={}, errorMessage={}", jsonStr, Arrays.toString(path), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取最新作品ID列表
     *
     * @return 最新作品ID列表
     */
    private List<Picture> getLatestPictureIdList() {
        String version = RandomUtil.randomString(40);
        String url = "https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh&version=" + version;

        // 在JDK 1.8中，try-with-resources要求资源变量在try语句内部声明
        try (InputStream inputStream = pixivUtils.getInputStream(url)) {
            if (inputStream == null) {
                log.error("获取更新数据失败");
                return null;
            }

            String urlResult = pixivUtils.getUrlResult(inputStream);
            Object ids = getJsonValue(urlResult, "body", "page", "ids");

            // 截取所有的图片Id
            if (ids != null) {
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
                    log.info("数据异常，没有获取到最新更新的任何相关数据, ids={}", ids);
                } else {
                    log.info("成功获取到{}个最新图片ID", pictureList.size());
                }
                return pictureList;
            } else {
                log.error("获取ids失败, response={}", urlResult);
                return null;
            }
        } catch (NumberFormatException e) {
            log.error("图片ID格式解析失败", e);
            return null;
        } catch (Exception e) {
            log.error("未获取到正确的数据,解析失败, url={}", url, e);
            return null;
        }
    }

    /**
     * 过滤已下载的作品
     *
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
     *
     * @param pictureList 作品列表
     * @return 包含详细信息的作品列表
     */
    private List<Picture> getDetailedPictureInfo(List<Picture> pictureList) {
        return pictureList.parallelStream()
                .map(picture -> pixivUtils.getPictureInfo(picture.getPictureId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 筛选已关注画师的作品
     *
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
     * 处理图片组数据, 将图片组中的图片分别保存为单独的图片
     *
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
     *
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
        // 1. 查找用户
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserId, userId)
                .eq(User::getType, SourceConstant.PIXIV_SOURCE));

        if (user == null) {
            log.error("未找到用户ID为{}的Pixiv用户", userId);
            return false;
        }

        if (!StringUtils.hasText(newUserName)) {
            log.error("新用户名不能为空，用户ID：{}", userId);
            return false;
        }

        String oldUserName = user.getUserName();
        if (newUserName.equals(oldUserName)) {
            log.info("新用户名与旧用户名相同，无需修改，用户ID：{}", userId);
            return true;
        }

        // 2. 构建下载目录路径
        File oldDir = new File(baseDownloadDir, SourceConstant.PIXIV_SOURCE + File.separator + oldUserName + "-" + userId);
        if (!oldDir.exists()) {
            log.info("用户目录不存在，直接更新数据库，用户ID：{}", userId);
            return updateUserAndRelatedData(userId, user, newUserName);
        }

        // 3. 重命名目录
        File newDir = new File(baseDownloadDir, SourceConstant.PIXIV_SOURCE + File.separator + newUserName + "-" + userId);
        boolean renameTo = oldDir.renameTo(newDir);
        if (!renameTo) {
            log.error("目录重命名失败，旧目录：{}, 新目录：{}", oldDir.getPath(), newDir.getPath());
            return false;
        }
        log.info("目录重命名成功，旧目录：{}, 新目录：{}", oldDir.getPath(), newDir.getPath());

        // 4. 更新数据库中的用户信息和相关数据
        return updateUserAndRelatedData(userId, user, newUserName);
    }

    /**
     * 更新用户信息和相关数据
     *
     * @param userId      用户ID
     * @param user        用户对象
     * @param newUserName 新用户名
     * @return 是否更新成功
     */
    private boolean updateUserAndRelatedData(Long userId, User user, String newUserName) {
        try {
            // 更新用户信息
            CompletableFuture<Boolean> userUpdateFuture = CompletableFuture.supplyAsync(() -> {
                user.setUserName(newUserName);
                boolean result = userService.updateById(user);
                log.info("用户信息更新结果：{}，用户ID：{}, 新用户名：{}", result ? "成功" : "失败", userId, newUserName);
                return result;
            }, executor);

            // 更新图片信息
            CompletableFuture<Boolean> pictureUpdateFuture = CompletableFuture.supplyAsync(() -> {
                LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getUserId, userId)
                        .eq(Picture::getType, SourceConstant.PIXIV_SOURCE);
                List<Picture> pictureList = pictureService.list(queryWrapper);
                if (pictureList.isEmpty()) {
                    log.info("没有需要更新的图片信息，用户ID：{}", userId);
                    return true;
                }

                pictureList.forEach(picture -> picture.setUserName(newUserName));
                boolean result = pictureService.updateBatchById(pictureList);
                log.info("图片信息更新结果：{}，用户ID：{}, 更新数量：{}", result ? "成功" : "失败", userId, pictureList.size());
                return result;
            }, executor);

            // 更新失败图片信息
            CompletableFuture<Boolean> failPictureUpdateFuture = CompletableFuture.supplyAsync(() -> {
                LambdaQueryWrapper<FailPicture> queryWrapper = new LambdaQueryWrapper<FailPicture>()
                        .eq(FailPicture::getUserId, userId)
                        .eq(FailPicture::getType, SourceConstant.PIXIV_SOURCE);
                List<FailPicture> failPictureList = failPictureService.list(queryWrapper);
                if (failPictureList.isEmpty()) {
                    log.info("没有需要更新的失败图片信息，用户ID：{}", userId);
                    return true;
                }

                failPictureList.forEach(failPicture -> failPicture.setUserName(newUserName));
                boolean result = failPictureService.updateBatchById(failPictureList);
                log.info("失败图片信息更新结果：{}，用户ID：{}, 更新数量：{}", result ? "成功" : "失败", userId, failPictureList.size());
                return result;
            }, executor);

            // 等待所有异步任务完成
            CompletableFuture.allOf(userUpdateFuture, pictureUpdateFuture, failPictureUpdateFuture).join();

            // 检查所有任务的执行结果
            boolean userUpdateResult = userUpdateFuture.get();
            boolean pictureUpdateResult = pictureUpdateFuture.get();
            boolean failPictureUpdateResult = failPictureUpdateFuture.get();

            return userUpdateResult && pictureUpdateResult && failPictureUpdateResult;

        } catch (Exception e) {
            log.error("更新用户和相关数据失败，用户ID：{}, 新用户名：{}", userId, newUserName, e);
            return false;
        }
    }

    @Override
    public boolean saveUser(Long userId) {
        String userName = pixivUtils.getUserName(userId);
        boolean save = false;
        if (StringUtils.hasText(userName)) {
            User user = new User(userId, userName, SourceConstant.PIXIV_SOURCE);
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
        List<Picture> finalPictureList = new ArrayList<>();

        //数量过多无法获取数据，需要将数据拆分下载
        int batchSize = 80;
        int totalSize = pictureList.size();

        // 使用IntStream简化批次拆分逻辑
        IntStream.range(0, (int) Math.ceil((double) totalSize / batchSize))
                .forEach(i -> {
                    int startIndex = i * batchSize;
                    int endIndex = Math.min((i + 1) * batchSize, totalSize);
                    List<Picture> batchList = pictureList.subList(startIndex, endIndex);

                    log.debug("处理批次 {}，图片数量：{}", i + 1, batchList.size());
                    boolean hasError = getPictureList(userId, batchList, finalPictureList);
                    if (hasError) {
                        log.error("处理批次 {} 失败", i + 1);
                        throw new RuntimeException("处理图片列表失败");
                    }
                });

        return finalPictureList;
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
        String responseJson;
        // 使用try-with-resources确保inputStream正确关闭 - JDK 1.8兼容：资源在try块内部声明
        try (InputStream inputStream = pixivUtils.getInputStream(builder.toString())) {
            if (inputStream == null) return true;
            // 解析页面数据并转成字符串用于后续获取实际有效数据
            responseJson = pixivUtils.getUrlResult(inputStream);
        } catch (IOException e) {
            log.error("获取URL结果失败", e);
            return true;
        }

        if (!StringUtils.hasText(responseJson)) return true;

        JSONObject worksJson;
        try {
            Object works = getJsonValue(responseJson, "body", "works");
            if (works == null) {
                log.error("获取works数据失败, responseJson={}", responseJson);
                return true;
            }
            worksJson = JSONUtil.parseObj(works);
        } catch (ClassCastException e) {
            log.error("works数据类型转换失败, 期望JSONObject, responseJson={}", responseJson, e);
            return true;
        } catch (Exception e) {
            log.error("解析works数据失败, responseJson={}", responseJson, e);
            return true;
        }
        for (Picture picture : pictureList) {
            Object value = worksJson.get(picture.getPictureId().toString());
            if (value == null) {
                log.error("未找到图片ID {} 的数据", picture.getPictureId());
                continue;
            }

            JSONObject pictureJson;
            try {
                pictureJson = JSONUtil.parseObj(value);
            } catch (Exception e) {
                log.error("解析图片ID {} 的数据失败, value={}", picture.getPictureId(), value, e);
                continue;
            }

            String title = (String) pictureJson.get("title");
            picture.setTitle(title);
            Integer pageCount = (Integer) pictureJson.get("pageCount");
            if (pageCount == null) {
                pageCount = 1;
                log.warn("图片ID {} 的pageCount为空，默认设置为1", picture.getPictureId());
            }

            String url = (String) pictureJson.get("url");
            if (StringUtils.hasText(url)) {
                String src = pixivUtils.getRealSrc(url);
                picture.setSrc(src);
            } else {
                log.error("图片ID {} 的url为空", picture.getPictureId());
                continue;
            }

            // 获取该picture组中的所有相关连接
            for (int i = 0; i < pageCount; i++) {
                Picture resultPicture = pixivUtils.getResultPicture(i, picture);
                if (resultPicture != null) {
                    resultPictureList.add(resultPicture);
                } else {
                    log.error("获取图片ID {} 第{}页的数据失败", picture.getPictureId(), i);
                }
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
        if (pictureList.isEmpty()) {
            return;
        }

        log.info("开始下载，共{}张图片", pictureList.size());
        long startTime = System.currentTimeMillis();
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalCount = pictureList.size();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<Picture> updatedPictures = Collections.synchronizedList(new ArrayList<>());
        List<FailPicture> failPictures = Collections.synchronizedList(new ArrayList<>());

        for (Picture picture : pictureList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try (InputStream inputStream = pixivUtils.getInputStream(picture)) {
                    if (inputStream != null) {
                        String downloadDir = baseDownloadDir + "\\" + SourceConstant.PIXIV_SOURCE + "\\" + picture.getUserName() + "-" + picture.getUserId() + "\\";
                        boolean isDownloaded = PictureUtils.downloadPicture(downloadDir, picture, inputStream, SourceConstant.PIXIV_SOURCE, useUserMode);

                        if (isDownloaded) {
                            log.info("{}的第{}张图片下载完成，剩余{}张图片等待下载",
                                    picture.getUserName(),
                                    completedCount.incrementAndGet(),
                                    (totalCount - completedCount.get()));
                            log.debug("下载完成的图片信息: {}", picture);
                            picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                        } else {
                            picture.setStatus(PictureStatusConstant.FAIL_STATUS);
                            failPictures.add(new FailPicture(picture));
                        }
                    } else {
                        picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
                        failPictures.add(new FailPicture(picture));
                    }
                } catch (IOException e) {
                    log.error("处理图片下载时发生IO异常, pictureId={}, errorMessage={}", picture.getPictureId(), e.getMessage(), e);
                    picture.setStatus(PictureStatusConstant.FAIL_STATUS);
                    failPictures.add(new FailPicture(picture));
                } finally {
                    updatedPictures.add(picture);
                }
            }, executor);
            futures.add(future);
        }

        // 等待所有下载任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 批量更新数据库，减少数据库操作次数
        if (!updatedPictures.isEmpty()) {
            pictureService.updateBatchById(updatedPictures);
        }
        if (!failPictures.isEmpty()) {
            failPictureService.saveOrUpdateBatch(failPictures);
        }

        long endTime = System.currentTimeMillis();
        log.info("总需要下载{}份图片", totalCount);
        log.info("下载完成，耗时：{}秒", (endTime - startTime) / 1000);
    }

    /**
     * 正式下载图片 会对下载时间进行统计 更新数据库中的图片下载状态，并将下载失败的图片另存数据库，默认不会隔离用户下载
     *
     * @param pictureList 图片列表 userId/userName/PictureId/pageCount/type/status
     */
    private void downloadPicture(List<Picture> pictureList) {
        downloadPicture(pictureList, false);
    }

}

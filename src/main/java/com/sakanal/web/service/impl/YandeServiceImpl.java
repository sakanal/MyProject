package com.sakanal.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sakanal.web.constant.PictureStatusConstant;
import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.FailPicture;
import com.sakanal.web.entity.Picture;
import com.sakanal.web.entity.User;
import com.sakanal.web.service.FailPictureService;
import com.sakanal.web.service.PictureService;
import com.sakanal.web.service.UserService;
import com.sakanal.web.service.YandeService;
import com.sakanal.web.util.MySslUtils;
import com.sakanal.web.util.PictureUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sakanal.web.constant.SourceConstant.YANDE_SOURCE;
import static com.sakanal.web.constant.SourceConstant.YANDE_URL;

/**
 * Yande服务实现类
 * 负责处理Yande.re网站的图片下载业务
 * 
 * 主要功能包括：
 * 1. 根据标签（tags）搜索并下载图片
 * 2. 批量更新已关注标签的图片
 * 3. 重试下载失败的图片
 * 4. 并发下载所有页面的图片
 * 
 * @author sakanal
 */
@Slf4j
@Service
public class YandeServiceImpl implements YandeService {
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
     * 下载指定标签的所有图片
     * 功能流程：
     * 1. 初始化SSL设置
     * 2. 获取总页数
     * 3. 创建下载目录
     * 4. 并发下载所有页面的图片
     *
     * @param tags 搜索标签，最终会作为用户名使用
     */
    @Override
    public void download(String tags) {
        String baseUrl = YANDE_URL + "post?tags=" + tags;
        // 初始化 SSL 设置
        if (!initSsl()) {
            return;
        }

        Document pageDocument = getDocument(baseUrl, "获取总页数");
        if (pageDocument == null) {
            return;
        }
        String tempDownloadDir = baseDownloadDir + "\\" + YANDE_SOURCE + "\\" + tags + "\\";
        int pages = getPages(pageDocument, tags, tempDownloadDir);
        if (pages == 0) {
            return;
        }

        long start = System.currentTimeMillis();
        downloadAllPages(baseUrl, tags, tempDownloadDir, pages);
        long end = System.currentTimeMillis();
        log.info("耗时：{}秒", (end - start) / 1000);
    }

    /**
     * 并发下载所有页面的图片
     * 使用CompletableFuture实现异步并发下载
     *
     * @param baseUrl          基础URL
     * @param tags             搜索标签
     * @param tempDownloadDir  临时下载目录路径
     * @param pages            总页数
     */
    private void downloadAllPages(String baseUrl, String tags, String tempDownloadDir, int pages) {
        List<CompletableFuture<Void>> pageFutures = new ArrayList<>();
        List<Picture> allUpdatedPictures = Collections.synchronizedList(new ArrayList<>());
        List<FailPicture> allFailPictures = Collections.synchronizedList(new ArrayList<>());

        for (int page = 1; page <= pages; page++) {
            CompletableFuture<Void> pageFuture = processSinglePage(
                    baseUrl, tags, tempDownloadDir, page,
                    allUpdatedPictures, allFailPictures);
            pageFutures.add(pageFuture);
        }

        CompletableFuture.allOf(pageFutures.toArray(new CompletableFuture[0])).join();
        updateDatabase(allUpdatedPictures, allFailPictures);
    }

    /**
     * 处理单个页面的图片下载
     * 异步获取页面图片列表，然后下载该页面的所有图片
     *
     * @param baseUrl             基础URL
     * @param tags                搜索标签
     * @param tempDownloadDir     临时下载目录路径
     * @param currentPage         当前页码
     * @param allUpdatedPictures  所有更新的图片列表（线程安全）
     * @param allFailPictures     所有失败的图片列表（线程安全）
     * @return CompletableFuture对象
     */
    private CompletableFuture<Void> processSinglePage(
            String baseUrl, String tags, String tempDownloadDir, int currentPage,
            List<Picture> allUpdatedPictures, List<FailPicture> allFailPictures) {

        return CompletableFuture.<List<Picture>>supplyAsync(() -> {
                    String pageUrl = baseUrl + "&page=" + currentPage;
                    Document document = getDocumentWithRetry(pageUrl, "获取页面数据");
                    if (document == null) {
                        return Collections.emptyList();
                    }
                    List<Picture> pictures = initPictureList(document, tags);
                    return pictures != null ? pictures : Collections.emptyList();
                }, executor)
                .thenAcceptAsync(pictures -> {
                    if (pictures.isEmpty()) {
                        return;
                    }
                    downloadPicturesForPage(pictures, currentPage,
                            tempDownloadDir, allUpdatedPictures, allFailPictures);
                }, executor);
    }

    /**
     * 下载单个页面内的所有图片
     * 使用线程池并发下载该页面的所有图片
     *
     * @param pictures           图片列表
     * @param currentPage        当前页码
     * @param tempDownloadDir    临时下载目录路径
     * @param allUpdatedPictures 所有更新的图片列表（线程安全）
     * @param allFailPictures    所有失败的图片列表（线程安全）
     */
    private void downloadPicturesForPage(
            List<Picture> pictures, int currentPage, String tempDownloadDir,
            List<Picture> allUpdatedPictures, List<FailPicture> allFailPictures) {

        List<CompletableFuture<Void>> pictureFutures = new ArrayList<>();
        List<Picture> picturesToUpdate = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < pictures.size(); i++) {
            final int currentIndex = i;
            final Picture picture = pictures.get(i);

            CompletableFuture<Void> pictureFuture = CompletableFuture.runAsync(() -> {
                log.info("第{}页，第{}张图片开始下载", currentPage, currentIndex + 1);

                boolean needUpdateUrl = isPictureInfoUrl(picture);
                boolean downloadResult = download(picture, tempDownloadDir);

                if (downloadResult) {
                    log.info("第{}页，第{}张图片完成", currentPage, currentIndex + 1);
                    picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                } else {
                    log.info("存入失败队列，等待后续下载");
                    picture.setStatus(PictureStatusConstant.FAIL_STATUS);
                    allFailPictures.add(new FailPicture(picture));
                }

                if (needUpdateUrl) {
                    picturesToUpdate.add(picture);
                }
                allUpdatedPictures.add(picture);
            }, executor);
            pictureFutures.add(pictureFuture);
        }

        CompletableFuture.allOf(pictureFutures.toArray(new CompletableFuture[0])).join();

        if (!picturesToUpdate.isEmpty()) {
            pictureService.updateBatchById(picturesToUpdate);
        }
    }

    /**
     * 批量更新数据库
     * 更新所有下载完成的图片状态，并保存失败的图片记录
     *
     * @param allUpdatedPictures 所有更新的图片列表
     * @param allFailPictures    所有失败的图片列表
     */
    private void updateDatabase(List<Picture> allUpdatedPictures, List<FailPicture> allFailPictures) {
        if (!allUpdatedPictures.isEmpty()) {
            pictureService.updateBatchById(allUpdatedPictures);
        }
        if (!allFailPictures.isEmpty()) {
            boolean saveBatch = failPictureService.saveOrUpdateBatch(allFailPictures);
            log.info("失败队列保存{}", saveBatch ? "成功" : "失败");
        }
    }

    @Override
    public void againDownload() {
        // 获取数据中状态为 default 和 fail 的数据
        List<Picture> pictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getType, YANDE_SOURCE)
                .and(query -> query.eq(Picture::getStatus, PictureStatusConstant.DEFAULT_STATUS)
                        .or().eq(Picture::getStatus, PictureStatusConstant.FAIL_STATUS)));
        if (pictureList != null && !pictureList.isEmpty()) {
            if (!initSsl()) {
                return;
            }
            Set<Long> failPictureIdSet = new HashSet<>();
            log.info("当前有{}图片需要进行补充下载", pictureList.size());
            AtomicInteger count = new AtomicInteger(1);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<Picture> updatedPictures = Collections.synchronizedList(new ArrayList<>());

            for (Picture picture : pictureList) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    String tempDownloadDir = this.baseDownloadDir + "\\" + YANDE_SOURCE + "\\" + picture.getUserName() + "\\";
                    // 如果src是页面数据会去获取实际的图片数据，如果src是图片数据则直接进行下载
                    if (download(picture, tempDownloadDir)) {
                        log.info("第{}张图片完成下载", count.getAndIncrement());
                        picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                        failPictureIdSet.add(picture.getId());
                    } else {
                        log.error("第{}张图片下载失败", count.getAndIncrement());
                    }
                    updatedPictures.add(picture);
                }, executor);
                futures.add(future);
            }

            // 等待所有下载任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            if (!updatedPictures.isEmpty()) {
                pictureService.updateBatchById(updatedPictures);
            }
            failPictureService.removeBatchByIds(failPictureIdSet);
        } else {
            log.info("暂无图片需要补充下载");
        }
    }

    @Override
    public void update() {
        List<User> userList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, YANDE_SOURCE));
        if (userList != null && !userList.isEmpty()) {
            userList.forEach(user -> download(user.getUserName()));
        }
    }

    /**
     * 获取总页数
     * 同时处理以下逻辑：
     * 1. 创建下载目录（如果不存在）
     * 2. 如果目录已存在，从数据库中获取下载失败的图片进行补充下载
     *
     * @param pageDocument 页面数据
     * @param tags         搜索标签，作为作者名保存
     * @param downloadDir  下载目录路径
     * @return 总页数，如果创建文件夹失败返回0
     */
    private int getPages(Document pageDocument, String tags, String downloadDir) {
        int total = 0;
        Elements pagination = pageDocument.getElementsByClass("pagination");
        if (!pagination.isEmpty()) {
            Elements a = pagination.get(0).getElementsByTag("a");
            total = Integer.parseInt(a.get(a.size() - 1 - 1).text());
            log.info("总页数为:{}", total);
        }
        File file = new File(downloadDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.info("创建文件夹失败，请检查路径是否正确");
                total = 0;
            } else {
                createUserIfNotExists(tags);
            }
        } else {
            //文件夹存在，过去曾经下载过该tags
            //从数据库中获取到下载失败/默认状态的图片进行再次下载
            LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<Picture>()
                    .eq(Picture::getType, YANDE_SOURCE)
                    .eq(Picture::getUserName, tags)
                    .and(query -> query.eq(Picture::getStatus, PictureStatusConstant.DEFAULT_STATUS)
                            .or().eq(Picture::getStatus, PictureStatusConstant.FAIL_STATUS));
            List<Picture> pictureList = pictureService.list(lambdaQueryWrapper);
            if (!pictureList.isEmpty()) {
                log.info("重新下载失败的图片");
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                List<FailPicture> failPictures = Collections.synchronizedList(new ArrayList<>());
                List<Picture> updatedPictures = Collections.synchronizedList(new ArrayList<>());

                for (Picture picture : pictureList) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        log.info("补充下载：{}开始下载", picture.getPictureId());
                        if (download(picture, downloadDir)) {
                            log.info("补充下载：{}下载成功", picture.getPictureId());
                            picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                            updatedPictures.add(picture);
                        } else {
                            log.info("补充下载：{}下载失败，等待后续下载", picture.getPictureId());
                            failPictures.add(new FailPicture(picture));
                        }
                    }, executor);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                if (!updatedPictures.isEmpty()) {
                    pictureService.updateBatchById(updatedPictures);
                }
                if (!failPictures.isEmpty()) {
                    failPictureService.saveOrUpdateBatch(failPictures);
                }
            }
        }

        return total;
    }

    /**
     * 获取当前页的所有图片数据
     * 解析页面DOM，提取图片信息并保存到数据库
     * 
     * @param document 页面DOM数据
     * @param tags     搜索标签/作者名
     * @return 数据库没有下载记录的图片数据列表，如果没有新图片返回null
     */
    private List<Picture> initPictureList(Document document, String tags) {
        Elements li = Objects.requireNonNull(document.getElementById("post-list-posts")).getElementsByTag("li");
        log.info("当前页面有{}张图片", li.size());
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, tags)
                .eq(User::getType, YANDE_SOURCE)
                .last("limit 1"));
        if (user == null) {
            log.error("未找到用户: {}", tags);
            return null;
        }
        Long id = user.getId();
        List<Picture> pictureList = new ArrayList<>();
        for (Element element : li) {
            //获取图片id
            String pictureId = element.attr("id").replace("p", "");
            //获取图片路径 /post/show/1046310
            String href = element.getElementsByTag("a").get(0).attr("href");

            Picture picture = new Picture();
            picture.setUserId(id);
            picture.setSrc(SourceConstant.YANDE_URL + href);
            picture.setPictureId(Long.valueOf(pictureId));
            picture.setUserName(tags);
            picture.setPageCount(1);
            picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
            picture.setType(YANDE_SOURCE);
            pictureList.add(picture);
        }

        if (pictureList.isEmpty()) {
            log.info("该页没有图片数据");
            return null;
        }

        List<Picture> dataSourcePictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getUserName, tags)
                .eq(Picture::getType, YANDE_SOURCE)
                .in(Picture::getPictureId, pictureList.stream().map(Picture::getPictureId).collect(Collectors.toList())));

        if (!dataSourcePictureList.isEmpty()) {
            Set<Long> existingIds = dataSourcePictureList.stream().map(Picture::getPictureId).collect(Collectors.toSet());
            pictureList.removeIf(pic -> existingIds.contains(pic.getPictureId()));
        }

        if (!pictureList.isEmpty()) {
            boolean saveBatch = pictureService.saveBatch(pictureList);
            log.info("图片数据持久化:{}", (saveBatch ? "成功" : "失败"));
        } else {
            log.info("该页数据过去已经下载完成");
            return null;
        }

        return pictureList;
    }

    /**
     * 下载图片
     * 如果图片src存储的是图片详细页面URL，则先获取实际的图片网络地址并更新数据库
     *
     * @param picture     图片数据
     * @param downloadDir 下载目录路径
     * @return 下载是否成功
     */
    private boolean download(Picture picture, String downloadDir) {
        boolean check = isPictureInfoUrl(picture);
        if (check) {
            boolean pictureInfo = getPictureInfo(picture);
            if (!pictureInfo) {
                return false;
            }
        }
        return PictureUtils.downloadPicture(downloadDir, picture, null, YANDE_SOURCE);

    }

    /**
     * 检测src是图片的详细页面URL还是直接的图片网络地址
     *
     * @param picture 图片数据，使用src属性进行判断
     * @return true-是图片详细页面的URL，需要进一步解析
     *         false-是直接的图片网络地址，可以直接下载
     */
    private boolean isPictureInfoUrl(Picture picture) {
        //https://files.yande.re/image/f9fcee6e7b8dd0cbf2d291d8b485d0a2/yande.re%201021573%20bondage%20breasts%20censored%20cum%20dress%20extreme_content%20garter%20iijima_masashi%20nipples%20no_bra%20nopan%20pussy%20pussy_juice%20skirt_lift%20tentacles%20wet%20wings.png
        //https://yande.re//post/show/1021572
        String src = picture.getSrc();
        String regex = ".*files.yande.re.*";
        return !Pattern.matches(regex, src);
    }

    /**
     * 从图片详细页面中获取图片的网络地址
     * 优先获取PNG格式的图片，如果没有则获取高分辨率版本（highres）
     *
     * @param picture 图片数据，src需要为图片页面URL，例如：https://yande.re//post/show/1026154
     * @return 是否成功获取到图片地址
     */
    private boolean getPictureInfo(Picture picture) {
        Document documentImage = getDocument(picture.getSrc(), "获取图片详细信息");
        if (documentImage == null) {
            return false;
        }

        Element elementById = documentImage.getElementById("png");
        if (elementById != null) {
            String href = elementById.attr("href");
            picture.setSrc(href);
        } else {
            elementById = documentImage.getElementById("highres");
            if (elementById != null) {
                String href = elementById.attr("href");
                picture.setSrc(href);
            } else {
                log.error("检查网站是否更新，未找到图片下载链接");
                return false;
            }
        }

        // 不在这里单独更新数据库，改为在批量处理中更新
        return true;
    }

    /**
     * 初始化SSL设置
     * 忽略SSL证书验证，用于访问HTTPS网站
     *
     * @return 初始化是否成功
     */
    private boolean initSsl() {
        try {
            MySslUtils.ignoreSsl();
            return true;
        } catch (Exception e) {
            log.error("忽略SSL证书失败, message={}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取页面文档
     * 使用Jsoup解析URL，设置10秒超时
     *
     * @param url           页面URL
     * @param operationDesc 操作描述，用于日志输出
     * @return 页面Document对象，获取失败返回null
     */
    private Document getDocument(String url, String operationDesc) {
        try {
            return Jsoup.parse(new URL(url), 10 * 1000);
        } catch (SSLHandshakeException ssl) {
            log.error("{}时发生SSLHandshakeException, message={}", operationDesc, ssl.getMessage());
        } catch (SocketTimeoutException socketTimeoutException) {
            log.error("{}时发生SocketTimeoutException, message={}", operationDesc, socketTimeoutException.getMessage());
        } catch (IOException e) {
            log.error("{}时发生IOException, message={}", operationDesc, e.getMessage());
        }
        return null;
    }

    /**
     * 带重试机制的页面文档获取
     * 如果第一次获取失败，会再尝试一次
     *
     * @param url           页面URL
     * @param operationDesc 操作描述
     * @return 页面Document对象
     */
    private Document getDocumentWithRetry(String url, String operationDesc) {
        Document document = getDocument(url, operationDesc);
        if (document == null) {
            log.info("第二次尝试{}", operationDesc);
            document = getDocument(url, operationDesc + "(重试)");
        }
        return document;
    }

    /**
     * 如果用户不存在则创建
     * 用于在首次下载某个标签的图片时创建对应的用户记录
     *
     * @param userName 用户名（标签名）
     */
    private void createUserIfNotExists(String userName) {
        User existingUser = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUserName, userName)
                .eq(User::getType, YANDE_SOURCE)
                .last("limit 1"));
        if (existingUser == null) {
            User user = new User();
            user.setUserName(userName);
            user.setType(YANDE_SOURCE);
            userService.save(user);
            log.info("创建用户{}成功", userName);
        }
    }
}
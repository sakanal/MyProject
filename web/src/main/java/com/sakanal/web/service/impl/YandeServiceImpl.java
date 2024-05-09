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
import com.sakanal.web.util.MySSlUtils;
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
import java.util.regex.Pattern;

import static com.sakanal.web.constant.SourceConstant.YANDE_SOURCE;
import static com.sakanal.web.constant.SourceConstant.YANDE_URL;

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

    /**
     * 下载图片
     *
     * @param tags 搜索项，最终会作为用户名使用
     */
    @Override
    public void download(String tags) {
        StringBuilder builder = new StringBuilder(YANDE_URL);
        String tryGetTotalPageURL = builder.append("post").append("?tags=").append(tags).toString();
        String baseURL = builder.toString();
        try {
            Document pageDocument;
            try {
                MySSlUtils.ignoreSsl();
                pageDocument = Jsoup.parse(new URL(tryGetTotalPageURL), 10 * 1000);
            } catch (SSLHandshakeException ssl) {
                log.error("SSLHandshakeException异常,message={}",ssl.getMessage());
                return;
            }catch (SocketTimeoutException socketTimeoutException){
                log.error("SocketTimeoutException异常,message={}",socketTimeoutException.getMessage());
                return;
            } catch (Exception e) {
                log.error("忽略SSL证书失败,message={}",e.getMessage());
                return;
            }
            String tempDownloadDir = baseDownloadDir + "\\" + YANDE_SOURCE + "\\" + tags + "\\";
            int pages = getPages(pageDocument, tags, tempDownloadDir);
            if (pages != 0) {
                long start = System.currentTimeMillis();

                List<FailPicture> failPictureList = new ArrayList<>();
                for (int page = 1; page <= pages; page++) {
                    String pageURL = baseURL + "&page=" + page;
                    Document document = null;
                    try {
                        document = Jsoup.parse(new URL(pageURL), 10 * 1000);
                    } catch (SocketTimeoutException socketTimeoutException) {
                        log.error("SocketTimeoutException异常,message={}",socketTimeoutException.getMessage());
                        log.info("第二次尝试获取页面数据");
                        try {
                            document = Jsoup.parse(new URL(pageURL), 10 * 1000);
                        } catch (IOException e) {
                            log.error("SocketTimeoutException异常,message={}",socketTimeoutException.getMessage());
                            log.info("第二次尝试获取页面数据-失败");
                            continue;
                        }
                    }
                    List<Picture> pictures = initPictureList(document, tags);
                    if (pictures == null) {
                        continue;
                    }
                    for (int i = 0; i < pictures.size(); i++) {
                        log.info("第" + page + "页，第" + (i + 1) + "张图片开始下载");
                        boolean download = download(pictures.get(i), tempDownloadDir);
                        if (download) {
                            log.info("第" + page + "页，第" + (i + 1) + "张图片完成");
                            pictures.get(i).setStatus(PictureStatusConstant.SUCCESS_STATUS);
                            //更新数据库中的图片状态
                            pictureService.updateById(pictures.get(i));
                        } else {
                            log.info("存入失败队列，等待后续下载");
                            pictures.get(i).setStatus(PictureStatusConstant.FAIL_STATUS);
                            //更新数据库中的图片状态
                            pictureService.updateById(pictures.get(i));
                            FailPicture failPicture = new FailPicture(pictures.get(i));
                            failPictureList.add(failPicture);
                        }
                    }
                }
                if (!failPictureList.isEmpty()) {
                    boolean saveBatch = failPictureService.saveOrUpdateBatch(failPictureList);
                    log.info("失败队列保存{}",saveBatch?"成功":"失败");
                }

                long end = System.currentTimeMillis();
                log.info("耗时：" + (end - start) / 1000 + "秒");
            }
        } catch (IOException e) {
            log.info("建立连接失败，尝试获取总页数失败，请检查是否开启代理");
            e.printStackTrace();
        }

    }

    @Override
    public void againDownload() {
        // 获取数据中状态为default和fail的数据
        List<Picture> pictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getType, YANDE_SOURCE)
                .and(query -> query.eq(Picture::getStatus, PictureStatusConstant.DEFAULT_STATUS)
                        .or().eq(Picture::getStatus, PictureStatusConstant.FAIL_STATUS)));
        if (pictureList != null && !pictureList.isEmpty()) {
            try {
                MySSlUtils.ignoreSsl();
            } catch (Exception e) {
                log.error("忽略SSL证书失败,message={}",e.getMessage());
                return;
            }
            Set<Long> failPictureIdSet = new HashSet<>();
            log.info("当前有" + pictureList.size() + "图片需要进行补充下载");
            String tempDownloadDir;
            int i = 1;
            for (Picture picture : pictureList) {
                tempDownloadDir = this.baseDownloadDir + "\\" + YANDE_SOURCE + "\\" + picture.getUserName() + "\\";
                // 如果src是页面数据会去获取实际的图片数据，如果src是图片数据则直接进行下载
                if (download(picture, tempDownloadDir)) {
                    log.info("第" + i + "张图片完成下载");
                    picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                    failPictureIdSet.add(picture.getId());
                } else {
                    log.error("第" + i + "张图片下载失败");
                }
                i++;
            }
            pictureService.updateBatchById(pictureList);
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
     *
     * @param pageDocument 页面数据
     * @param tags         搜索项，作为作者名保存
     */
    private int getPages(Document pageDocument, String tags, String downloadDir) {
        int total = 0;
        Elements pagination = pageDocument.getElementsByClass("pagination");
        if (!pagination.isEmpty()) {
            Elements a = pagination.get(0).getElementsByTag("a");
            total = Integer.parseInt(a.get(a.size() - 1 - 1).text());
            log.info("总页数为:" + total);
        }
        File file = new File(downloadDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.info("创建文件夹失败，请检查路径是否正确");
                total = 0;
            } else {
                List<User> list = userService.list(new LambdaQueryWrapper<User>().eq(User::getUserName, tags).eq(User::getType, YANDE_SOURCE).last("limit 1"));
                if (list.isEmpty()) {
                    User user = new User();
                    user.setUserName(tags);
                    user.setType(YANDE_SOURCE);
                    userService.save(user);
                }
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
                List<FailPicture> failPictures = new ArrayList<>();
                pictureList.forEach(picture -> {
                    log.info("补充下载：" + picture.getPictureId() + "开始下载");
                    boolean download = download(picture, downloadDir);
                    if (!download) {
                        log.info("补充下载：" + picture.getPictureId() + "下载失败，等待后续下载");
                        FailPicture failPicture = new FailPicture(picture);
                        failPictures.add(failPicture);
                    } else {
                        log.info("补充下载：" + picture.getPictureId() + "下载成功");
                        picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                        pictureService.updateById(picture);
                    }
                });
                failPictureService.saveOrUpdateBatch(failPictures);
            }
        }

        return total;
    }

    /**
     * 获取当前页的所有图片数据--id(无效数据，用户表id)/pictureId/src/userName(tags)/type
     * 保存未下载的图片数据到数据库中
     *
     * @param document 页面数据
     * @param tags     搜索项/作者名
     * @return 数据库没有下载记录的图片数据
     */
    private List<Picture> initPictureList(Document document, String tags) {
        Elements li = Objects.requireNonNull(document.getElementById("post-list-posts")).getElementsByTag("li");
        log.info("当前页面有" + li.size() + "张图片");
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, tags).eq(User::getType, YANDE_SOURCE));
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

        List<Picture> dataSourcePictureList = pictureService.list(new LambdaQueryWrapper<Picture>().eq(Picture::getUserName, tags).eq(Picture::getType, YANDE_SOURCE));
        pictureList.removeAll(dataSourcePictureList);
        if (!pictureList.isEmpty()) {
            boolean saveBatch = pictureService.saveBatch(pictureList);
            log.info("图片数据持久化:" + (saveBatch ? "成功" : "失败"));
        } else {
            log.info("该页数据过去已经下载完成");
            return null;
        }

        return pictureList;
    }

    /**
     * 下载图片
     * 如果图片src存储的是图片详细页面，则获取图片网络地址并更新数据库的图片源src
     *
     * @param picture 图片数据
     */
    private boolean download(Picture picture, String downloadDir) {
        boolean check = isPictureInfoURL(picture);
        if (check) {
            boolean pictureInfo = getPictureInfo(picture);
            if (!pictureInfo)
                return false;
        }
        return PictureUtils.downloadPicture(downloadDir, picture, null, YANDE_SOURCE);

    }

    /**
     * 检测src是图片的详细页面的路径还是图片网络地址
     *
     * @param picture 图片数据 使用src
     * @return true-是图片详细页面的路径 false-是图片网络地址
     */
    private boolean isPictureInfoURL(Picture picture) {
        //https://files.yande.re/image/f9fcee6e7b8dd0cbf2d291d8b485d0a2/yande.re%201021573%20bondage%20breasts%20censored%20cum%20dress%20extreme_content%20garter%20iijima_masashi%20nipples%20no_bra%20nopan%20pussy%20pussy_juice%20skirt_lift%20tentacles%20wet%20wings.png
        //https://yande.re//post/show/1021572
        String src = picture.getSrc();
        String regex = ".*files.yande.re.*";
        return !Pattern.matches(regex, src);
    }

    /**
     * 从图片详细页面中获取图片的网络地址
     *
     * @param picture 图片数据，要有src，src需要为图片页面目录 eg:https://yande.re//post/show/1026154
     */
    private boolean getPictureInfo(Picture picture) {
        try {
            Document documentImage = Jsoup.parse(new URL(picture.getSrc()), 10 * 1000);
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
                    log.error("检查网站是否更新");
                    return false;
                }
            }
            pictureService.updateById(picture);
            return true;
        }catch (SocketTimeoutException socketTimeoutException){
            log.error("SocketTimeoutException,message={}",socketTimeoutException.getMessage());
            return false;
        }catch (IOException e) {
            log.info("获取图片详细页面失败，请检查代理是否有效");
            e.printStackTrace();
            return false;
        }
    }
}

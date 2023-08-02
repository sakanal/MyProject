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
import com.sakanal.web.util.PictureUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
     * @param tags 搜索项，最终会作为用户名使用
     */
    @Override
    public void download(String tags) {
        StringBuilder builder = new StringBuilder(YANDE_URL);
        String tryGetTotalPageURL = builder.append("post").append("?tags=").append(tags).toString();
        String baseURL = builder.toString();
        try {
            Document pageDocument = Jsoup.parse(new URL(tryGetTotalPageURL), 10 * 1000);
            baseDownloadDir = baseDownloadDir + "\\" + YANDE_SOURCE + "\\" + tags + "\\";
            int pages = getPages(pageDocument, tags);
            if (pages != 0) {
                long start = System.currentTimeMillis();

                List<FailPicture> failPictureList = new ArrayList<>();
                for (int page = 1; page <= pages; page++) {
                    String pageURL = baseURL + "&page=" + page;
                    Document document = Jsoup.parse(new URL(pageURL), 10 * 1000);
                    List<Picture> pictures = initPictureList(document, tags);
                    if (pictures == null) {
                        break;
                    }
                    for (int i = 0; i < pictures.size(); i++) {
                        log.info("第" + page + "页，第" + (i + 1) + "张图片开始下载");
                        boolean download = download(pictures.get(i));
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
                if (failPictureList.size() > 0) {
                    failPictureService.saveBatch(failPictureList);
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
        List<Picture> pictureList = pictureService.list(new LambdaQueryWrapper<Picture>()
                .eq(Picture::getType, YANDE_SOURCE)
                .and(query -> query.eq(Picture::getStatus, PictureStatusConstant.DEFAULT_STATUS)
                        .or().eq(Picture::getStatus, PictureStatusConstant.FAIL_STATUS)));
        if (pictureList != null && pictureList.size() > 0) {
            String tempDir = this.baseDownloadDir;
            pictureList.forEach(picture -> {
                baseDownloadDir = tempDir + "\\" + YANDE_SOURCE + "\\" + picture.getUserName() + "\\";
                if (download(picture)) {
                    picture.setStatus(PictureStatusConstant.SUCCESS_STATUS);
                }
            });
            pictureService.updateBatchById(pictureList);
            failPictureService.saveOrUpdateBatch(pictureList);
            failPictureService.remove(new LambdaQueryWrapper<FailPicture>()
                    .eq(FailPicture::getType, YANDE_SOURCE)
                    .and(query -> query.eq(FailPicture::getStatus, PictureStatusConstant.SUCCESS_STATUS)
                            .or().eq(FailPicture::getStatus, PictureStatusConstant.COVER_STATUS)));
        }
    }

    @Override
    public void update() {
        List<User> userList = userService.list(new LambdaQueryWrapper<User>().eq(User::getType, YANDE_SOURCE));
        if (userList != null && userList.size() > 0) {
            userList.forEach(user -> download(user.getUserName()));
        }
    }

    /**
     * 获取总页数
     *
     * @param pageDocument 页面数据
     * @param tags         搜索项，作为作者名保存
     */
    private int getPages(Document pageDocument, String tags) {
        int total = 0;
        Elements pagination = pageDocument.getElementsByClass("pagination");
        if (pagination.size() != 0) {
            Elements a = pagination.get(0).getElementsByTag("a");
            total = Integer.parseInt(a.get(a.size() - 1 - 1).text());
            log.info("总页数为:" + total);
        }
        File file = new File(baseDownloadDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.info("创建文件夹失败，请检查路径是否正确");
                total = 0;
            } else {
                User user = new User();
                user.setUserName(tags);
                user.setType(YANDE_SOURCE);
                userService.save(user);
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
            if (pictureList.size() > 0) {
                log.info("重新下载失败的图片");
                List<FailPicture> failPictures = new ArrayList<>();
                pictureList.forEach(picture -> {
                    log.info("补充下载：" + picture.getPictureId() + "开始下载");
                    boolean download = download(picture);
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
                failPictureService.saveBatch(failPictures);
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

        LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<Picture>().eq(Picture::getUserName, tags);
        List<Picture> dataSourcePictureList = pictureService.list(lambdaQueryWrapper);
        pictureList.removeAll(dataSourcePictureList);
        if (pictureList.size() > 0) {
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
    private boolean download(Picture picture) {
        boolean check = isPictureInfoURL(picture);
        if (check) {
            boolean pictureInfo = getPictureInfo(picture);
            if (!pictureInfo)
                return false;
        }
        return PictureUtils.downloadPicture(baseDownloadDir, picture, null, YANDE_SOURCE);

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
        } catch (IOException e) {
            log.info("获取图片详细页面失败，请检查代理是否有效");
            e.printStackTrace();
            return false;
        }
    }
}

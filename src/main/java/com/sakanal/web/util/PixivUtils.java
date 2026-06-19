package com.sakanal.web.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sakanal.web.config.MyPixivConfig;
import com.sakanal.web.constant.PictureStatusConstant;
import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.Picture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pixiv工具类，提供与Pixiv网站交互的各种方法
 * 包括获取画师信息、图片列表、图片详情等功能
 *
 * @author sakanal
 */
@Slf4j
@Data
@Configuration
public class PixivUtils {

    /**
     * 数字匹配正则表达式，用于从JSON中提取图片ID等数字信息
     */
    /**
     * 数字匹配正则表达式，用于从JSON中提取图片ID等数字信息
     */
    public static Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    @Resource
    private MyPixivConfig myPixivConfig;
    @Resource
    private SeleniumUtils seleniumUtils;


    /**
     * 获取指定画师的所有作品ID列表
     * 通过调用Pixiv的Ajax接口获取画师的所有作品信息
     *
     * @param userId   画师的Pixiv用户ID
     * @param userName 画师的用户名
     * @return 作品列表，包含userId、userName、pictureId、pageCount、type、status等基础信息
     *         如果获取失败则返回null
     */
    public List<Picture> initPictureList(Long userId, String userName) {
        String allPictureAjaxUrl = "https://www.pixiv.net/ajax/user/" + userId + "/profile/all";

        try (InputStream inputStream = getInputStream(allPictureAjaxUrl)) {
            if (inputStream == null) {
                return null;
            }

            String result = getUrlResult(inputStream);
            if (!StringUtils.hasText(result)) {
                log.error("获取所有作品数据失败，请检查网络情况");
                return null;
            }

            JSONObject bodyObj = parseJsonBody(result);
            if (bodyObj == null) {
                return null;
            }

            Object illusts = bodyObj.get("illusts");
            Matcher matcher = NUMBER_PATTERN.matcher(illusts.toString());
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
        } catch (IOException e) {
            log.error("获取所有作品数据失败", e);
            return null;
        }
    }


    /**
     * 获取画师的用户名
     * 通过调用Pixiv的用户资料接口获取画师的显示名称
     *
     * @param userId 画师的Pixiv用户ID
     * @return 画师的用户名（已去除" - pixiv"后缀），获取失败返回null
     */
    public String getUserName(Long userId) {
        String ajaxUrl = "https://www.pixiv.net/ajax/user/" + userId + "/profile/top";

        try (InputStream inputStream = getInputStream(ajaxUrl)) {
            if (inputStream == null) {
                return null;
            }

            String result = getUrlResult(inputStream);
            if (!StringUtils.hasText(result)) {
                log.error("获取作者名称失败，请检查网络情况");
                return null;
            }

            JSONObject bodyObj = parseJsonBody(result);
            if (bodyObj == null) {
                return null;
            }

            JSONObject extraDataObj = JSONUtil.parseObj(bodyObj.get("extraData"));
            JSONObject metaObj = JSONUtil.parseObj(extraDataObj.get("meta"));
            return metaObj.getStr("title").replace(" - pixiv", "");
        } catch (IOException e) {
            log.error("获取作者名称失败", e);
            return null;
        }
    }


    /**
     * 处理图片组数据，为图片组中的每一张图片生成独立的Picture对象
     * 通过替换URL中的页码标识符（_p0, _p1, ...）来生成每张图片的URL
     *
     * @param i          当前图片在图片组中的索引（从0开始）
     * @param oldPicture 原始图片对象，作为模板使用
     * @return 新的Picture对象，包含对应页码的URL和pageCount
     */
    public Picture getResultPicture(int i, Picture oldPicture) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(oldPicture, picture);
        picture.setSrc(oldPicture.getSrc().replace("_p0", "_p" + i));
        picture.setPageCount((i + 1));
        return picture;
    }

    /**
     * 根据URL获取网络连接的输入流
     * 统一处理各种网络异常，包括SSL异常、超时异常、文件不存在等
     *
     * @param url 目标URL地址
     * @return 输入流对象，获取失败返回null
     */
    public InputStream getInputStream(String url) {
        URLConnection urlConnection = getUrlConnection(url);
        if (urlConnection == null) {
            return null;
        }
        try {
            return urlConnection.getInputStream();
        } catch (SSLHandshakeException sslHandshakeException) {
            log.error("SSLHandshakeException异常,message={}", sslHandshakeException.getMessage());
        } catch (SocketException socketException) {
            log.error("SocketException异常,message={}", socketException.getMessage());
        } catch (SocketTimeoutException socketTimeoutException) {
            log.error("SocketTimeoutException异常,message={}", socketTimeoutException.getMessage());
        } catch (FileNotFoundException fileNotFoundException) {
            log.error("文件不存在", fileNotFoundException);
        } catch (IOException e) {
            log.error("建立连接失败，请检查请求头是否有效，也有可能是作者销号了", e);
        }
        return null;
    }

    /**
     * 根据Picture对象获取图片的输入流
     * 如果首次获取失败，会尝试获取原始图片链接（针对缩略图URL的情况）
     *
     * @param picture 图片信息对象，需要包含有效的src属性
     * @return 图片输入流，获取失败返回null
     */
    public InputStream getInputStream(Picture picture) {
        InputStream inputStream = getInputStream(picture.getSrc());
        if (inputStream != null) {
            picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
            return inputStream;
        }

        // 获取失败，尝试获取原始链接
        log.error("获取数据失败，尝试获取原始链接");
        picture.setStatus(PictureStatusConstant.FAIL_STATUS);

        boolean flag = getPictureOriginalUrl(picture);
        if (flag) {
            // 成功获取到源链接，再次尝试获取输入流
            inputStream = getInputStream(picture.getSrc());
            if (inputStream != null) {
                picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
                return inputStream;
            } else {
                log.error("获取原始链接数据再次失败，此次大概率为网络问题");
            }
        } else {
            // 获取链接失败，应该是gif文件
            log.error("获取数据失败，大概率为gif文件，需要自主下载");
        }

        return null;
    }

    /**
     * 获取连接并设置参数
     *
     * @param url 链接地址
     * @return URLConnection
     */
    private URLConnection getUrlConnection(String url) {
        URLConnection urlConnection;
        try {
            urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(20000);
            urlConnection.setUseCaches(false);
        } catch (IOException e) {
            log.error("建立连接失败，请检查代理以及网络情况", e);
            return null;
        }
        Set<String> keySet = myPixivConfig.getRequestHeader().keySet();
        for (String key : keySet) {
            String value = myPixivConfig.getRequestHeader().get(key);
            urlConnection.setRequestProperty(key, value);
        }
        return urlConnection;
    }

    /**
     * 将输入流转换为字符串
     * 使用配置的字符集进行编码转换
     *
     * @param inputStream 输入流对象
     * @return 转换后的字符串，转换失败返回null
     */
    public String getUrlResult(InputStream inputStream) {
        try (InputStreamReader inputStreamReader = createInputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            log.error("获取请求结果失败，请检查网络状态", e);
            return null;
        }
    }

    /**
     * 创建InputStreamReader，支持自定义字符集
     */
    private InputStreamReader createInputStreamReader(InputStream inputStream) throws UnsupportedEncodingException {
        if (StringUtils.hasText(myPixivConfig.getCharsetName())) {
            return new InputStreamReader(inputStream, myPixivConfig.getCharsetName());
        } else {
            return new InputStreamReader(inputStream);
        }
    }

    /**
     * 解析JSON响应中的body部分
     *
     * @param jsonResponse JSON响应字符串
     * @return 解析后的body JSON对象
     */
    private JSONObject parseJsonBody(String jsonResponse) {
        try {
            JSONObject responseObj = JSONUtil.parseObj(jsonResponse);
            return JSONUtil.parseObj(responseObj.get("body"));
        } catch (Exception e) {
            log.error("解析JSON body失败", e);
            return null;
        }
    }

    /**
     * 获取高清图片链接
     * 将Pixiv的缩略图URL转换为原图URL
     * 例如：https://i.pximg.net/c/250x250_80_a2/img-master/img/2022/07/06/00/13/07/99529275_p0_square1200.jpg
     * 转换为：https://i.pximg.net/img-original/img/2022/07/06/00/13/07/99529275_p0.jpg
     *
     * @param src 原始缩略图链接
     * @return 高清原图链接
     */
    public String getRealSrc(String src) {
        src = src.replace("c/250x250_80_a2/", "").replace("_square1200", "");
        //https://i.pximg.net/img-master/img/2022/07/06/00/13/07/99529275_p0.jpg
        src = src.replace("_custom1200", "").replace("custom-thumb", "img-original");
        return src.replace("img-master", "img-original");
    }

    /**
     * 获取图片的原始链接
     * 通过调用Pixiv的Ajax接口获取图片的详细信息，包括原图URL
     * 对于GIF文件（illustType==2）不会获取原图链接
     *
     * @param picture 图片对象，需要包含有效的pictureId
     * @return 是否成功获取到原图链接（GIF文件返回false）
     */
    public boolean getPictureOriginalUrl(Picture picture) {
        String url = "https://www.pixiv.net/ajax/illust/" + picture.getPictureId();
        try (InputStream inputStream = getInputStream(url)) {
            if (inputStream == null) {
                log.error("通过链接获取数据失败");
                return false;
            }

            String result = getUrlResult(inputStream);
            if (!StringUtils.hasText(result)) {
                log.error("数据解析失败");
                return false;
            }

            // 将 body 解析结果存储在 bodyObj 中，避免重复解析
            JSONObject bodyObj = parseJsonBody(result);
            if (bodyObj != null) {
                picture.setTitle(bodyObj.getStr("title"));
                // type==2应该是gif文件
                if (bodyObj.getInt("illustType") != 2) {
                    picture.setSrc(JSONUtil.parseObj(bodyObj.get("urls")).getStr("original"));
                    return true;
                }
            }

        } catch (Exception e) {
            log.error("通过连接获取数据出现异常", e);
        }
        return false;
    }

    /**
     * 根据图片ID获取图片的完整下载信息
     * 包括userId、userName、title、pageCount、src等
     * 如果是图片组，只返回首张图片的信息，但pageCount会大于1
     *
     * @param pictureId 图片的Pixiv作品ID
     * @return 包含完整信息的Picture对象，获取失败返回null
     */
    public Picture getPictureInfo(Long pictureId) {
        //https://www.pixiv.net/ajax/illust/110090680?lang=zh&version=b461aaba721300d63f4506a979bf1c3e6c11df13 可以获取到所有的数据
        String url = "https://www.pixiv.net/ajax/illust/" + pictureId;

        try (InputStream inputStream = getInputStream(url)) {
            if (inputStream == null) {
                log.error("通过链接获取图片数据失败");
                return null;
            }

            String result = getUrlResult(inputStream);
            if (!StringUtils.hasText(result)) {
                log.error("数据解析失败");
                return null;
            }

            JSONObject bodyObj = parseJsonBody(result);
            if (bodyObj == null) {
                return null;
            }

            Picture picture = new Picture();
            picture.setPictureId(pictureId);
            picture.setUserId(bodyObj.getLong("userId"));
            picture.setUserName(bodyObj.getStr("userName"));
            picture.setTitle(bodyObj.getStr("title"));
            picture.setPageCount(bodyObj.getInt("pageCount"));
            picture.setSrc(JSONUtil.parseObj(bodyObj.get("urls")).getStr("original"));
            picture.setType(SourceConstant.PIXIV_SOURCE);

            return picture;
        } catch (Exception e) {
            log.error("通过连接获取图片所需数据出现异常", e);
            return null;
        }
    }

}
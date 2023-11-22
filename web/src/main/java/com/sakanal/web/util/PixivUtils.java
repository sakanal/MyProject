package com.sakanal.web.util;

import cn.hutool.json.JSONUtil;
import com.sakanal.web.config.MyPixivConfig;
import com.sakanal.web.constant.PictureStatusConstant;
import com.sakanal.web.constant.SourceConstant;
import com.sakanal.web.entity.Picture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

@Slf4j
@Data
@Configuration
//@ConfigurationProperties(prefix = "system.pixiv")
public class PixivUtils {

    @Resource
    private MyPixivConfig myPixivConfig;
    @Resource
    private SeleniumUtils seleniumUtils;

    /**
     * 根据链接获取数据，通用
     * @param url 链接
     * @return inputStream数据
     */
    public InputStream getInputStream(String url) {
        URLConnection urlConnection = getURLConnection(url);
        if (urlConnection == null) {
            return null;
        }
        InputStream inputStream;
        try {
            inputStream = urlConnection.getInputStream();
        } catch (IOException e) {
            log.info("建立连接失败，请检查请求头是否有效，也有可能是作者销号了");
            e.printStackTrace();
            return null;
        }
        return inputStream;
    }

    /**
     * 根据图片url获取图片数据
     * @param picture 图片信息，需要其中的src数据
     * @return 图片源数据
     */
    public InputStream getInputStream(Picture picture) {
        URLConnection urlConnection = getURLConnection(picture.getSrc());
        if (urlConnection == null) {
            return null;
        }
        InputStream inputStream;
        try {
            inputStream = urlConnection.getInputStream();
        } catch (IOException e) {
            log.info("获取数据失败，可能是后缀不匹配，也可能是网络问题");
            // 获取源链接
            picture.setStatus(PictureStatusConstant.FAIL_STATUS);
            boolean flag = getPictureOriginalUrl(picture);
            if (flag) {
                // 成功获取到源链接
                try {
                    urlConnection = getURLConnection(picture.getSrc());
                    if (urlConnection == null) {
                        return null;
                    }
                    inputStream = urlConnection.getInputStream();
                } catch (IOException exception) {
                    log.error("获取数据再次失败，此次大概率为网络问题");
                    exception.printStackTrace();
                    return null;
                }
            } else {
                // 获取链接失败，应该是gif文件
                log.error("获取数据失败，大概率为gif文件，需要自主下载");
                return null;
            }
        }
        picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
        return inputStream;
    }

    /**
     * 获取连接并设置参数
     * @param url 链接地址
     * @return URLConnection
     */
    private URLConnection getURLConnection(String url) {
        URLConnection urlConnection;
        try {
            urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(20000);
            urlConnection.setUseCaches(false);
        } catch (IOException e) {
            log.info("建立连接失败，请检查代理以及网络情况");
            e.printStackTrace();
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
     * 转换链接数据
     * @param inputStream 获取到的链接数据
     * @return 转换后的String类型数据
     */
    public String getUrlResult(InputStream inputStream) {
        InputStreamReader inputStreamReader;
        if (StringUtils.hasText(myPixivConfig.getCharsetName())) {
            try {
                inputStreamReader = new InputStreamReader(inputStream, myPixivConfig.getCharsetName());
            } catch (UnsupportedEncodingException e) {
                log.info("不支持的编码格式");
                e.printStackTrace();
                return null;
            }
        } else {
            inputStreamReader = new InputStreamReader(inputStream);
        }

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        StringBuilder builder = new StringBuilder();
        try {
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            log.info("获取请求结果失败，请检查网络状态");
            e.printStackTrace();
            return null;
        } finally {
            closeConnection(bufferedReader, inputStream, inputStreamReader);
        }
        return builder.toString();
    }

    /**
     * 关闭连接
     * @param bufferedReader bufferedReader
     * @param inputStream inputStream
     * @param inputStreamReader inputStreamReader
     */
    public void closeConnection(BufferedReader bufferedReader, InputStream inputStream, InputStreamReader inputStreamReader) {
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取高清图片链接
     *
     * @param src 原始图片连接 https://i.pximg.net/c/250x250_80_a2/img-master/img/2022/07/06/00/13/07/99529275_p0_square1200.jpg
     * @return 高清图片连接 https://i.pximg.net/img-original/img/2022/07/06/00/13/07/99529275_p0.jpg
     */
    public String getRealSrc(String src) {
        src = src.replace("c/250x250_80_a2/", "").replace("_square1200", "");
        //https://i.pximg.net/img-master/img/2022/07/06/00/13/07/99529275_p0.jpg
        src = src.replace("_custom1200", "").replace("custom-thumb", "img-original");
        return src.replace("img-master", "img-original");
    }

    /**
     * 获取原图连接
     * @param picture 图片，只需要图片id即可
     * @return 如果是gif返回null，否则返回原图链接，不需要考虑后缀的问题
     */
    public boolean getPictureOriginalUrl(Picture picture) {
        String url = "https://www.pixiv.net/ajax/illust/" + picture.getPictureId();
        InputStream inputStream = getInputStream(url);
        if (inputStream == null) {
            log.error("通过链接获取数据失败");
            return false;
        }
        String result = getUrlResult(inputStream);
        if (!StringUtils.hasText(result)) {
            log.error("数据解析失败");
            return false;
        }
        Object body = JSONUtil.parseObj(result).get("body");
        String title = (String) JSONUtil.parseObj(body).get("title");
        String originalURL = (String) JSONUtil.parseObj(JSONUtil.parseObj(body).get("urls")).get("original");
        Integer type = (Integer) JSONUtil.parseObj(body).get("illustType");
        picture.setTitle(title);
        // type==2应该是gif文件
        if (type != 2) {
            picture.setSrc(originalURL);
            return true;
        }
        closeConnection(null,inputStream,null);
        return false;
    }

    /**
     * 根据图片id获取图片的所有所需信息，如果是图片组则只有首张图片的信息，pageCount会>1
     * @param pictureId 图片id
     * @return 该图片的所有下载所需的信息
     */
    public Picture getPictureInfo(Long pictureId) {
        //
        //https://www.pixiv.net/ajax/illust/110090680?lang=zh&version=b461aaba721300d63f4506a979bf1c3e6c11df13 可以获取到所有的数据
        String url = "https://www.pixiv.net/ajax/illust/" + pictureId;

        InputStream inputStream = getInputStream(url);
        if (inputStream==null){
            log.error("通过链接获取数据失败");
            return null;
        }
        String result = getUrlResult(inputStream);
        if (!StringUtils.hasText(result)){
            log.error("数据解析失败");
            return null;
        }
//        String result = HttpUtil.get(url);
        Object body = JSONUtil.parseObj(result).get("body");
        String userId = (String) JSONUtil.parseObj(body).get("userId");
        String userName = (String) JSONUtil.parseObj(body).get("userName");
        String title = (String) JSONUtil.parseObj(body).get("title");
        Integer type = (Integer) JSONUtil.parseObj(body).get("illustType");
        Integer pageCount = (Integer) JSONUtil.parseObj(body).get("pageCount");
        String originalURL = (String) JSONUtil.parseObj(JSONUtil.parseObj(body).get("urls")).get("original");
        Picture picture = new Picture();
        picture.setPictureId(pictureId);
        picture.setUserId(Long.valueOf(userId));
        picture.setUserName(userName);
        picture.setTitle(title);
        picture.setPageCount(pageCount);
        picture.setSrc(originalURL);
        picture.setType(SourceConstant.PIXIV_SOURCE);

        closeConnection(null,inputStream,null);
        return picture;
    }

}

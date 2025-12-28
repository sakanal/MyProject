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
     * 获取该画师的所有作品数据，对结果进行转换  解析数据，获取pictureId，设置userId/userName/PictureId/pageCount/type/status
     *
     * @param userId   当前用户id
     * @param userName 当前用户姓名
     * @return List<Picture> userId/userName/PictureId/pageCount/type/status
     */
    public List<Picture> initPictureList(Long userId, String userName) {
        String allPictureAjaxURL = "https://www.pixiv.net/ajax/user/" + userId + "/profile/all";

        try (InputStream inputStream = getInputStream(allPictureAjaxURL)) {
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
        } catch (IOException e) {
            log.error("获取所有作品数据失败", e);
            return null;
        }
    }


    /**
     * 获取作者名称
     *
     * @param userId 作者id
     * @return 作者名称
     */
    public String getUserName(Long userId) {
        String ajaxURL = "https://www.pixiv.net/ajax/user/" + userId + "/profile/top";

        try (InputStream inputStream = getInputStream(ajaxURL)) {
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
     * 获取图片组，替换url中的图片组编号
     *
     * @param i          当前图片的count值
     * @param oldPicture 当前pictureId模板
     * @return 最终图片数据，相同的pictureId，不同的pageCount以及src
     */
    public Picture getResultPicture(int i, Picture oldPicture) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(oldPicture, picture);
        picture.setSrc(oldPicture.getSrc().replace("_p0", "_p" + i));
        picture.setPageCount((i + 1));
        return picture;
    }

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
     * 根据图片url获取图片数据，如果获取失败则尝试获取原始URL
     * @param picture 图片信息，需要其中的src数据
     * @return 图片源数据
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
            log.error("建立连接失败，请检查代理以及网络情况",e);
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
     * 根据图片id获取图片的所有所需信息，如果是图片组则只有首张图片的信息，pageCount会>1
     * @param pictureId 图片id
     * @return 该图片的所有下载所需的信息
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

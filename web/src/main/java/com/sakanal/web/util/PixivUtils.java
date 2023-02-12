package com.sakanal.web.util;

import com.sakanal.web.constant.PictureStatusConstant;
import com.sakanal.web.entity.Picture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system.pixiv")
public class PixivUtils {
    private Map<String, String> requestHeader;
    private String charsetName;

    public InputStream getInputStream(String url) {
        URLConnection urlConnection = getURLConnection(url);
        if (urlConnection == null) {
            return null;
        }
        InputStream inputStream;
        try {
            inputStream = urlConnection.getInputStream();
        } catch (IOException e) {
            log.info("建立连接失败，请检查请求头是否有效");
            e.printStackTrace();
            return null;
        }
        return inputStream;
    }

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
            log.info("第一次尝试：再次建立连接");
            picture.setStatus(PictureStatusConstant.FAIL_STATUS);
            try {
                inputStream = urlConnection.getInputStream();
            } catch (IOException ex) {
                log.info("第二次尝试：修改后缀");
                changeSuffix(picture);
                urlConnection = getURLConnection(picture.getSrc());
                if (urlConnection == null) {
                    return null;
                }
                try {
                    inputStream = urlConnection.getInputStream();
                } catch (IOException exc) {
                    log.info("两次尝试都失败，等待后续补充下载");
                    exc.printStackTrace();
                    return null;
                }
            }
        }
        picture.setStatus(PictureStatusConstant.DEFAULT_STATUS);
        return inputStream;
    }

    private void changeSuffix(Picture picture) {
        String src = picture.getSrc();
        String[] split = src.split("\\.");
        String suffix = split[split.length - 1];
        if ("jpg".equals(suffix)){
            log.info("当前后缀为" + suffix+"改为png");
            src = src.replace("jpg","png");
        }else {
            log.info("当前后缀为" + suffix+"改为jpg");
            src = src.replace("png","jpg");
        }
        picture.setSrc(src);
    }

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
        Set<String> keySet = requestHeader.keySet();
        for (String key : keySet) {
            String value = requestHeader.get(key);
            urlConnection.setRequestProperty(key, value);
        }
        return urlConnection;
    }

    public String getUrlResult(InputStream inputStream) {
        InputStreamReader inputStreamReader;
        if (StringUtils.hasText(charsetName)) {
            try {
                inputStreamReader = new InputStreamReader(inputStream, charsetName);
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

    public void closeConnection(BufferedReader bis, InputStream is, InputStreamReader inputStreamReader) {
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bis != null) {
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (is != null) {
            try {
                is.close();
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

}

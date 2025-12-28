package pixiv;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import pixiv.utils.CheckPixivPicture;
import pixiv.bean.Picture;
import pixiv.utils.ConnectionUtils;
import pixiv.utils.PixivUtils;
import pixiv.utils.URLConnectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsoupPixivUser2 {
    //1960050 torino
    //56627683 SWKL·D
    //14801956 void_0
    //103410 スコッティ
    //1878082 モ誰
    //67093476 日宝
    //162475 煎茶
    //6558698 siki
    //70050825 ほうき星
    private static final Integer userId=70050825;
    private static final Integer lastPictureId=0;
    private static String userName=null;

    private static final String simpleDirName="E:\\图片\\pixiv\\";

    private static final List<Picture> failPicture = new ArrayList<>();

    private static final String proxyHost = "127.0.0.1";
    private static final String proxyPort = "7890";

    public static void main(String[] args) throws Exception {
        //开启代理
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);

        String url="https://www.pixiv.net/ajax/user/"+userId+"/profile/all";
        //获取ajax请求结果
        String ids = fetchUrl(url, null);
        //解析结果，获取id列表
        List<Picture> pictureList = initPictureList(ids);
        {
            //提前获取画师名称
            String ajaxUrl = getAjaxUrl(pictureList.get(0));
            String pictureInfo = getPictureInfo(ajaxUrl);
            CheckPixivPicture.getRealPictureList(simpleDirName,pictureInfo,pictureList);
        }
        download(pictureList);
    }

    /**
     * 截取url 获取到该画师的所有作品id
     * @param url   可以获取到图片详细信息的Ajax链接
     * @param code  默认为UTF-8
     * @return      返回ajax请求结果
     */
    public static String fetchUrl(String url,String code) throws IOException {
        if (code==null)
            code="UTF-8";
        BufferedReader bis = null;
        InputStream is = null;
        InputStreamReader inputStreamReader = null;
        try {
            URLConnection connection = URLConnectionUtils.getURLConnection(url);
            is = URLConnectionUtils.getInputStream(connection);
            if (is==null){
                return null;
            }
            inputStreamReader = new InputStreamReader(is, code);
            bis = new BufferedReader(inputStreamReader);
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = bis.readLine()) != null) {
                result.append(line);
            }
            System.out.println("获取ajax请求成功");
            return result.toString();
        } finally {
            ConnectionUtils.close(bis,is,inputStreamReader);
        }
    }
    //初始化图片列表，将所有图片id放入列表中
    public static List<Picture> initPictureList(String ids){
        JSONObject jsonObject = JSONUtil.parseObj(ids);
        Object body = jsonObject.get("body");
        jsonObject  = JSONUtil.parseObj(body);
        Object illusts = jsonObject.get("illusts");
        Matcher matcher = Pattern.compile("[0-9]+").matcher(illusts.toString());
        List<Picture> pictureList = new ArrayList<>();
        while (matcher.find()){
            String id=matcher.group();
            if (Integer.valueOf(id).equals(lastPictureId))
                break;
            pictureList.add(new Picture(null,id,null,null,null));
        }
        return pictureList;
    }

    /**
     * 获取所有图片的ajax的请求链接
     * @param pictureList 集合内只需要该画师的所有作品id
     * @return 获取图片的请求链接 格式为https://www.pixiv.net/ajax/user/userId/profile/illusts?ids[]=pictureId&work_category=illustManga&is_first_page=1&lang=zh
     */
    public static String getAjaxUrl(List<Picture> pictureList){
        StringBuilder url= new StringBuilder("https://www.pixiv.net/ajax/user/" + userId + "/profile/illusts?");
        for (Picture picture : pictureList) {
            url.append("ids[]=").append(picture.getId()).append("&");
        }
        url.append("work_category=illustManga&is_first_page=1&lang=zh");
        return new String(url);
    }

    /**
     * 获取图片ajax请求链接，主要用于获取画师名称
     * @param picture 图片的部分基础信息，主要使用图片id
     * @return 获取图片的请求链接 格式为https://www.pixiv.net/ajax/user/userId/profile/illusts?ids[]=pictureId&work_category=illustManga&is_first_page=1&lang=zh
     */
    public static String getAjaxUrl(Picture picture){
        ArrayList<Picture> pictureList = new ArrayList<>();
        pictureList.add(picture);
        return getAjaxUrl(pictureList);
    }

    /**
     * 获取完整图片信息
     * @param ajaxUrl https://www.pixiv.net/ajax/user/userId/profile/illusts?ids[]=pictureId&work_category=illustManga&is_first_page=1&lang=zh
     * @return json格式的图片消息
     * @throws Exception bufferedReader.readLine()
     */
    public static String getPictureInfo(String ajaxUrl) throws Exception{
        //根据ajax链接获取图片的详细信息
        URLConnection urlConnection = URLConnectionUtils.getURLConnection(ajaxUrl);
        InputStream inputStream = URLConnectionUtils.getInputStream(urlConnection);
        if (inputStream==null){
            return null;
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }
        return new String(result);
    }

    /**
     * 解析json格式的图片消息，并将其保存到集合中
     * @param result json格式的图片信息
     * @param pictures 存放图片的重要信息
     */
    public static void getPictureList(String result, List<Picture> pictures){
        //将获取到图片信息封装到对象中：id，标题，src，该链接下的图片总数，作者
        Object body = JSONUtil.parseObj(result).get("body");
        Object works = JSONUtil.parseObj(body).get("works");
        JSONObject jsonObject = JSONUtil.parseObj(works);
        for (Picture picture : pictures) {
            Object pictureInfo = jsonObject.get(picture.getId());
            JSONObject tempJsonObject = JSONUtil.parseObj(pictureInfo);
            String title = (String) tempJsonObject.get("title");
            title = PixivUtils.checkTitle(title);
            picture.setTitle(title);
            picture.setPageCount((Integer) tempJsonObject.get("pageCount"));
            String realSrc = PixivUtils.getRealSrc((String) tempJsonObject.get("url"));
            picture.setSrc(realSrc);
            picture.setUserName((String) tempJsonObject.get("userName"));
        }
        userName = PixivUtils.checkTitle(pictures.get(0).getUserName());
    }

    /**
     * 下载模块最外层，用于计时，分批下载和重新下载失败图片
     * @param pictureList 图片集合
     * @throws Exception getPictureInfo()
     */
    public static void download(List<Picture> pictureList) throws Exception {
        long start = System.currentTimeMillis();
        if (pictureList.size() > 80) {
            int count = Math.toIntExact(Math.round(Math.ceil(pictureList.size() / 80.0)));
            int offset = 0;
            for (int i = 0; i < count; i++, offset += 80) {
                List<Picture> tempPictureList = new ArrayList<>();
                for (int j = 0; j < 80; j++) {
                    if ((j + offset) < pictureList.size()) {
                        tempPictureList.add(pictureList.get(j + offset));
                    } else {
                        break;
                    }
                }
                String ajaxUrl = getAjaxUrl(tempPictureList);
                String pictureInfo = getPictureInfo(ajaxUrl);
                getPictureList(pictureInfo, tempPictureList);
                download(tempPictureList, offset);
            }
        } else {
            String ajaxUrl = getAjaxUrl(pictureList);
            String pictureInfo = getPictureInfo(ajaxUrl);
            getPictureList(pictureInfo, pictureList);
            download(pictureList, 0);
        }

        long end = System.currentTimeMillis();
        System.out.println("总需要下载" + pictureList.size() + "份图片");
        System.out.println("下载完成，耗时：" + (end - start) / 1000 + "秒");

        if (failPicture.size()>0){
            System.out.println("下载失败的图片：");
            failPicture.forEach(System.out::println);
            int sign=0;
            while (failPicture.size()>0){
                sign++;
                System.out.println("第"+sign+"轮下载，有"+failPicture.size()+"张图片待下载");
                Iterator<Picture> iterator = failPicture.iterator();
                while (iterator.hasNext()){
                    Picture next = iterator.next();
                    boolean flag = downloadFile(next);
                    if (flag){
                        System.out.println(next+"下载成功");
                        iterator.remove();
                    }else {
                        System.out.println(next+"下载失败");
                    }
                }
                if (sign>5){
                    System.out.println("五次循环后仍未下载成功的图片信息：");
                    System.out.println(failPicture);
                    break;
                }
            }
        }
    }
    /**
     * 下载模块中层，主要用于区分批次下载
     * @param pictureList 图片集合
     * @param offset 偏移量，用于统计已经下载了多少张图片
     */
    public static void download(List<Picture> pictureList,Integer offset) {
        for (int i = 0; i < pictureList.size(); i++) {
            Integer pageCount = pictureList.get(i).getPageCount();
            if (pageCount > 1) {
                Picture picture = pictureList.get(i);
                for (int j = 1; j <= pageCount; j++) {
                    if (j != 1) {
                        String replaceSrc = picture.getSrc().replace("_p" + (j - 2), "_p" + (j - 1));
                        picture.setSrc(replaceSrc);
                    }
                    picture.setPageCount(j);
                    System.out.println("第" + (i + 1 + offset) + "组图片，第" + (j) + "张图片开始下载");
                    downloadFile(picture);
                    System.out.println("第" + (i + 1 + offset) + "组图片，第" + (j) + "张图片完成");
                }
            } else {
                System.out.println("第" + (i + 1 + offset) + "张图片开始下载");
                downloadFile(pictureList.get(i));
                System.out.println("第" + (i + 1 + offset) + "张图片完成");
            }
        }
    }
    /**
     * 正式下载图片
     * @param picture 图片信息
     */
    public static boolean downloadFile(Picture picture){
        URLConnection urlConnection;
        InputStream inputStream;
        try {
            urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
            inputStream  = URLConnectionUtils.getInputStream(urlConnection);
        } catch (IOException e) {
            String src = picture.getSrc();
            String[] split = src.split("\\.");
            String imageFormat = split[split.length - 1];
            System.out.println("src = " + src);
            System.out.println("imageFormat = " + imageFormat);
            if ("jpg".equals(imageFormat)){
                System.out.println("改为png");
                picture.setSrc(src.replace("jpg","png"));
            }else {
                System.out.println("改为jpg");
                picture.setSrc(src.replace("png","jpg"));
            }
            try {
                urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
                inputStream  = URLConnectionUtils.getInputStream(urlConnection);
            } catch (IOException ex) {
                ex.printStackTrace();
                failPicture.add(picture);
                return false;
            }
        }
        String dirName=simpleDirName+userName+"\\";
        return PixivUtils.downloadPicture(dirName,picture,inputStream);
    }

}

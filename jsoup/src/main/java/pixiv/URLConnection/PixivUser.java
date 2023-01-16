package pixiv.URLConnection;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PixivUser {
    private final String userId;
    private String userName=null;

    private final String simpleDirName;

    private final List<Picture> pictureList;
    public static List<Picture> failPictureList = new ArrayList<>();

    public PixivUser(String userId, String simpleDirName) throws Exception {
        this.userId=userId;
        this.simpleDirName=simpleDirName;
        String AllPictureAjaxURL="https://www.pixiv.net/ajax/user/"+userId+"/profile/all";
        //获取ajax请求结果
        String ids = fetchUrl(AllPictureAjaxURL, null);
        //解析结果，获取id列表
        this.pictureList = initPictureList(ids);
        {
            //提前获取画师名称
            String ajaxUrl = getAjaxUrl(pictureList.get(0));
            String pictureInfo = getPictureInfo(ajaxUrl);
            userName = CheckPixivPicture.getRealPictureList(simpleDirName, pictureInfo, pictureList);
        }
    }

    /**
     * 截取url 获取到该画师的所有作品id
     * @param url   可以获取到图片详细信息的Ajax链接
     * @param code  默认为UTF-8
     * @return      返回ajax请求结果
     */
    public String fetchUrl(String url,String code) throws IOException {
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
    public List<Picture> initPictureList(String ids){
        JSONObject jsonObject = JSONUtil.parseObj(ids);
        Object body = jsonObject.get("body");
        jsonObject  = JSONUtil.parseObj(body);
        Object illusts = jsonObject.get("illusts");
        Matcher matcher = Pattern.compile("[0-9]+").matcher(illusts.toString());
        List<Picture> pictureList = new ArrayList<>();
        while (matcher.find()){
            String id=matcher.group();
            pictureList.add(new Picture(id));
        }
        return pictureList;
    }

    /**
     * 获取所有图片的ajax的请求链接
     * @param pictureList 集合内只需要该画师的所有作品id
     * @return 获取图片的请求链接 格式为https://www.pixiv.net/ajax/user/userId/profile/illusts?ids[]=pictureId&work_category=illustManga&is_first_page=1&lang=zh
     */
    public String getAjaxUrl(List<Picture> pictureList){
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
    public String getAjaxUrl(Picture picture){
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
    public String getPictureInfo(String ajaxUrl) throws Exception{
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
    public void getPictureList(String result, List<Picture> pictures){
        //将获取到图片信息封装到对象中：id，标题，src，该链接下的图片总数，作者
        Object body = JSONUtil.parseObj(result).get("body");
        Object works = JSONUtil.parseObj(body).get("works");
        JSONObject jsonObject = JSONUtil.parseObj(works);
        for (Picture picture : pictures) {
            Object pictureInfo = jsonObject.get(picture.getId());
            JSONObject tempJsonObject = JSONUtil.parseObj(pictureInfo);
            String title = (String) tempJsonObject.get("title");
            title = PixivUtils.checkTitle(title);
            picture.setUserId((String) tempJsonObject.get("userId"));
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
     * @throws Exception getPictureInfo()
     */
    public void download() throws Exception {
        long start = System.currentTimeMillis();
        if (pictureList.size()>0) {
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
        }else {
            System.out.println("画师:"+userName+"\tuserID:"+userId+"\t暂无新画作，无需下载");
        }
    }
    /**
     * 下载模块中层，主要用于区分批次下载
     * @param pictureList 图片集合
     * @param offset 偏移量，用于统计已经下载了多少张图片
     */
    public void download(List<Picture> pictureList,Integer offset) {
        for (int i = 0; i < pictureList.size(); i++) {
            Integer pageCount = pictureList.get(i).getPageCount();
//            System.out.println(pictureList.get(i));
            if (pageCount > 1) {
                Picture picture = pictureList.get(i);
                for (int j = 1; j <= pageCount; j++) {
                    if (j != 1) {
                        String replaceSrc = picture.getSrc().replace("_p" + (j - 2), "_p" + (j - 1));
                        picture.setSrc(replaceSrc);
                    }
                    picture.setPageCount(j);
                    System.out.println("第" + (i + 1 + offset) + "组图片，第" + (j) + "张图片开始下载");
                    boolean result = downloadFile(picture);
                    if(result){
                        System.out.println("第" + (i + 1 + offset) + "组图片，第" + (j) + "张图片完成");
                    }else {
                        System.out.println("第" + (i + 1 + offset) + "组图片，第" + (j) + "张图片下载失败，等待重新下载该组图片");
                        break;
                    }
                }
            } else {
                System.out.println("第" + (i + 1 + offset) + "张图片开始下载");
                boolean result = downloadFile(pictureList.get(i));
                if (result){
                    System.out.println("第" + (i + 1 + offset) + "张图片完成");
                }else {
                    System.out.println("第" + (i + 1 + offset) + "张图片下载失败，等待后续下载");
                }
            }
        }
    }
    /**
     * 正式下载图片
     * @param picture 图片信息
     */
    public boolean downloadFile(Picture picture){
        URLConnection urlConnection;
        InputStream inputStream;
        try {
            urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
            inputStream  = URLConnectionUtils.getInputStream(urlConnection);
        } catch (IOException e) {
            System.out.println("第一次修改后缀");
            picture.setSrc(PixivUtils.changeSrcFormat(picture.getSrc()));
            try {
                urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
                inputStream  = URLConnectionUtils.getInputStream(urlConnection);
            } catch (IOException ex) {
                System.out.println("修改后缀依然无法建立连接，尝试再次修改为原来的后缀");
                picture.setSrc(PixivUtils.changeSrcFormat(picture.getSrc()));
                try {
                    urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
                    inputStream  = URLConnectionUtils.getInputStream(urlConnection);
                }catch (IOException ioe){
                    System.out.println("两次修改后缀都失败");
                    failPictureList.add(picture);
                    return false;
                }
            }
        }
        String dirName=simpleDirName+userName+"-"+picture.getUserId()+"\\";
        boolean result = PixivUtils.downloadPicture(dirName, picture, inputStream);
        if (!result){
            failPictureList.add(picture);
            return false;
        }else {
            return true;
        }
    }

    public static void downloadFailPicture(String simpleDirName){
        System.out.println("下载失败的图片：");
        failPictureList.forEach(System.out::println);
        List<Picture> resultFail = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (Picture picture : failPictureList) {
            boolean flag = new PixivPicture(picture, simpleDirName).download();
            if (!flag)
                resultFail.add(picture);
        }
        long end = System.currentTimeMillis();
        System.out.println("下载耗时"+((end-start)/1000)+"秒");
        if (resultFail.size()>0){
            System.out.println("仍然下载失败的图片，大概率是动图");
            resultFail.forEach(System.out::println);
        }else {
            System.out.println("补充下载完成");
        }
    }
}

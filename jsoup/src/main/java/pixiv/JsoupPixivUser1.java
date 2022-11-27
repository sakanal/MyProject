package pixiv;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import pixiv.bean.Picture;
import utils.ConnectionUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsoupPixivUser1 {
    public static Integer userId=673438;
    public static Integer lastPictureId=0;
    public static String userName=null;
    /**
     * 截取url
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
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
            is = connection.getInputStream();
            inputStreamReader = new InputStreamReader(is, code);
            bis = new BufferedReader(inputStreamReader);
            String line = null;
            StringBuffer result = new StringBuffer();
            while ((line = bis.readLine()) != null) {
                result.append(line);
            }
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
    //获取图片ajax请求链接
    public static String getAjaxUrl(List<Picture> pictureList){
        StringBuilder url= new StringBuilder("https://www.pixiv.net/ajax/user/" + userId + "/profile/illusts?");
        for (Picture picture : pictureList) {
            url.append("ids[]=").append(picture.getId()).append("&");
        }
        url.append("work_category=illustManga&is_first_page=1&lang=zh");
        return new String(url);
    }

    /**
     * 获取完整图片信息
     * @param ajaxUrl
     * @param pictures
     * @return
     * @throws Exception
     */
    public static List<Picture> getPictureList(String ajaxUrl,List<Picture> pictures) throws Exception{
        //根据ajax链接获取图片的详细信息
        URLConnection urlConnection = new URL(ajaxUrl).openConnection();
        urlConnection.setConnectTimeout(20000);
        urlConnection.setReadTimeout(20000);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = null;
        StringBuffer result = new StringBuffer();
        while ((line = bufferedReader.readLine()) != null) {
            result.append(line);
        }

        //将获取到图片信息封装到对象中：id，标题，src，该链接下的图片总数，作者
        JSONObject jsonObject = JSONUtil.parseObj(result);
        Object body = jsonObject.get("body");
        jsonObject = JSONUtil.parseObj(body);
        Object works = jsonObject.get("works");
        jsonObject = JSONUtil.parseObj(works);
        Object o = jsonObject.get(pictures.get(0).getId());
        for (int i = 0; i < pictures.size(); i++) {
            Object pictureInfo = jsonObject.get(pictures.get(i).getId());
            JSONObject tempJsonObject = JSONUtil.parseObj(pictureInfo);
            String title = (String) tempJsonObject.get("title");
            title = title.replace(":","-").replace("/","").replace("\\","");
            pictures.get(i).setTitle(title);
            pictures.get(i).setPageCount((Integer) tempJsonObject.get("pageCount"));
            String realSrc = getRealSrc((String) tempJsonObject.get("url"));
            pictures.get(i).setSrc(realSrc);
            pictures.get(i).setUserName((String) tempJsonObject.get("userName"));
        }
        userName=pictures.get(0).getUserName().replace(":"," ");
        return pictures;
    }

    /**
     * 获取高清图片链接
     * @param src
     * @return
     */
    public static String getRealSrc(String src) {
        //https://i.pximg.net/img-original/img/2022/07/06/00/13/07/99529275_p0.jpg
        //https://i.pximg.net/c/250x250_80_a2/img-master/img/2022/07/06/00/13/07/99529275_p0_square1200.jpg

        src = src.replace("c/250x250_80_a2/","").replace("_square1200","");
        //https://i.pximg.net/img-master/img/2022/07/06/00/13/07/99529275_p0.jpg
        src = src.replace("_custom1200","").replace("custom-thumb","img-original");
        return src.replace("img-master","img-original");

    }

    /**
     * 下载图片
     * @param picture
     */
    public static void downloadFile(Picture picture){
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = new URL(picture.getSrc()).openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(20000);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
            urlConnection.setRequestProperty("Referer","https://www.pixiv.net/");
            inputStream = urlConnection.getInputStream();
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
                urlConnection = new URL(picture.getSrc()).openConnection();
                urlConnection.setConnectTimeout(20000);
                urlConnection.setReadTimeout(20000);
                urlConnection.setUseCaches(false);
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
                urlConnection.setRequestProperty("Referer","https://www.pixiv.net/");
                inputStream = urlConnection.getInputStream();
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }

        try {
            String dirName="E:\\图片\\pixiv\\"+userName+"\\";
            File file = new File(dirName);
            if (!file.exists())
                file.mkdirs();
            String fileName=dirName + picture.getId() + "_p" + picture.getPageCount() + "_" + picture.getTitle()+".png";
            System.out.println(fileName);
            FileOutputStream outputStream = new FileOutputStream(fileName);
            System.out.println(picture.getSrc());
            int temp=0;
            while ((temp=inputStream.read())!=-1){
                outputStream.write(temp);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 下载该画师的所有图片
     * @param pictureList
     * @param offset
     */
    public static void download(List<Picture> pictureList,Integer offset){
        for (int i = 0; i < pictureList.size(); i++) {
            Integer pageCount = pictureList.get(i).getPageCount();
            if (pageCount>1){
                Picture picture = pictureList.get(i);
                for (int j = 1; j <= pageCount; j++) {
                    if (j!=1){
                        String replaceSrc = picture.getSrc().replace("_p" + (j - 2), "_p" + (j-1));
                        picture.setSrc(replaceSrc);
                    }
                    picture.setPageCount(j);
                    System.out.println("第"+(i+1+offset)+"组图片，第"+(j)+"张图片开始下载");
                    downloadFile(picture);
                    System.out.println("第"+(i+1+offset)+"组图片，第"+(j)+"张图片完成");
                }
            }else {
                System.out.println("第"+(i+1+offset)+"张图片开始下载");
                downloadFile(pictureList.get(i));
                System.out.println("第"+(i+1+offset)+"张图片完成");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        //开启代理
        String proxyHost = "127.0.0.1";
        String proxyPort = "7890";

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);

        String url="https://www.pixiv.net/ajax/user/"+userId+"/profile/all";
        //获取所有id列表

        String ids = fetchUrl(url, null);
        List<Picture> pictureList = initPictureList(ids);

        long start = System.currentTimeMillis();
        if (pictureList.size()>80){
            Integer count= Math.toIntExact(Math.round(Math.ceil(pictureList.size()/80.0)));
            int offset=0;
            for (int i = 0; i < count; i++,offset+=80) {
                List<Picture> tempPictureList = new ArrayList<>();
                for (int j = 0; j < 80; j++) {
                    if ((j+offset)<pictureList.size()){
                        tempPictureList.add(pictureList.get(j+offset));
                    }else {
                        break;
                    }
                }
                String ajaxUrl = getAjaxUrl(tempPictureList);
                tempPictureList  = getPictureList(ajaxUrl, tempPictureList);
                download(tempPictureList,offset);
            }
        }else {
            String ajaxUrl = getAjaxUrl(pictureList);
            pictureList = getPictureList(ajaxUrl,pictureList);
            download(pictureList,0);
        }

        long end = System.currentTimeMillis();
        System.out.println("总需要下载"+pictureList.size()+"份图片");
        System.out.println("下载完成，耗时："+(end-start)/1000+"秒");

    }
}

package pixiv.URLConnection;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import pixiv.bean.Picture;
import pixiv.utils.PixivUtils;
import pixiv.utils.URLConnectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public class PixivPicture {
    private String userId="27517";
    private String pictureId="20830181";
    private Picture picture;
    private String downloadDirName = "E:\\图片\\pixiv\\";

    public PixivPicture(String userId, String pictureId, String downloadDirName) {
        this.userId = userId;
        this.pictureId = pictureId;
        this.downloadDirName = downloadDirName;
        String ajaxURL = getAjaxURL(userId,pictureId);
        try {
            this.picture=getPicture(ajaxURL,pictureId);
        } catch (IOException e) {
            System.out.println("获取图片基础信息失败");
            e.printStackTrace();
        }
    }

    public PixivPicture(Picture picture,String downloadDirName) {
        Integer pageCount = picture.getPageCount();
        // 如果是图片组，需要重新下载该组图片
        if (pageCount>1){
            String src = picture.getSrc();
            String[] split = src.split("_p");
            StringBuilder stringBuilder = new StringBuilder(split[0]).append("_p0");
            split = split[1].split("\\.");
            stringBuilder.append(".").append(split[1]);
            picture.setSrc(new String(stringBuilder));
        }
        this.userId=picture.getUserId();
        this.pictureId=picture.getId();
        this.picture = picture;
        this.downloadDirName = downloadDirName;
    }

    public String getAjaxURL(String userId,String pictureId){
        StringBuilder url= new StringBuilder("https://www.pixiv.net/ajax/user/" + userId + "/profile/illusts?");
        url.append("ids[]=").append(pictureId).append("&");
        url.append("work_category=illustManga&is_first_page=1&lang=zh");
        return new String(url);
    }
    public Picture getPicture(String ajaxUrl,String pictureId) throws IOException {
        URLConnection urlConnection = URLConnectionUtils.getURLConnection(ajaxUrl);
        InputStream inputStream = URLConnectionUtils.getInputStream(urlConnection);
        if (inputStream!=null){
            String result = PixivUtils.getJsonResult(inputStream);
            //将获取到图片信息封装到对象中：id，标题，src，该链接下的图片总数，作者
            JSONObject jsonObject = JSONUtil.parseObj(result);
            Object body = jsonObject.get("body");
            jsonObject = JSONUtil.parseObj(body);
            Object works = jsonObject.get("works");
            jsonObject = JSONUtil.parseObj(works);
            Object pictureInfo = jsonObject.get(pictureId);

            jsonObject = JSONUtil.parseObj(pictureInfo);
            String userId = (String) jsonObject.get("userId");
            String title = (String) jsonObject.get("title");
            title=PixivUtils.checkTitle(title);
            String url = PixivUtils.getRealSrc((String) jsonObject.get("url"));
            String userName = (String) jsonObject.get("userName");
            Integer pageCount = (Integer) jsonObject.get("pageCount");

            return new Picture(title,pictureId,url,pageCount,userId,userName);
        }
        return null;
    }
    public boolean download(){
        Integer pageCount = picture.getPageCount();
        if (pageCount>1){
            boolean flag = true;
            for (int i = 1; i <= pageCount; i++) {
                if (i!=1){
                    String replaceSrc = picture.getSrc().replace("_p" + (i - 2), "_p" + (i - 1));
                    picture.setSrc(replaceSrc);
                }
                picture.setPageCount(i);
                if (!downloadFile(picture)){
                    flag=false;
                }
            }
            return flag;
        }else {
            return downloadFile(picture);
        }

    }
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
                    return false;
                }
            }
        }
        String dirName=downloadDirName+PixivUtils.checkTitle(picture.getUserName())+"-"+ picture.getUserId()+"\\";
        return PixivUtils.downloadPicture(dirName, picture, inputStream);
    }
}

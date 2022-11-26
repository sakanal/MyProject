package pixiv;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import pixiv.bean.Picture;
import utils.PixivUtils;
import utils.URLConnectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;

public class URLConnectionPixivPicture {
    private static final String proxyHost = "127.0.0.1";
    private static final String proxyPort = "7890";

    private static final Integer userId=51198160;
    private static final String pictureId="91354871";
    private static final String downloadDirName = "E:\\图片\\pixiv\\";

    public static void main(String[] args){
        //开启代理
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);

        ArrayList<String> failAjaxUrl = new ArrayList<>();

        String ajaxUrl = getAjaxUrl(pictureId);
        try {
            Picture picture = getPicture(ajaxUrl,pictureId);
            if (picture!=null){
                download(picture);
            }
        } catch (IOException e) {
            failAjaxUrl.add(ajaxUrl);
            e.printStackTrace();
        }
        if (failAjaxUrl.size()>0){
            failAjaxUrl.forEach(System.out::println);
        }
        System.out.println("下载完成");


    }

    public static String getAjaxUrl(String pictureId){
        StringBuilder url= new StringBuilder("https://www.pixiv.net/ajax/user/" + userId + "/profile/illusts?");
        url.append("ids[]=").append(pictureId).append("&");
        url.append("work_category=illustManga&is_first_page=1&lang=zh");
        return new String(url);
    }
    public static Picture getPicture(String ajaxUrl,String pictureId) throws IOException {
        URLConnection urlConnection = URLConnectionUtils.getURLConnection(ajaxUrl);
        InputStream inputStream = URLConnectionUtils.getInputStream(urlConnection);
        if (inputStream!=null){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            //将获取到图片信息封装到对象中：id，标题，src，该链接下的图片总数，作者
            JSONObject jsonObject = JSONUtil.parseObj(result);
            Object body = jsonObject.get("body");
            jsonObject = JSONUtil.parseObj(body);
            Object works = jsonObject.get("works");
            jsonObject = JSONUtil.parseObj(works);
            Object pictureInfo = jsonObject.get(pictureId);
            jsonObject = JSONUtil.parseObj(pictureInfo);
            String title = (String) jsonObject.get("title");
            title=PixivUtils.checkTitle(title);
            String url = PixivUtils.getRealSrc((String) jsonObject.get("url"));
            String userName = (String) jsonObject.get("userName");
            Integer pageCount = (Integer) jsonObject.get("pageCount");
            return new Picture(title,pictureId,url,pageCount,userName);
        }
        return null;
    }
    public static void download(Picture picture){
        URLConnection urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
        InputStream inputStream = null;
        try {
            inputStream = URLConnectionUtils.getInputStream(urlConnection);
        } catch (IOException e) {
            String src = picture.getSrc();
            String[] split = src.split("\\.");
            String imageFormat = split[split.length - 1];
            if ("jpg".equals(imageFormat)){
                System.out.println("改为png");
                picture.setSrc(src.replace("jpg","png"));
            }else {
                System.out.println("改为jpg");
                picture.setSrc(src.replace("png","jpg"));
            }
            urlConnection = URLConnectionUtils.getURLConnection(picture.getSrc());
            try {
                inputStream  = URLConnectionUtils.getInputStream(urlConnection);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        String dirName=downloadDirName+picture.getUserName()+"\\";
        PixivUtils.downloadPicture(dirName,picture,inputStream);

    }
}

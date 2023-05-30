package pixiv;

import pixiv.URLConnection.PixivFollowing;
import pixiv.URLConnection.PixivPicture;
import pixiv.URLConnection.PixivUser;
import pixiv.URLConnection.UpdateUser;

import java.io.FileNotFoundException;

public class application {
    private static final String proxyHost = "127.0.0.1";
    private static final String proxyPort = "7890";
    static {
        //开启代理
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
    }
    //登录用户id
    private static final Integer myUserId=20225328;
    //画师id
    private static final String userId="3975343";
    //画作id
    private static final String pictureId="106031367";
    //画师id组
    private static final Integer[] userIdArray={10495320,22124330,31147264,792198};
    //下载地址
    private static final String simpleDownloadDirName="E:\\图片\\pixiv\\";

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

//        update();

//        downloadByUser(userId);
//        downloadByUser(userIdArray);
        downloadByPictureId(userId,pictureId);
//        downloadByFollowing();



        long end = System.currentTimeMillis();
        System.out.println("总共耗时"+((end-start)/1000)+"秒");
    }

    public static void update() throws Exception {
        UpdateUser updateUser = null;
        try {
            updateUser = new UpdateUser(simpleDownloadDirName);
        } catch (FileNotFoundException e) {
            System.out.println("当前路径下没有画师图片文件夹");
        }
        if (updateUser!=null){
            updateUser.download();
        }

    }

    public static void downloadByPictureId(String userId,String pictureId) {
        new PixivPicture(userId,pictureId,simpleDownloadDirName).download();
    }
    public static void downloadByUser(String userId) throws Exception {
        new PixivUser(userId,simpleDownloadDirName).download();
        if (PixivUser.failPictureList.size()>0){
            PixivUser.downloadFailPicture(simpleDownloadDirName);
        }
    }
    public static void downloadByUser(Integer[] userIdArray) throws Exception{
        for (Integer userId : userIdArray) {
            new PixivUser(String.valueOf(userId),simpleDownloadDirName).download();
        }
        if (PixivUser.failPictureList.size()>0){
            PixivUser.downloadFailPicture(simpleDownloadDirName);
        }
    }
    public static void downloadByFollowing() throws Exception{
        new PixivFollowing(myUserId,simpleDownloadDirName).download();
        if (PixivUser.failPictureList.size()>0){
            PixivUser.downloadFailPicture(simpleDownloadDirName);
        }
    }
}

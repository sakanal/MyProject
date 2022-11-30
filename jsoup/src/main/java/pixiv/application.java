package pixiv;

import pixiv.URLConnection.PixivFollowing;
import pixiv.URLConnection.PixivPicture;
import pixiv.URLConnection.PixivUser;
import pixiv.URLConnection.UpdataUser;

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
    private static final String userId="8189060";
    //画作id
    private static final String pictureId="78468459";
    //下载地址
    private static final String simpleDownloadDirName="E:\\图片\\pixiv\\";

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        updata();
//        downloadByUser(userId);
//        Integer[] userIdArray ={212801,2188232,1113943,2131660,1218472,1039353,8189060,490219,11729,480448};
//        downloadByUser(userIdArray);
//        downloadByPictureId(userId,pictureId);
//        downloadByFollowing();



        long end = System.currentTimeMillis();
        System.out.println("总共耗时"+((end-start)/1000)+"秒");
    }

    public static void updata() throws Exception {
        UpdataUser updataUser = null;
        try {
            updataUser = new UpdataUser(simpleDownloadDirName);
        } catch (FileNotFoundException e) {
            System.out.println("当前路径下没有画师图片文件夹");
        }
        if (updataUser!=null){
            updataUser.download();
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

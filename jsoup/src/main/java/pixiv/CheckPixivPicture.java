package pixiv;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import pixiv.bean.Picture;
import utils.PixivUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CheckPixivPicture {
    private static String SimpleDirName = "E:\\图片\\pixiv\\";
    public static void getRealPictureList(String pictureInfo,List<Picture> pictureList){
        List<Picture> myPictureList = getMyPictureList(pictureInfo);
        if (myPictureList!=null && myPictureList.size()>0){
            pictureList.removeAll(myPictureList);
        }
    }
    public static List<Picture> getMyPictureList(String pictureInfo) {
        //获取画家名称
        String userName = getUserName(pictureInfo);
        SimpleDirName+=userName;
        //获取本地中该画家的所有图片id
        List<String> pictureNameList = getPictureName();
        if (pictureNameList!=null){
            //图片id转型
            List<Picture> pictureList = getPictureList(pictureNameList);
            if (pictureList.size()>0){
                return pictureList;
            }
        }
        return null;
    }
    //获取画家名称
    public static String getUserName(String pictureInfo){
        Object body = JSONUtil.parseObj(pictureInfo).get("body");
        Object works = JSONUtil.parseObj(body).get("works");
        JSONObject jsonObject = JSONUtil.parseObj(works);
        Set<String> keySet = jsonObject.keySet();
        String userName = null;
        for (String key : keySet) {
            Object info = jsonObject.get(key);
            userName = (String) JSONUtil.parseObj(info).get("userName");
        }
        return PixivUtils.checkTitle(userName);
    }
    //获取本地中该画家的所有图片id
    public static List<String> getPictureName(){
        File folder = new File(SimpleDirName);
        if (!folder.exists())
            return null;
        File[] files = folder.listFiles();
        ArrayList<String> list = new ArrayList<>();
        if (files!=null){
            for (File file : files) {
                String name = file.getName();
                list.add(name);
            }
            return list;
        }else {
            return null;
        }
    }
    //图片id转型
    public static List<Picture> getPictureList(List<String> pictureNameList){
        ArrayList<Picture> pictureList = new ArrayList<>();
        for (String pictureName : pictureNameList) {
            String[] split = pictureName.split("_");
            pictureList.add(new Picture(split[0]));
        }
        return pictureList;
    }
}

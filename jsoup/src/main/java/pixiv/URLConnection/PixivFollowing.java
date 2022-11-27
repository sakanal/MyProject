package pixiv.URLConnection;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import pixiv.bean.User;
import utils.PixivUtils;
import utils.URLConnectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;

public class PixivFollowing {
    //个人用户id
    private final Integer myUserId;
    //下载前缀
    private final String simpleDownloadDirName;
    //当前页的所有用户id及用户名
    private List<User> userList;
    //总数
    private Integer total;
    //页码容量
    private final Integer limit = 24;
    //偏移量
    private Integer offset=0;
    public PixivFollowing(Integer myUserId,String simpleDownloadDirName){
        this.myUserId=myUserId;
        this.simpleDownloadDirName=simpleDownloadDirName;
        getTotalFollowing();
//        getUserList();
    }
    public void getTotalFollowing(){
        //https://www.pixiv.net/ajax/user/extra?lang=zh
        try {
            URLConnection urlConnection = URLConnectionUtils.getURLConnection("https://www.pixiv.net/ajax/user/extra?lang=zh");
            InputStream inputStream = URLConnectionUtils.getInputStream(urlConnection);
            String result = PixivUtils.getJsonResult(inputStream);
            Object body = JSONUtil.parseObj(result).get("body");
            this.total= (Integer) JSONUtil.parseObj(body).get("following");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void getUserList(){
        //https://www.pixiv.net/ajax/user/20225328/following?offset=0&limit=24&rest=show&tag=&acceptingRequests=0&lang=zh
        String followingURL="https://www.pixiv.net/ajax/user/" + myUserId +
                "/following?offset=" + offset +
                "&limit=" + limit +
                "&rest=show&tag=&acceptingRequests=0&lang=zh";
        URLConnection urlConnection = URLConnectionUtils.getURLConnection(followingURL);
        try {
            InputStream inputStream = URLConnectionUtils.getInputStream(urlConnection);
            String result = PixivUtils.getJsonResult(inputStream);

            Object body = JSONUtil.parseObj(result).get("body");
            Object users = JSONUtil.parseObj(body).get("users");

            JSONArray jsonArray = JSONUtil.parseArray(users);

            this.userList= JSONUtil.toList(jsonArray, User.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void download() throws Exception {
        int page=(int) Math.ceil((double) total/limit);
        for (int i = 0; i < page; i++) {
            offset=limit*i;
            getUserList();
            downloadByUser();
        }
    }
    public void downloadByUser() throws Exception {
        for (User user : userList) {
            new PixivUser(user.getUserId(),simpleDownloadDirName).download();
        }
    }
}

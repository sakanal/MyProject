package pixiv.URLConnection;

import pixiv.bean.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class UpdataUser {
    private final String simpleDirName;
    private final List<String> myUserIdList;
    public UpdataUser(String simpleDirName) throws FileNotFoundException {
        this.simpleDirName=simpleDirName;
        myUserIdList = getMyUserIdList();
    }

    private List<String> getMyUserIdList() throws FileNotFoundException {
        File simpleFile = new File(simpleDirName);
        if (simpleFile.exists()){
            File[] files = simpleFile.listFiles();
            if (files!=null && files.length>0){
                List<String> list = new ArrayList<>();
                for (File file : files) {
                    String[] split = file.getName().split("-");
                    list.add(split[split.length-1]);
                }
                return list;
            }else {
                throw new FileNotFoundException();
            }
        }else {
            throw new FileNotFoundException();
        }
    }

    public void download() throws Exception{
        for (String userId : myUserIdList) {
            new PixivUser(userId,simpleDirName).download();
        }
        if (PixivUser.failPictureList.size()>0){
            PixivUser.downloadFailPicture(simpleDirName);
        }
    }
}

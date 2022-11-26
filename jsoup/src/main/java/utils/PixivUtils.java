package utils;

import pixiv.bean.Picture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PixivUtils {

    /**
     * 获取高清图片链接
     * @param src 原始图片连接
     * @return 高清图片连接
     */
    public static String getRealSrc(String src) {
        //https://i.pximg.net/img-original/img/2022/07/06/00/13/07/99529275_p0.jpg
        //https://i.pximg.net/c/250x250_80_a2/img-master/img/2022/07/06/00/13/07/99529275_p0_square1200.jpg

        src = src.replace("c/250x250_80_a2/","").replace("_square1200","");
        //https://i.pximg.net/img-master/img/2022/07/06/00/13/07/99529275_p0.jpg
        src = src.replace("_custom1200","").replace("custom-thumb","img-original");
        return src.replace("img-master","img-original");

    }

    //       \/:*?"<>|
    public static String checkTitle(String title){
        if (title!=null){
            return title.replace("/","·")
                    .replace("\\","·")
                    .replace(":","·")
                    .replace("*","·")
                    .replace("?","·")
                    .replace("\"","·")
                    .replace("<","·")
                    .replace(">","·")
                    .replace("|","·");
        }else {
            return null;
        }
    }


    public static boolean downloadPicture(String dirName, Picture picture, InputStream inputStream){
        try {
            File file = new File(dirName);
            if (!file.exists())
                file.mkdirs();
            String fileName=dirName + picture.getId() + "_p" + picture.getPageCount() + "_" + picture.getTitle()+".png";
            System.out.println(fileName);
            FileOutputStream outputStream = new FileOutputStream(fileName);
            System.out.println(picture.getSrc());
            int temp;
            while ((temp=inputStream.read())!=-1){
                outputStream.write(temp);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

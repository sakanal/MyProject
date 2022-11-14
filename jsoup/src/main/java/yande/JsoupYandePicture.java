package yande;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class JsoupYandePicture {
    public static Integer id=701881;
    public static String tags="ijac_ray";
    public static Integer page=2;
    public static String simpleUrl="https://yande.re/post/show/";
    public static void main(String[] args) throws Exception {
        //开启代理
        String proxyHost = "127.0.0.1";
        String proxyPort = "7890";
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);

        Document document = Jsoup.parse(new URL(simpleUrl + id), 10 * 1000);
        Element elementById = document.getElementById("png");
        if (elementById==null){
            elementById = document.getElementById("highres");
        }
        String href = elementById.attr("href");

        InputStream inputStream = new URL(href).openConnection().getInputStream();
        String dirName="E:\\图片\\yande\\"+tags+"\\"+page+"\\";
        String fileName=id+".png";
        File file = new File(dirName);
        if (!file.exists())
            file.mkdirs();
        FileOutputStream outputStream = new FileOutputStream(dirName + fileName);
        int temp=0;
        System.out.println("开始下载");
        while ((temp=inputStream.read())!=-1){
            outputStream.write(temp);
        }
        System.out.println("完成");
        inputStream.close();
        outputStream.close();
    }
}

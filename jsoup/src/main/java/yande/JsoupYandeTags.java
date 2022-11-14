package yande;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import yande.bean.Picture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class JsoupYandeTags {
    public static String tags="hiki_niito";
    public static Integer page=1;
    public static Integer total=1;
    public static String simpleUrl="https://yande.re";
    public static Integer lastPictureId=0;
    public static Boolean isFist=true;

    //获取总页数
    public static void getTotal(Document document){
        Elements pagination = document.getElementsByClass("pagination");
        if (pagination.size()!=0){
            Elements a = pagination.get(0).getElementsByTag("a");
            total = Integer.valueOf(a.get(a.size() - 2).text());
        }
    }
    //获取某一页的所有图片详细页面的href
    public static List<Picture> initPictureList(Document document){
        Elements li = document.getElementById("post-list-posts").getElementsByTag("li");
        List<Picture> pictureList = new ArrayList<>();
        for (Element element:li){
            String id = element.attr("id").replace("p","");
            if (Integer.valueOf(id).equals(lastPictureId)){
                isFist=false;
                break;
            }
            String attr = element.getElementsByTag("a").get(0).attr("href");
            pictureList.add(new Picture(id,simpleUrl+attr));
        }
        return pictureList;
    }
    public static void download(Picture picture,int j)throws IOException {
        j++;
        Document documentImage = Jsoup.parse(new URL(picture.getHref()), 10 * 1000);
        Element elementById = documentImage.getElementById("png");
        if (elementById==null){
            elementById = documentImage.getElementById("highres");
        }
        String href = elementById.attr("href");

        InputStream inputStream = new URL(href).openConnection().getInputStream();
        String dirName="E:\\图片\\yande\\"+tags+"\\"+page+"\\";
        String fileName=picture.getId()+".png";
        File file = new File(dirName);
        if (!file.exists())
            file.mkdirs();
        FileOutputStream outputStream = new FileOutputStream(dirName + fileName);
        int temp=0;
        System.out.println("第"+page+"页，第"+j+"张图片开始下载");
        while ((temp=inputStream.read())!=-1){
            outputStream.write(temp);
        }
        System.out.println("第"+page+"页，第"+j+"张图片完成");
        inputStream.close();
        outputStream.close();
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

        String url="https://yande.re/post?page="+page+"&tags="+tags;

        Document document =  Jsoup.parse(new URL(url), 10 * 1000);
        getTotal(document);

        long start = System.currentTimeMillis();
        for (int i = (page-1); i < total; i++,page++) {
            url="https://yande.re/post?page="+page+"&tags="+tags;
            document =  Jsoup.parse(new URL(url), 10 * 1000);
            List<Picture> pictureList = initPictureList(document);
            for (int j = 0; j < pictureList.size(); j++) {
                download(pictureList.get(j),j);
            }
            if (!isFist)
                break;
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时："+(end-start)/1000+"秒");

    }
}

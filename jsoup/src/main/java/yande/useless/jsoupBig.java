package yande.useless;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

//条件下载，普通图片
public class jsoupBig {

    public static void main(String[] args) throws Exception {
        //开启代理
        String proxyHost = "127.0.0.1";
        String proxyPort = "7890";

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);

        int page=1;
        String tags="myabit";
        String url="https://yande.re/post?page="+page+"&tags="+tags;

        Document document =  Jsoup.parse(new URL(url), 10 * 1000);

        System.out.println("first link success");
        Element elementById = document.getElementById("post-list-posts");
        Elements li = elementById.getElementsByTag("li");
        int i=0;
        for (Element value : li) {
            //手动防止重复下载
            if (i<8){
                System.out.println("第"+(i+1)+"张图片已下载");
                i++;
                continue;
            }


            Element element = value.getElementsByTag("div").get(0).getElementsByTag("a").get(0);
            if (element != null) {
//                System.out.println(element);
                //截取图片的详细信息
                String href = element.attr("href");
                String realUrl = "https://yande.re" + href;

                //获取图片id，为图片名
                String[] split = href.split("/");
                String num = split[split.length - 1];

                //进入图片详细信息页
                Document realDocument = Jsoup.parse(new URL(realUrl), 10 * 1000);

                System.out.println("link success");
//                Element realElement = realDocument.getElementById("image");

                Element realElement = realDocument.getElementById("png");
                if (realElement == null) {
                    realElement = realDocument.getElementById("highres");
                }

                //获取图片网络地址
                String realHref = realElement.attr("href");

                InputStream inputStream = new URL(realHref).openConnection().getInputStream();
                File file = new File("D:\\yande\\"+tags);
                if (!file.exists()){
                    //创建文件夹
                    file.mkdirs();
                }
                OutputStream outputStream = new FileOutputStream("D:\\yande\\"+tags+"\\" + num + ".png");

                int temp = 0;
                System.out.println("开始下载："+num);
                while ((temp = inputStream.read()) != -1) {
                    outputStream.write(temp);
                }
                i++;
                System.out.println("第"+i+"张图片下载完毕");

                inputStream.close();
                outputStream.close();
            }

        }

    }
}
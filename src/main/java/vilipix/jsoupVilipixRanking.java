package vilipix;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;

public class jsoupVilipixRanking {
    static String day="20220823";
    static String mode="monthly";
    static Integer page=1;
    static String realUrl="https://www.vilipix.com";

    public static void getPages(Document document) throws Exception {
        Elements elements = document.getElementsByClass("el-pager").get(0).getElementsByTag("li");
        Element element = elements.get(elements.size() - 1);
        page = Integer.valueOf(element.text());
    }

    public static Document getDocument(String href,int timeoutMillis){
        try {
            //参数转义就好了，keys = URLEncoder.encode(keys, StandardCharsets.UTF_8)
            //new URL(href+keys)
            return Jsoup.parse(new URL(href),timeoutMillis);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String getImageUrl(String src){
        //获取图片网络地址
        //https://img9.vilipix.com/picture/pages/regular/2022/07/06/13/100645288_p0_master1200.jpg?x-oss-process=image/resize,m_fill,w_1000
        String[] split = src.split("\\?");
        //https://img9.vilipix.com/picture/pages/regular/2022/07/06/13/100645288_p0_master1200.jpg
        src = split[0];
        //获取大图片的网络地址
        //https://img9.vilipix.com/picture/pages/original/2022/07/06/13/100645288_p0.png
        //            System.out.println(src);
        return src.replace("regular", "original").replace("_master1200", "").replace("jpg", "png");
    }
    public static String getImageName(String src){
        //https://img9.vilipix.com/picture/pages/original/2022/07/06/13/100645288_p0.png
        String[] split = src.split("/");
        return split[split.length-1];
    }
    public static void download(String href,Integer nowPage){
        //获取图片详细信息页面的链接
        href = realUrl + href;
        Document document = getDocument(href, 10 * 1000);
        assert document != null;

        Elements elements = document.getElementsByClass("illust-pages").get(0).getElementsByTag("img");
        for (Element element : elements) {
            String src = element.attr("src");
            String imageUrl = getImageUrl(src);
            //获取图片唯一id
            String imageName = getImageName(imageUrl);

            InputStream inputStream = null;
            //最终大图片的格式可能为png或jpg
            try {
                //自定义默认为png
                inputStream = new URL(imageUrl).openConnection().getInputStream();
            } catch (IOException e) {
                //如果运行出异常就改为jpg格式
                imageUrl = imageUrl.replace("png", "jpg");
                try {
                    inputStream = new URL(imageUrl).openConnection().getInputStream();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            assert inputStream != null;
            OutputStream outputStream = null;
            try {
//                String dirName="E:\\图片\\vilipix\\" + day + "\\" + page+"\\";
                String dirName = "E:\\图片\\vilipix\\ranking\\" + mode + "\\" + day + "\\" + nowPage + "\\";
                File file = new File(dirName);
                if (!file.exists()) {
                    if (file.mkdirs()) {
                        System.out.println("新建文件成功");
                    }
                }
                outputStream = new FileOutputStream(dirName + imageName);
                int temp = 0;
                System.out.println("开始下载");
                while ((temp = inputStream.read()) != -1) {
                    outputStream.write(temp);
                }
                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) throws Exception {
        for (int i=0;i<page; i++) {
            String url="https://www.vilipix.com/ranking?date="+day+"&mode="+mode+"&p="+(i+1);
            Document document = getDocument(url, 10 * 1000);
            assert document != null;
            getPages(document);
            Elements elementsByClass = document.getElementsByClass("illust-content");
            //获取当前页面的所有图片列表
            Elements li = elementsByClass.get(0).getElementsByTag("li");

            for (int j = 0; j < li.size(); j++) {
                String href = li.get(j).getElementsByTag("a").attr("href");
                //传递图片详细页面的链接
                download(href,(i+1));
                System.out.println("第"+(i+1)+"页，第"+(j+1)+"组图片下载完成");
//                System.out.println("href = " + href);
            }
        }
    }
}

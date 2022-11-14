package vilipix;

import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Scanner;

public class jsoupVIlipixUserAll {
    public static Integer author_user_id=0;
    public static String author_user_name="";
    public static Integer offset=0;
    public static Integer pages=1;
    public static String realHref="https://www.vilipix.com/";
    //https://www.vilipix.com/user/400684

    /**
     * 截取url
     * @param url   可以获取到图片详细信息的Ajax链接
     * @param code  默认为UTF-8
     * @return      返回ajax请求结果
     */
    public static String fetchUrl(String url,String code) throws IOException {
        if (code==null)
            code="UTF-8";
        BufferedReader bis = null;
        InputStream is = null;
        InputStreamReader inputStreamReader = null;
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
            is = connection.getInputStream();
            inputStreamReader = new InputStreamReader(is, code);
            bis = new BufferedReader(inputStreamReader);
            String line = null;
            StringBuffer result = new StringBuffer();
            while ((line = bis.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } finally {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 转义json，将json字符串转成list列表
     * @param fetchUrl  通过ajax获取到的所有图片信息
     * @return          图片信息列表
     */
    public static List<Dict> getJson(String fetchUrl){
        JSONObject jsonObject = JSONUtil.parseObj(fetchUrl);
        Object code = jsonObject.get("code");
        Object data = jsonObject.get("data");
//        System.out.println("code = " + code);
//        System.out.println("data = " + data);
        jsonObject = JSONUtil.parseObj(data);
        Integer count = jsonObject.getInt("count");
        pages= Math.toIntExact(Math.round(Math.ceil(count / 30.0)));
        Object rows = jsonObject.get("rows");
//        System.out.println("count = " + count);
//        System.out.println("rows = " + rows);
        JSONArray jsonArray = JSONUtil.parseArray(rows);
//        System.out.println("jsonArray.size() = " + jsonArray.size());
        return JSONUtil.toList(jsonArray, Dict.class);
    }
    //https://img9.vilipix.com/picture/pages/regular/2022/05/31/20/27/98742880_p0_master1200.jpg?x-oss-process=image/resize,m_fill,w_1000
    public static String getRealSrc(String src){
        src = src.split("\\?")[0];
        src = src.replace("regular","original").replace("_master1200","").replace("jpg","png");
        //https://img9.vilipix.com/picture/pages/original/2022/05/31/20/27/98742880_p0.png
        return src;
    }
    //https://img9.vilipix.com/picture/pages/original/2022/05/31/20/27/98742880_p0.png
    public static String getFileName(String src){
        String[] split = src.split("/");
        //98742880_p0.png
        src=split[split.length-1];
        //98742880_p0
        return src.split("\\.")[0];
    }
    public static void downloadFile(String url,String fileName,String title){
        String dirName="E:\\图片\\vilipix\\painter\\"+author_user_name+"\\";
        File file = new File(dirName);
        if (!file.exists())
            file.mkdirs();
        try {
            InputStream inputStream = new URL(url).openConnection().getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(dirName+fileName+"_"+title+".png");
            int temp=0;
            while ((temp=inputStream.read())!=-1){
                fileOutputStream.write(temp);
            }
            inputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void downloadFile(String pictureURL,String title) throws IOException {
        Document document=null;
        try {
            document = Jsoup.parse(new URL(pictureURL), 10 * 1000);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Elements li = document.getElementsByClass("illust-pages").get(0).getElementsByTag("li");
        for (int i = 0; i < li.size(); i++) {
            String src = li.get(i).getElementsByTag("img").get(0).attr("src");
            src = getRealSrc(src);

            String dirName="E:\\图片\\vilipix\\painter\\"+author_user_name+"\\";
            String fileName = getFileName(src);
            File file = new File(dirName);
            if (!file.exists())
                file.mkdirs();
            InputStream inputStream=null;
            try {
                inputStream = new URL(src).openConnection().getInputStream();
            } catch (IOException e) {
                src=src.replace("png","jpg");
                try {
                    inputStream = new URL(src).openConnection().getInputStream();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            try {
                FileOutputStream outputStream = new FileOutputStream(dirName + fileName + "_" + title + ".png");
                int temp=0;
                while ((temp=inputStream.read())!=-1){
                    outputStream.write(temp);
                }
                System.out.println("第"+(i+1)+"张图片下载完成");
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入作者id：");
        author_user_id=scanner.nextInt();

        String userURL="https://www.vilipix.com/user/";
        String ajaxURL="https://www.vilipix.com/api/v1/picture/public?sort=new&type=0&author_user_id="+author_user_id+"&limit=30&offset="+offset;
        String pictureURL="https://www.vilipix.com/illust/";

        Document document = Jsoup.parse(new URL(userURL + author_user_id), 10 * 1000);
        //获取作者名称
        author_user_name=document.getElementsByClass("name").get(0).getElementsByTag("h2").get(0).text();


        for (int j = 0; j < pages; j++,offset+=30) {
            //https://www.vilipix.com/api/v1/picture/public?sort=new&type=0&author_user_id=400684&limit=30&offset=0
            String fetchUrl = fetchUrl(ajaxURL, null);
            List<Dict> dictList = getJson(fetchUrl);
            for (int i = 0; i < dictList.size(); i++) {
                String picture_id = dictList.get(i).getStr("picture_id");
                String title = dictList.get(i).getStr("title");
                Integer page_total = dictList.get(i).getInt("page_total");
                if (page_total>1){
                    System.out.println("第" + (j + 1) + "页，第" + (i + 1) + "组图片开始下载");
//                    downloadFile(author_user_name, original_url, picture_id, title);
                    downloadFile(pictureURL+picture_id,title);
                    System.out.println("第" + (j + 1) + "页，第" + (i + 1) + "组图片完成");
                }else {
                    String original_url = dictList.get(i).getStr("original_url");
                    System.out.println("第" + (j + 1) + "页，第" + (i + 1) + "张图片开始下载");
                    downloadFile(original_url, picture_id, title);
                    System.out.println("第" + (j + 1) + "页，第" + (i + 1) + "张图片完成");
                }
            }
        }
    }
}

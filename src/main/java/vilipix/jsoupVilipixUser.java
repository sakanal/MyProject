package vilipix;

import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class jsoupVilipixUser {
    public static Integer author_user_id=103410;
    public static String author_user_name="スコッティ";
    public static Integer offset=0;
    public static Integer pages=1;
    //https://vilipix.com/api/v1/picture/public?sort=new&type=0&author_user_id=1878082&limit=30&offset=30
    //https://vilipix.com/api/v1/picture/public?sort=new&type=0&author_user_id=1878082&limit=30&offset=60
    //https://vilipix.com/api/v1/picture/public?sort=new&type=0&author_user_id=103410&limit=30&offset=0

    /**
     * 截取url
     * @param url   可以获取到图片详细信息的Ajax链接
     * @param code  默认为UTF-8
     * @return      返回ajax请求结果
     */
    public static String fetchUrl(String url,String code) throws IOException{
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

    /**
     * 下载图片
     * @param author    作者名
     * @param url       大图片的网络url
     * @param fileName  文件名称，图片唯一id
     * @param title     图片标题
     */
    public static void downloadFile(String author,String url,String fileName,String title){
        String dirName="E:\\图片\\vilipix\\painter\\"+author+"\\";
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
    public static void main(String[] args) throws Exception {
        for (int j = 0; j < pages; j++,offset+=30) {
            String url="https://vilipix.com/api/v1/picture/public?sort=new&type=0&author_user_id="+author_user_id+"&limit=30&offset="+offset;
            String fetchUrl = fetchUrl(url, null);
            List<Dict> dictList = getJson(fetchUrl);
            for (int i = 0; i < dictList.size(); i++) {
                String original_url = dictList.get(i).getStr("original_url");
                String picture_id = dictList.get(i).getStr("picture_id");
                String title = dictList.get(i).getStr("title");
                System.out.println("第"+(j+1)+"页，第"+(i+1)+"张图片开始下载");
                downloadFile(author_user_name,original_url,picture_id,title);
                System.out.println("第"+(j+1)+"页，第"+(i+1)+"张图片完成");
            }
        }
    }
}


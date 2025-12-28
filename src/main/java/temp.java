import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class temp {
    public static void main(String[] args) throws Exception {
        //开启代理
        String proxyHost = "127.0.0.1";
        String proxyPort = "7890";

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        // 对https也开启代理
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);


        String url="https://i.pximg.net/img-original/img/2021/03/08/00/12/17/88292471_p0.jpg";
        URLConnection urlConnection = new URL(url).openConnection();
        urlConnection.setRequestProperty("Referer","https://www.pixiv.net/");
        InputStream inputStream = urlConnection.getInputStream();
        System.out.println("link success");
        String dirName="D:\\图片\\pixiv\\simple\\";
        File file = new File(dirName);
        if (!file.exists())
            file.mkdirs();
        FileOutputStream outputStream = new FileOutputStream(dirName+ "test.jpg");
        int temp=0;
        System.out.println("start");
        while ((temp=inputStream.read())!=-1){
            outputStream.write(temp);
        }
        System.out.println("success");
        inputStream.close();
        outputStream.close();
    }
}

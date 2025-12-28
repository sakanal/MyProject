import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Test {
    public static Integer id=830322;
    public static String tags="myabit";
    public static Integer page=3;
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

        String loginURL="https://accounts.pixiv.net/ajax/login?lang=zh";
        URLConnection urlConnection = new URL(loginURL).openConnection();
        urlConnection.setRequestProperty("login_id","104820805@qq.com");
        urlConnection.setRequestProperty("password","qaz360782");

        InputStream inputStream = urlConnection.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuffer result = new StringBuffer();
        String line=null;
        while ((line=bufferedReader.readLine())!=null){
            result.append(line);
        }
        System.out.println(result);
    }
}

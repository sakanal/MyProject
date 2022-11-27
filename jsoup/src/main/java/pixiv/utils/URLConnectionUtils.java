package pixiv.utils;

import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class URLConnectionUtils {
    public static HashMap<String,String> configMap = new HashMap<>();
    static {
        configMap.put("User-Agent","Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
        configMap.put("Referer","https://www.pixiv.net/");
        configMap.put("cookie","first_visit_datetime_pc=2022-07-10+17%3A10%3A05; p_ab_id=6; p_ab_id_2=5; p_ab_d_id=691196442; yuid_b=U0kEWQA; c_type=42; privacy_policy_notification=0; a_type=0; b_type=1; adr_id=zpzn2XhPxtz3Mx1Lh0ZWgJQ7hIbLjZZMc5MpfXJXQRISutsi; _td=79ef159f-2056-4e84-929a-085a14893022; pt_47mvrj9g=uid=UxGCmQql/amsUNcA7zIH-Q&nid=0&vid=3XEo8q-aMkPA3NizfWoqEQ&vn=2&pvn=1&sact=1662007646717&to_flag=0&pl=IZoRf4OCSv6aouL5bhDEAg*pt*1662007646717; QSI_S_ZN_5hF4My7Ad6VNNAi=v:0:0; PHPSESSID=20225328_nnb9sHHhdck51TAduFwa865t1c8JdKrb; device_token=0616c4e29fb20b18b4baf0fff71779ed; privacy_policy_agreement=0; tag_view_ranking=_EOd7bsGyl~0xsDLqCEW6~Lt-oEicbBr~RTJMXD26Ak~ziiAzr_h04~pnCQRVigpy~Ie2c51_4Sp~BSlt10mdnm~tgP8r-gOe_~O2wfZxfonb~RcahSSzeRf~azESOjmQSV~KN7uxuR89w~QaiOjmwQnI~-StjcwdYwv~HY55MqmzzQ~aKhT3n4RHZ~5oPIfUbtd6~sr5scJlaNv~EZQqoW9r8g~jk9IzfjZ6n~y3NlVImyly~MnGbHeuS94~hW_oUTwHGx~faHcYIP1U0~4QveACRzn3~PwDMGzD6xn~VPl-5u9cu6~1TeQXqAyHD~gpglyfLkWs~X_1kwTzaXt~eVxus64GZU~M2vKPRxAge~-98s6o2-Rp~TqiZfKmSCg~T40wdiG5yy~DriUjI1aUj~J8EUeZUBbW~jH0uD88V6F~OUF2gvwPef~_hSAdpN9rx~_GuOetFMMO~-TeGk6mN86~pzZvureUki~2kSJBy_FeR~ePN3h1AXKX~KOnmT1ndWG~CrFcrMFJzz~lBcRAWFuPM~9Gbahmahac~T23401WRgq~uW5495Nhg-~N0yI5Cxu-1~yxJ5qTP-Ch~ryeW-BDpTM~TWrozby2UO~04qbH3MKfd~fSOqxxa1Xl~zyKU3Q5L4C~q303ip6Ui5~O7VsJlSXXG~ea63_dbx7n~i_dZaon0j6~6GYRfMzuPl~Bd2L9ZBE8q~0Sds1vVNKR~K8esoIs2eW~WVrsHleeCL~yvtgjYieK1~BU9SQkS-zU~9SofJuz-DY~gCB7z_XWkp~QniSV1XRdS~JVpW4LXMKc~PHQDP-ccQD~OLOvpsajRs~YXsA4N8tVW~yTFlPOVybE~9ODMAZ0ebV~1Cu1TkXAKa~DpYZ-BAzxm~EUwzYuPRbU~9AiXiObEfr~otWaj1bQDp~wmxKAirQ_H~L58xyNakWW~qZU6LDEucx~dqqWNpq7ul~cFXtS-flQO~HHxwTpn5dx~j3leh4reoN~LmbPyhfNiW~lH5YZxnbfC~kGYw4gQ11Z~qkC-JF_MXY~rm2ttySGVc~ipCE6l0Qss~_wkIquW3VS~pzzjRSV6ZO~bvp7fCUKNH; __cf_bm=.La2tk4Cl46wdhKKxn4tKGMGR60cVVIhBlDlLvdcvww-1669432828-0-AXb/gJlKBpaWtWIRMpwRwSEDKexqFq+1uev4Zn+LIL7hPiMeL6ublM2D2Vjf0TbwL/nNxw/A8TQTIIrooF9tPhBVYm6ktCgrRhougD8aNs0cC1eYIt1WZ0g5ug8c5kYbLTXNYS8a+5A4vK3wo7REAXrGn3PGz2F3NEObUe81VGjnKle0NTEizXbhq4vnDuCqldjk3M500lEV3QK0/x6k+wY=");
    }
    public static URLConnection getURLConnection(String url){
        try {
            URLConnection urlConnection = new URL(url).openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(20000);
            urlConnection.setUseCaches(false);
            return urlConnection;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static InputStream getInputStream(URLConnection connection) throws IOException {
        Set<String> keySet = configMap.keySet();
        for (String key : keySet) {
            String value = configMap.get(key);
            connection.setRequestProperty(key, value);
        }
        return connection.getInputStream();
    }
}

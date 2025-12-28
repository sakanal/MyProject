package com.sakanal.selenium;

import com.sakanal.selenium.config.MyConfigProperties;
import com.sakanal.selenium.service.LiePinService;
import com.sakanal.selenium.utils.LiePinUtil;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.springframework.boot.test.context.SpringBootTest;


import javax.annotation.Resource;
import java.util.*;

@SpringBootTest
class WebApplicationTests {

    @Resource
    private LiePinService leiPinService;
    @Resource
    private MyConfigProperties myConfigProperties;


    @Test
    void isProxy(){
        System.setProperty("webdriver.edge.driver", "D:\\Tools\\edgeDriver\\msedgedriver.exe");
        EdgeOptions edgeOptions = new EdgeOptions();
//        String proxyServer="183.236.232.160:8080";
//        edgeOptions.setProxy(new Proxy().setHttpProxy(proxyServer));
        edgeOptions.addArguments("--remote-allow-origins=*");
//        edgeOptions.addArguments("-headless");
        EdgeDriver driver = new EdgeDriver(edgeOptions);

        driver.get("https://www.baidu.com");
        Object proxy =  driver.getCapabilities().getCapability("proxy");
        if (proxy != null ) {
            System.out.println("代理已启用，地址为：" + proxy);
        } else {
            System.out.println("未使用代理");
        }

        System.out.println(driver.getTitle());

        driver.close();
    }

    @Test
    void testConfigProperties(){
        System.out.println(myConfigProperties.getDownloadDir());
        for (String s : myConfigProperties.getJobResponsibilitiesDescription()) {
            System.out.println(s);
        }
        for (String s : myConfigProperties.getJobQualificationDescription()) {
            System.out.println(s);
        }
        Map<String, String> simpleSearchParams = myConfigProperties.getSimpleSearchParams();
        for (String s : simpleSearchParams.keySet()) {
            System.out.println(s+"="+simpleSearchParams.get(s));
        }
    }

    @Test
    void testGetLeiPinInfo(){
        Map<String, String> params = new HashMap<>();
        params.put("key","前端开发");

        leiPinService.getLeiPinInfo(params);
    }

    @Test
    void testCookie(){
        WebDriver headLessDriver = LiePinUtil.getWebDriver(myConfigProperties);

        System.setProperty(myConfigProperties.getDriverName(),myConfigProperties.getDriverDir());
        EdgeOptions edgeOptions = new EdgeOptions().addArguments("--remote-allow-origins=*");
        EdgeDriver simpleDriver = new EdgeDriver(edgeOptions);

        String url = LiePinUtil.getURL(null, myConfigProperties.getSimpleSearchParams());

        headLessDriver.get(url);
        Set<Cookie> headLessCookie = headLessDriver.manage().getCookies();

        simpleDriver.get(url);
        Set<Cookie> simpleCookie = simpleDriver.manage().getCookies();
        simpleDriver.close();

        HashMap<String, String> headLessMap = new HashMap<>();
        HashMap<String, String> simpleMap = new HashMap<>();

        headLessCookie.forEach(cookie-> headLessMap.put(cookie.getName(),cookie.getValue()));
        simpleCookie.forEach(cookie -> simpleMap.put(cookie.getName(),cookie.getValue()));;

        headLessCookie.forEach(cookie -> {
            String name = cookie.getName();
            String headLessValue = headLessMap.get(name);
            String simpleValue = simpleMap.get(name);
            System.out.println(name+"\t"+headLessValue+"\t"+simpleValue+"\t");
        });
    }

    @Test
    void testException(){
        System.setProperty(myConfigProperties.getDriverName(),myConfigProperties.getDriverDir());
        EdgeOptions edgeOptions = new EdgeOptions().addArguments("--remote-allow-origins=*");
        EdgeDriver webDriver = new EdgeDriver(edgeOptions);
        HashMap<String, String> map = new HashMap<>();
        map.put("key","java");
        map.put("pubTime","1");
        map.put("workYearCode","0$1");
        map.put("salary","60$999");
        webDriver.get(LiePinUtil.getURL(map,myConfigProperties.getSimpleSearchParams()));
        int maxPage = LiePinUtil.getMaxPage(webDriver);
        System.out.println(maxPage);
    }
}

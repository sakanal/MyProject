package com.sakanal.web.util;

import com.sakanal.web.config.MyPixivConfig;
import com.sakanal.web.config.MyWebConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system.selenium")
public class SeleniumUtils {
    private String driverName;
    private String driverDir;
    @Resource
    private MyWebConfig myWebConfig;
    @Resource
    private MyPixivConfig myPixivConfig;

    public WebDriver getWebDriver() {
        System.setProperty(driverName, driverDir);
        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.addArguments("–proxy-server=" + myWebConfig.getProxyHost() + ":" + myWebConfig.getProxyPort());
        edgeOptions.addArguments("--remote-allow-origins=*");
        return new EdgeDriver(edgeOptions);
    }

//    @Bean
    public void setCookies(){
        WebDriver webDriver = getWebDriver();
        webDriver.get("https://www.pixiv.net/");
        webDriver.findElement(By.className("signup-form__submit--login")).click();
        List<WebElement> elementList = webDriver.findElements(By.className("sc-bn9ph6-1"));
        if(elementList.size()==2){
            elementList.get(0).click();
            elementList.get(0).sendKeys(myPixivConfig.getUsername());
            elementList.get(1).click();
            elementList.get(1).sendKeys(myPixivConfig.getPassword());
            webDriver.findElement(By.className("sc-2o1uwj-6")).click();
            Set<Cookie> cookies = webDriver.manage().getCookies();
            StringBuilder builder = new StringBuilder();
            cookies.forEach(cookie->{
                String name = cookie.getName();
                String value = cookie.getValue();
                builder.append(name).append("=").append(value).append(";").append(" ");
            });
            Map<String, String> requestHeader = myPixivConfig.getRequestHeader();
            requestHeader.put("cookie",new String(builder));
            myPixivConfig.setRequestHeader(requestHeader);
        }
    }

}

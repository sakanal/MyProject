package com.sakanal.web.util;

import com.sakanal.web.config.MyPixivConfig;
import com.sakanal.web.config.MyWebConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Selenium工具类
 * 用于自动化浏览器操作，当前主要用于获取Pixiv的Cookie信息
 * 配置前缀：system.selenium
 *
 * @author sakanal
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "system.selenium")
public class SeleniumUtils {
    /**
     * WebDriver驱动名称，用于设置系统属性
     */
    private String driverName;
    
    /**
     * WebDriver驱动文件路径
     */
    private String driverDir;
    @Resource
    private MyWebConfig myWebConfig;
    @Resource
    private MyPixivConfig myPixivConfig;

    /**
     * 获取配置了代理的WebDriver实例
     * 使用Edge浏览器，并配置代理服务器和跨域允许参数
     *
     * @return 配置好的WebDriver实例
     */
    public WebDriver getWebDriver() {
        System.setProperty(driverName, driverDir);
        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.addArguments("–proxy-server=" + myWebConfig.getProxyHost() + ":" + myWebConfig.getProxyPort());
        edgeOptions.addArguments("--remote-allow-origins=*");
        return new EdgeDriver(edgeOptions);
    }

/*    @Bean
    public void setCookies() {
        WebDriver webDriver = getWebDriver();
        webDriver.get("https://www.pixiv.net/");
        webDriver.findElement(By.className("signup-form__submit--login")).click();
        List<WebElement> elementList = webDriver.findElements(By.className("sc-bn9ph6-1"));
        if (elementList.size() == 2) {
            elementList.get(0).click();
            elementList.get(0).sendKeys(myPixivConfig.getUsername());
            elementList.get(1).click();
            elementList.get(1).sendKeys(myPixivConfig.getPassword());
            webDriver.findElement(By.className("sc-2o1uwj-6")).click();
            Set<Cookie> cookies = webDriver.manage().getCookies();
            StringBuilder builder = new StringBuilder();
            cookies.forEach(cookie -> {
                String name = cookie.getName();
                String value = cookie.getValue();
                builder.append(name).append("=").append(value).append(";").append(" ");
            });
            Map<String, String> requestHeader = myPixivConfig.getRequestHeader();
            requestHeader.put("cookie", new String(builder));
            myPixivConfig.setRequestHeader(requestHeader);
        }
    }*/

}
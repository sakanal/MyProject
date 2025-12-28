package com.sakanal.selenium.utils;

import cn.hutool.core.util.RandomUtil;
import com.sakanal.selenium.config.MyConfigProperties;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class LiePinUtil {

    public static WebDriver getWebDriver(MyConfigProperties myConfigProperties) {
        System.setProperty(myConfigProperties.getDriverName(), myConfigProperties.getDriverDir());
        EdgeOptions edgeOptions = new EdgeOptions();
//        String proxyServer = "223.241.78.87:1133";
//        edgeOptions.setProxy(new Proxy().setHttpProxy(proxyServer));
        edgeOptions.addArguments("--remote-allow-origins=*");
        edgeOptions.addArguments("--headless");
        edgeOptions.addArguments("--disable-gpu");
        edgeOptions.addArguments("--window-size=1920,1080");
        edgeOptions.addArguments("--start-maximized");
        edgeOptions.addArguments("--no-sandbox");
        edgeOptions.addArguments("--disable-dev-shm-usage");
        edgeOptions.addArguments("user-agent=" + UserAgentUtil.randomUserAgent());
        return new EdgeDriver(edgeOptions);
    }

    public static WebDriver getSimpleDriver(MyConfigProperties myConfigProperties){
        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.addArguments("--remote-allow-origins=*");
        return new EdgeDriver(edgeOptions);
    }

    public static String getURL(Map<String, String> params, Map<String, String> simpleParams) {
        if (params != null && params.size() > 0) {
            params.keySet().forEach(key -> simpleParams.put(key, params.get(key)));
        }

        String random = RandomUtil.randomString(32);
        simpleParams.put("ckId", random);
        simpleParams.put("skId", random);
        simpleParams.put("fkId", random);

        StringBuilder builder = new StringBuilder("https://www.liepin.com/zhaopin/?");
        simpleParams.keySet().forEach(key -> builder.append(key).append("=").append(simpleParams.get(key)).append("&"));
        builder.deleteCharAt(builder.length() - 1);
        return String.valueOf(builder);
    }

    public static boolean isSign(WebDriver driver, MyConfigProperties myConfigProperties) {
        String[] signTitle = myConfigProperties.getSignTitle();
        String title = driver.getTitle();
        for (String value : signTitle) {
            if (value.equals(title)) {
                return true;
            }
        }
        return false;
    }

    public static int getMaxPage(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        try {
            WebElement element = driver.findElement(By.className("list-pagination-box"));
            List<WebElement> li = element.findElement(By.tagName("ul")).findElements(By.tagName("li"));
            return Integer.parseInt(li.get(li.size() - 2).getAttribute("title"));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getNowPage(WebDriver driver){
        return Integer.parseInt(driver.findElement(By.className("ant-pagination-item-active")).getAttribute("title"));
    }


//    public static final String[] searchParamNames = {"city", "dq", "pubTime", "currentPage", "pageSize", "key", "suggestTag", "workYearCode", "compId", "compName", "compTag", "industry", "salary", "jobKind", "compScale", "compKind", "compStage", "eduLevel", "otherCity", "ckId", "scene", "skId", "fkId", "sfrom"};
//    private static final String[] jobResponsibilitiesDescription = {"职位描述", "工作职责", "岗位职责", "岗位职责", "职责描述"};
//    private static final String[] jobQualificationDescription = {"任职资格", "任职要求", "职位要求", "岗位要求"};
//
//    public static void nextPage(WebDriver driver,String nowTitle) {
//        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
//        WebElement element = driver.findElement(By.className("list-pagination-box"));
//        List<WebElement> li = element.findElement(By.tagName("ul")).findElements(By.tagName("li"));
//        nowTitle = String.valueOf(Integer.parseInt(nowTitle) + 1);
//        for (WebElement webElement : li) {
//            String title = webElement.getAttribute("title");
//            if (title.equals(nowTitle)) {
//                webElement.findElement(By.tagName("a")).click();
//                break;
//            }
//        }
//    }
//
//    public static void getPageListInfo(WebDriver driver, String key, List<LeiPin> leiPins) {
//        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
//        List<WebElement> elements = driver.findElements(By.className("job-detail-box"));
//        String windowHandle = driver.getWindowHandle();
//        for (int i = 0; i < elements.size(); i++) {
//            if (i%10==0 && i!=0){
//                try {
//                    TimeUnit.SECONDS.sleep(45);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            try {
//                elements.get(i).findElement(By.tagName("a")).click();
//            } catch (Exception e) {
//                System.out.println("淦");
//                e.printStackTrace();
//                continue;
//            }
//            for (String handle : driver.getWindowHandles()) {
//                if (!windowHandle.equals(handle)) {
//                    LeiPin info = LeiPinUtil.getInfo(driver, handle, windowHandle);
//                    info.setKey(key);
//                    leiPins.add(info);
//                    System.out.println("已获取"+leiPins.size()+"条数据");
//                }
//            }
//        }
//    }
//
//    public static LeiPin getInfo(WebDriver driver, String handle, String windowHandle) {
//        driver.switchTo().window(handle);
//        LeiPin leiPin = new LeiPin();
//        // 获取职位基本数据
//        {
//            WebElement element = driver.findElement(By.className("job-properties"));
//            List<WebElement> span = element.findElements(By.tagName("span"));
//            ArrayList<String> list = new ArrayList<>();
//            span.forEach(webElement -> {
//                // TODO 简单处理 地区-经验要求-学历要求
//                String text = webElement.getText();
//                if (!"".equals(text)) {
//                    list.add(text);
//                }
//            });
//            leiPin.setCity(list.get(0));
//            leiPin.setExperienceRequirements(list.get(1));
//            leiPin.setEducationalRequirements(list.get(2));
//        }
//        // 获取标题和薪资
//        {
//            WebElement element = driver.findElement(By.className("name-box"));
//            String name = element.findElement(By.className("name")).getText();
//            leiPin.setTitle(name);
//            String salary = element.findElement(By.className("salary")).getText();
//            leiPin.setSalary(salary);
//        }
//        // 获取职位福利
//        {
//            try {
//                WebElement jobApply = driver.findElement(By.className("job-apply-container-left"));
//                int divSize = jobApply.findElements(By.tagName("div")).size();
//                if (divSize > 1) {
//                    List<WebElement> elements = jobApply.findElement(By.className("labels")).findElements(By.tagName("span"));
//                    StringBuilder builder = new StringBuilder();
//                    elements.forEach(element -> builder.append(element.getText()).append("/"));
//                    leiPin.setJobWelfare(String.valueOf(builder));
//                } else {
//                    leiPin.setJobWelfare("无");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        // 获取网页基本信息
//        {
//            leiPin.setUrl(driver.getCurrentUrl());
//        }
//        // 获取公司相关信息
//        {
//            try {
//                int aSize = driver.findElement(By.className("apply-box")).findElements(By.tagName("a")).size();
//
//                if (aSize > 1) {
//                    WebElement element = driver.findElement(By.className("company-card"));
//                    String name = element.findElement(By.className("name")).getText();
//                    leiPin.setCompanyName(name);
//
//                    element = driver.findElement(By.className("company-other"));
//                    List<WebElement> elements = element.findElements(By.className("label-box"));
//                    elements.forEach(webElement -> {
//                        String label = webElement.findElement(By.className("label")).getText();
//                        String text = webElement.findElement(By.className("text")).getText();
//                        if ("企业行业：".equals(label)) {
//                            leiPin.setCompanyNature(text);
//                        } else if ("人数规模：".equals(label)) {
//                            leiPin.setCompanySize(text);
//                        }
//                    });
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        // 获取职责信息
//        {
//            WebElement paragraph = driver.findElement(By.className("paragraph"));
//            try {
//                List<WebElement> elements = paragraph.findElements(By.className("tag-box"));
//                if (elements.size() > 0) {
//                    List<WebElement> li = elements.get(0).findElements(By.tagName("li"));
//                    StringBuilder builder = new StringBuilder();
//                    li.forEach(webElement -> builder.append(webElement.getText()).append("/"));
//                    leiPin.setJobTechnology(String.valueOf(builder));
//                } else {
//                    leiPin.setJobTechnology("无");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//            String text = paragraph.findElement(By.tagName("dd")).getText();
//            leiPin.setJobDescription(text);
//            boolean flag = false;
//            for (String value : jobQualificationDescription) {
//                String[] split = text.split(value);
//                if (split.length > 1) {
//                    setJobInfo(leiPin, split);
//                    flag = true;
//                    break;
//                }
//            }
//            if (!flag) {
//                leiPin.setJobResponsibilities(text);
//                leiPin.setJobQualification(text);
//            }
//        }
//        driver.close();
//        driver.switchTo().window(windowHandle);
//        return leiPin;
//    }
//
//    public static void setJobInfo(LeiPin leiPin, String[] split) {
//        String responsibilities = split[0].replace("：", "").replace(":","");
//        for (String value : jobResponsibilitiesDescription) {
//            responsibilities = responsibilities.replace(value, "");
//            leiPin.setJobResponsibilities(responsibilities);
//        }
//        String qualification = split[1].replace("：", "").replace(":","");
//        leiPin.setJobQualification(qualification);
//    }
//
//    public static Map<String, String> getSimpleParams() {
//        HashMap<String, String> map = new HashMap<>();
//        for (String search : searchParamNames) {
//            map.put(search, "");
//        }
//        map.put("city", "410");
//        map.put("dq", "410");
//
//        map.put("currentPage", "0");
//        map.put("pageSize", "40");
//
//        map.put("workYearCode", "0");
//
//        String random = RandomUtil.randomString(32);
//        map.put("ckId", random);
//        map.put("skId", random);
//        map.put("fkId", random);
//        map.put("sfrom", "search_job_pc");
//        return map;
//    }

}

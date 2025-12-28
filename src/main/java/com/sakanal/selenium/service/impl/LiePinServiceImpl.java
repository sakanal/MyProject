package com.sakanal.selenium.service.impl;

import com.alibaba.excel.EasyExcel;
import com.sakanal.selenium.config.MyConfigProperties;
import com.sakanal.selenium.entity.LiePin;
import com.sakanal.selenium.service.LiePinService;
import com.sakanal.selenium.utils.LiePinUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class LiePinServiceImpl implements LiePinService {

    public static boolean flag=true;

    @Value("${server.port}")
    private String port="8080";

    @Resource
    private MyConfigProperties myConfigProperties;

    @Override
    @Async
    public void getLeiPinInfo(Map<String, String> params) {
        WebDriver driver = LiePinUtil.getWebDriver(myConfigProperties);
        driver.get(LiePinUtil.getURL(params, myConfigProperties.getSimpleSearchParams()));

        long start = System.currentTimeMillis();

        int maxPage;
        // 获取总页数
        if(!LiePinUtil.isSign(driver,myConfigProperties)){
            maxPage = LiePinUtil.getMaxPage(driver);
        }else {
            LiePinUtil.getSimpleDriver(null).get("http://localhost:"+port+"/error");
            return;
        }

        //进入页面
        int nowPage=0;
        ArrayList<LiePin> liePins = new ArrayList<>();
        for (;maxPage!=0 && nowPage<maxPage;nextPage(driver, String.valueOf(nowPage))) {
            // 如果没有任何数据在前面就会停止
            // 所以默认第一页是有数据的
            getPageListInfo(driver, params.get("key"), liePins);
            nowPage = LiePinUtil.getNowPage(driver);
            if (nowPage >= maxPage) {
                break;
            }
        }

        if (liePins.size()>0){
            EasyExcel.write(myConfigProperties.getDownloadDir() + params.get("key") + ".xlsx", LiePin.class)
                    .sheet("sheet")
                    .doWrite(liePins);
            LiePinUtil.getSimpleDriver(null).get("http://localhost:"+port+"/success");
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时"+(end - start) / 1000+"秒");
        driver.close();
    }

    @Override
    public void relieveSign() {
        flag=true;
    }


    public void nextPage(WebDriver driver, String nowPage) {
        WebElement element = driver.findElement(By.className("list-pagination-box"));
        List<WebElement> li = element.findElement(By.tagName("ul")).findElements(By.tagName("li"));
        nowPage = String.valueOf(Integer.parseInt(nowPage) + 1);
        for (WebElement webElement : li) {
            String title = webElement.getAttribute("title");
            if (title.equals(nowPage)) {
                webElement.findElement(By.tagName("a")).click();
                break;
            }
        }
    }

    public void getPageListInfo(WebDriver driver, String key, List<LiePin> liePins) {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        List<WebElement> elements = driver.findElements(By.className("job-detail-box"));
        String windowHandle = driver.getWindowHandle();
        for (int i = 0; i < elements.size(); i++) {
            if (i % 10 == 0) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                elements.get(i).findElement(By.tagName("a")).click();
            } catch (Exception e) {
                System.out.println("淦");
                e.printStackTrace();
                continue;
            }
            for (String handle : driver.getWindowHandles()) {
                if (!windowHandle.equals(handle)) {
                    LiePin info = getInfo(driver, handle, windowHandle);
                    info.setKey(key);
                    liePins.add(info);
                    System.out.println("已获取" + liePins.size() + "条数据");
                }
            }
        }
    }

    public LiePin getInfo(WebDriver driver, String handle, String windowHandle) {
        driver.switchTo().window(handle);
        LiePin leiPin = new LiePin();
        if (LiePinUtil.isSign(driver,myConfigProperties)){
            System.out.println("被标记");
            flag=false;
            while (!flag){
                try {
                    TimeUnit.MINUTES.sleep(1);
                    if (!flag){
                        System.out.println("还是被标记");
                    }else {
                        System.out.println("解除标记");
                        driver.navigate().refresh();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // 获取职位基本数据
        try {
            WebElement element = driver.findElement(By.className("job-properties"));
            List<WebElement> span = element.findElements(By.tagName("span"));
            ArrayList<String> list = new ArrayList<>();
            span.forEach(webElement -> {
                // TODO 简单处理 地区-经验要求-学历要求
                String text = webElement.getText();
                if (!"".equals(text)) {
                    list.add(text);
                }
            });
            leiPin.setCity(list.get(0));
            leiPin.setExperienceRequirements(list.get(1));
            leiPin.setEducationalRequirements(list.get(2));
        } catch (Exception e) {
            e.printStackTrace();
            driver.close();
            driver.switchTo().window(windowHandle);
            return leiPin;
        }
        // 获取标题和薪资
        try {
            WebElement element = driver.findElement(By.className("name-box"));
            String name = element.findElement(By.className("name")).getText();
            leiPin.setTitle(name);
            String salary = element.findElement(By.className("salary")).getText();
            leiPin.setSalary(salary);
        } catch (Exception e) {
            e.printStackTrace();
            driver.close();
            driver.switchTo().window(windowHandle);
            return leiPin;
        }
        // 获取职位福利
        try {
            WebElement jobApply = driver.findElement(By.className("job-apply-container-left"));
            int divSize = jobApply.findElements(By.tagName("div")).size();
            if (divSize > 1) {
                List<WebElement> elements = jobApply.findElement(By.className("labels")).findElements(By.tagName("span"));
                StringBuilder builder = new StringBuilder();
                elements.forEach(element -> builder.append(element.getText()).append("/"));
                leiPin.setJobWelfare(String.valueOf(builder));
            } else {
                leiPin.setJobWelfare("无");
            }
        } catch (Exception e) {
            e.printStackTrace();
            driver.close();
            driver.switchTo().window(windowHandle);
            return leiPin;
        }
        // 获取网页基本信息
        try {
            leiPin.setUrl(driver.getCurrentUrl());
        }catch (Exception e){
            e.printStackTrace();
            driver.close();
            driver.switchTo().window(windowHandle);
            return leiPin;
        }
        // 获取公司相关信息
            try {
                int aSize = driver.findElement(By.className("apply-box")).findElements(By.tagName("a")).size();

                if (aSize > 1) {
                    WebElement element = driver.findElement(By.className("company-card"));
                    String name = element.findElement(By.className("name")).getText();
                    leiPin.setCompanyName(name);

                    element = driver.findElement(By.className("company-other"));
                    List<WebElement> elements = element.findElements(By.className("label-box"));
                    elements.forEach(webElement -> {
                        String label = webElement.findElement(By.className("label")).getText();
                        String text = webElement.findElement(By.className("text")).getText();
                        if ("企业行业：".equals(label)) {
                            leiPin.setCompanyNature(text);
                        } else if ("人数规模：".equals(label)) {
                            leiPin.setCompanySize(text);
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                driver.close();
                driver.switchTo().window(windowHandle);
                return leiPin;
            }
        // 获取职责信息
        try {
            WebElement paragraph = driver.findElement(By.className("paragraph"));
            try {
                List<WebElement> elements = paragraph.findElements(By.className("tag-box"));
                if (elements.size() > 0) {
                    List<WebElement> li = elements.get(0).findElements(By.tagName("li"));
                    StringBuilder builder = new StringBuilder();
                    li.forEach(webElement -> builder.append(webElement.getText()).append("/"));
                    leiPin.setJobTechnology(String.valueOf(builder));
                } else {
                    leiPin.setJobTechnology("无");
                }
            } catch (Exception e) {
                e.printStackTrace();
                driver.close();
                driver.switchTo().window(windowHandle);
                return leiPin;
            }
            String text = paragraph.findElement(By.tagName("dd")).getText();
            leiPin.setJobDescription(text);
            boolean flag = false;
            for (String value : myConfigProperties.getJobQualificationDescription()) {
                String[] split = text.split(value);
                if (split.length > 1) {
                    setJobInfo(leiPin, split);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                leiPin.setJobResponsibilities(text);
                leiPin.setJobQualification(text);
            }
        }catch (Exception e){
            e.printStackTrace();
            driver.close();
            driver.switchTo().window(windowHandle);
            return leiPin;
        }
        driver.close();
        driver.switchTo().window(windowHandle);
        return leiPin;
    }

    public void setJobInfo(LiePin leiPin, String[] split) {
        String responsibilities = split[0].replace("：", "").replace(":", "");
        for (String value : myConfigProperties.getJobResponsibilitiesDescription()) {
            responsibilities = responsibilities.replace(value, "");
            leiPin.setJobResponsibilities(responsibilities);
        }
        String qualification = split[1].replace("：", "").replace(":", "");
        leiPin.setJobQualification(qualification);
    }
}

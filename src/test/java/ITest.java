
import com.sakanal.selenium.entity.LiePin;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索关键字-地区-url-发布时间-学历要求-经验要求-标题-薪资-岗位属性-公司性质-公司名称-公司规模-全文-岗位职责-任职资格-最小薪资-最大薪资-平均薪资
 */
public class ITest {
    private static final String[] jobResponsibilitiesDescription={"职位描述","工作职责","岗位职责","岗位职责","职责描述"};
    private static final String[] jobQualificationDescription={"任职资格","任职要求","职位要求","岗位要求"};
    private static final String key="前端";
    public static void main(String[] args) {
//        EdgeDriverService edgeDriverService = new EdgeDriverService.Builder()
//                .usingDriverExecutable(new File("D:\\Tools\\edgeDriver\\msedgedriver.exe"))
//                .build();
        String key = "webdriver.edge.driver";
        String value = "D:\\Tools\\edgeDriver\\msedgedriver.exe";
        System.setProperty(key, value);

        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.addArguments("--remote-allow-origins=*");
        WebDriver driver = new EdgeDriver(edgeOptions);

        driver.get("https://www.liepin.com/zhaopin/?city=410&dq=410&pubTime=&currentPage=0&pageSize=40&key=" +key+
                "&suggestTag=&workYearCode=0&compId=&compName=&compTag=&industry=&salary=&jobKind=&compScale=&compKind=&compStage=&eduLevel=&otherCity=&ckId=m5eo710ppeh0gozydofn2nz4xr0wn608&scene=input&skId=m5eo710ppeh0gozydofn2nz4xr0wn608&fkId=m5eo710ppeh0gozydofn2nz4xr0wn608&sfrom=search_job_pc&suggestId=");

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        List<WebElement> element = driver.findElements(By.className("job-detail-box"));

        String windowHandle = driver.getWindowHandle();
        ArrayList<LiePin> leiPins = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            element.get(i).findElement(By.tagName("a")).click();
            for (String handle : driver.getWindowHandles()) {
                if (!windowHandle.equals(handle)){
                    LiePin info = getInfo(driver, handle, windowHandle);
                    info.setKey(key);
                    leiPins.add(info);
                }
            }

        }
        leiPins.forEach(System.out::println);

        driver.close();
    }
    public static LiePin getInfo(WebDriver driver, String handle, String windowHandle){
        driver.switchTo().window(handle);
        LiePin leiPin = new LiePin();
        // 获取职位基本数据
        {
            WebElement element = driver.findElement(By.className("job-properties"));
            List<WebElement> span = element.findElements(By.tagName("span"));
            ArrayList<String> list = new ArrayList<>();
            span.forEach(webElement -> {
                // TODO 简单处理 地区-经验要求-学历要求
                String text = webElement.getText();
                if (!"".equals(text)){
                    list.add(text);
                }
            });
            leiPin.setCity(list.get(0));
            leiPin.setExperienceRequirements(list.get(1));
            leiPin.setEducationalRequirements(list.get(2));
        }
        // 获取标题和薪资
        {
            WebElement element = driver.findElement(By.className("name-box"));
            String name = element.findElement(By.className("name")).getText();
            leiPin.setTitle(name);
            String salary = element.findElement(By.className("salary")).getText();
            leiPin.setSalary(salary);
        }
        // 获取职位福利
        {
            try {
                List<WebElement> elements = driver.findElement(By.className("job-apply-container-left")).findElement(By.className("labels")).findElements(By.tagName("span"));
                StringBuilder builder = new StringBuilder();
                elements.forEach(element-> builder.append(element.getText()).append("/"));
                leiPin.setJobWelfare(String.valueOf(builder));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        // 获取网页基本信息
        {
            leiPin.setUrl(driver.getCurrentUrl());
        }
        // 获取公司相关信息
        {
            try {
                WebElement element = driver.findElement(By.className("company-card"));
                String name = element.findElement(By.className("name")).getText();
                leiPin.setCompanyName(name);

                element = driver.findElement(By.className("company-other"));
                List<WebElement> elements = element.findElements(By.className("label-box"));
                elements.forEach(webElement -> {
                    String label = webElement.findElement(By.className("label")).getText();
                    String text = webElement.findElement(By.className("text")).getText();
                    if("企业行业：".equals(label)){
                        leiPin.setCompanyNature(text);
                    }else if ("人数规模：".equals(label)){
                        leiPin.setCompanySize(text);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 获取职责信息
        {
            WebElement paragraph = driver.findElement(By.className("paragraph"));
            List<WebElement> li = paragraph.findElement(By.className("tag-box")).findElements(By.tagName("li"));
            StringBuilder builder = new StringBuilder();
            li.forEach(webElement -> builder.append(webElement.getText()).append("/"));
            leiPin.setJobTechnology(String.valueOf(builder));
            String text = paragraph.findElement(By.tagName("dd")).getText();
            leiPin.setJobDescription(text);
            for (String value : jobQualificationDescription) {
                String[] split = text.split(value);
                if (split.length>1){
                    setJobInfo(leiPin,split);
                }else {
                    leiPin.setJobResponsibilities(text);
                    leiPin.setJobQualification(text);
                }
            }
        }
        driver.close();
        driver.switchTo().window(windowHandle);
        return leiPin;
    }
    public static void setJobInfo(LiePin leiPin , String[] split){
        String responsibilities = split[0].replace("：", "");
        for (String value : jobResponsibilitiesDescription) {
            responsibilities=responsibilities.replace(value,"");
            leiPin.setJobResponsibilities(responsibilities);
        }
        String qualification = split[1].replace("：", "");
        leiPin.setJobQualification(qualification);
    }
}

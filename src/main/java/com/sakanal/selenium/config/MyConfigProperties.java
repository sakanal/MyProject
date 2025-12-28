package com.sakanal.selenium.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "config")
public class MyConfigProperties {
    private String downloadDir = "C://";
//    private String[] jobResponsibilitiesDescription = {"职位描述", "工作职责", "岗位职责", "岗位职责", "职责描述"};
//    private String[] jobQualificationDescription = {"任职资格", "任职要求", "职位要求", "岗位要求"};
    //    private String[] searchParamNames = {"city", "dq", "pubTime", "currentPage", "pageSize", "key", "suggestTag", "workYearCode", "compId", "compName", "compTag", "industry", "salary", "jobKind", "compScale", "compKind", "compStage", "eduLevel", "otherCity", "ckId", "scene", "skId", "fkId", "sfrom"};
    private String[] jobQualificationDescription;
    private String[] jobResponsibilitiesDescription;
    private Map<String,String> simpleSearchParams;
    private String driverName;
    private String driverDir;
    private String[] signTitle;
}

package com.sakanal.selenium.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiePin {
    //id
//    @TableId(value = "id", type = IdType.AUTO)
//    private Long id;
    //搜索关键字
    @ExcelProperty("搜索关键字")
    @ColumnWidth(20)
    private String key;//
    //公司名称
    @ExcelProperty("公司名称")
    @ColumnWidth(20)
    private String companyName;//
    //公司性质
    @ExcelProperty("公司性质")
    @ColumnWidth(20)
    private String companyNature;//
    //公司规模
    @ExcelProperty("公司规模")
    @ColumnWidth(20)
    private String companySize;//
    //地区
    @ExcelProperty("地区")
    @ColumnWidth(20)
    private String city;//
    //岗位福利
    @ExcelProperty("岗位福利")
    @ColumnWidth(20)
    private String jobWelfare;//
    //url
    @ExcelProperty("url")
    @ColumnWidth(20)
    private String url;//
    //发布时间
//    @ExcelProperty("发布时间")
//    @ColumnWidth(20)
//    private String publishDate;
    //标题
    @ExcelProperty("标题")
    @ColumnWidth(20)
    private String title;//
    //学历要求
    @ExcelProperty("学历要求")
    @ColumnWidth(20)
    private String educationalRequirements;//
    //经验要求
    @ExcelProperty("经验要求")
    @ColumnWidth(20)
    private String experienceRequirements;//
    //薪资
    @ExcelProperty("薪资")
    @ColumnWidth(20)
    private String salary;//
    // 职位技术
    @ExcelProperty("职位技术")
    @ColumnWidth(20)
    private String jobTechnology;
    //全文(职位介绍)
    @ExcelProperty("全文")
    @ColumnWidth(20)
    private String jobDescription;//
    //岗位职责
    @ExcelProperty("岗位职责")
    @ColumnWidth(20)
    private String jobResponsibilities;//
    //任职资格
    @ExcelProperty("任职资格")
    @ColumnWidth(20)
    private String jobQualification;//
    //最小薪资
    @ExcelProperty("最小薪资")
    @ColumnWidth(20)
    private String minSalary;//
    //最大薪资
    @ExcelProperty("最大薪资")
    @ColumnWidth(20)
    private String maxSalary;//
    //平均薪资
    @ExcelProperty("平均薪资")
    @ColumnWidth(20)
    private String avgSalary;//

    public void setSalary(String salary) {
        /*
          //100-200元/天  2  2
          //面议
          //6000元/月   2  1
          30-45k  1
          25-50k·14薪  1
         */
        this.salary = salary;
        if ("面议".equals(salary) || "薪资面议".equals(salary)) {
            this.minSalary = salary;
            this.maxSalary = salary;
            this.avgSalary = salary;
        } else {
            String[] split = salary.split("/");
            if (split.length > 1) {
                String[] resultSplit = split[0].replace("元", "").split("-");
                if (resultSplit.length > 1) {
                    // 100-200元/天
                    this.minSalary = resultSplit[0];
                    this.maxSalary = resultSplit[1];
                    this.avgSalary = String.valueOf((Integer.parseInt(this.minSalary.trim()) + Integer.parseInt(this.maxSalary.trim())) / 2.0);
                } else {
                    // 6000元/月
                    this.minSalary = resultSplit[0];
                    this.maxSalary = resultSplit[0];
                    this.avgSalary = resultSplit[0];
                }
            } else {
                //30-45k
                //25-50k·14薪
                String[] resultSplit = salary.split("k")[0].split("-");
                this.minSalary = String.valueOf(Integer.parseInt(resultSplit[0]) * 1000);
                this.maxSalary = String.valueOf(Integer.parseInt(resultSplit[1]) * 1000);
                this.avgSalary = String.valueOf((Integer.parseInt(this.minSalary.trim()) + Integer.parseInt(this.maxSalary.trim())) / 2.0);
            }
        }
    }
}

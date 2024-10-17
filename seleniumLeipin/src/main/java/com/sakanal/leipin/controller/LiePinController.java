package com.sakanal.leipin.controller;

import com.sakanal.leipin.service.LiePinService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author sakanal
 * @since 2023-05-30
 */
@RestController
@RequestMapping("/selenium/liePin")
public class LiePinController {
    @Resource
    private LiePinService liePinService;

//    @GetMapping("/start/{key}")
//    public String myStart(@PathVariable String key){
//        HashMap<String, String> map = new HashMap<>();
//        map.put("key",key);
//        liePinService.getLeiPinInfo(map);
//        return "success";
//    }
    @PostMapping("/start")
    private String start(HttpServletRequest request){
        Enumeration<String> parameterNames = request.getParameterNames();
        HashMap<String, String> params = new HashMap<>();
        while (parameterNames.hasMoreElements()){
            String key = parameterNames.nextElement();
            if ("city".equals(key)){
                params.put("dq",request.getParameter(key));
            }
            params.put(key,request.getParameter(key));
        }
        liePinService.getLeiPinInfo(params);
        return "正在获取数据...（可以关闭此页面）";
    }
    @RequestMapping("/relieveSign")
    public String relieveSign(){
        liePinService.relieveSign();
        return "success";
    }

}

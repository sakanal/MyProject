package com.sakanal.selenium.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class JumpController {
    @RequestMapping("/success")
    public String success(){
        return "success";
    }

}

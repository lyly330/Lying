package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/wehome")
    public String home(Map<String, Object> map) {
        map.put("pageTitle", "HOME");
        map.put("currentPath", "/wehome");
        map.put("content", "home/home");   // 对应 templates/home/home.html
        return "common/layout";
    }
}
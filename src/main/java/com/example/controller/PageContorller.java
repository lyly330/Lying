package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class PageContorller {

    @GetMapping("/")
    public String index(Map<String, Object> map) {
        map.put("title", "暖心智能助手");
        map.put("smallTitle", "今天也一起轻松解决问题吧");
        return "login";
    }

    @GetMapping("/login")
    public String login(Map<String, Object> map) {
        map.put("title", "暖心智能助手");
        map.put("smallTitle", "今天也一起轻松解决问题吧");
        return "login";
    }

    @GetMapping("/chat")
    public String chat(Map<String, Object> map) {
        map.put("pageTitle", "暖心聊天");
        map.put("currentPath", "/chat");
        map.put("content", "chat/chat");
        return "common/layout";
    }

    @GetMapping("/document")
    public String document(Map<String, Object> map) {
        map.put("pageTitle", "知识小屋");
        map.put("currentPath", "/document");
        map.put("content", "document/document");
        return "common/layout";
    }
}

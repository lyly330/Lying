package com.example.controller;

import com.example.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 发送消息，流式返回
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String msg, HttpSession session) {
        String userId = String.valueOf(session.getAttribute("userId"));
        String conversationId = session.getId();
        return chatService.chat(conversationId, userId, msg);
    }

    // 获取消息历史
    @GetMapping("/messageHistory")
    public Mono<List<Message>> messageHistory(HttpSession session) {
        String conversationId = session.getId();
        return chatService.history(conversationId);
    }
}
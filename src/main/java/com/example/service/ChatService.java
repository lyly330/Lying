package com.example.service;

import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ChatService {
    /**
     * 智能聊天方法
     * @param userId
     * @param msg
     * @return
     */
    Flux<String> chat(String conversationId, String userId, String msg);

    /**
     * 读取指定id的历史聊天记录
     * @param id
     * @return
     */

    Mono<List<Message>> history(String id);
}

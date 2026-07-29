package com.example.service.impl;

import com.example.domain.Pet;
import com.example.domain.UploadedDocument;
import com.example.service.ChatService;
import com.example.service.DocumentService;
import com.example.service.PetService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ChatMemoryRepository chatMemoryRepository;
    @Autowired
    private PetService petService;
    @Autowired
    private DocumentService documentService;

    @Override
    public Flux<String> chat(String conversationId, String userId, String msg) {
        // 解析用户ID
        Long uid = null;
        try {
            uid = Long.parseLong(userId);
        } catch (NumberFormatException ignored) {}

        // 获取宠物信息
        Pet pet = null;
        if (uid != null) {
            pet = petService.getPetByUserId(uid);
        }

        // 构建系统提示
        String systemPrompt = "你是一个智能AI助手，请用中文回答用户的问题。";
        if (pet != null && pet.getName() != null && !pet.getName().isEmpty()) {
            String breed = (pet.getBreed() != null && !pet.getBreed().isEmpty()) ? pet.getBreed() : "宠物";
            String personality = (pet.getPersonality() != null && !pet.getPersonality().isEmpty()) ? pet.getPersonality() : "友好、活泼";
            systemPrompt = "你是一只" + breed + "，名叫" + pet.getName() + "，性格" + personality + "。"
                    + "请以这只宠物的身份和口吻回答用户的问题，语气要符合它的性格。";
        }

        // 根据用户问题召回相关文档内容
        List<String> keywords = extractKeywords(msg);
        List<UploadedDocument> relatedDocs = documentService.searchDocumentsByKeywords(keywords, uid).block();
        if (relatedDocs != null && !relatedDocs.isEmpty()) {
            StringBuilder docContext = new StringBuilder("\n\n【参考文档】\n");
            int totalLength = 0;
            for (UploadedDocument doc : relatedDocs) {
                String content = doc.getContent();
                if (content == null || content.isBlank()) {
                    continue;
                }
                // 单篇文档最多取 1500 字符，避免超出上下文
                if (content.length() > 1500) {
                    content = content.substring(0, 1500) + "...";
                }
                docContext.append("文件：").append(doc.getFilename()).append("\n");
                docContext.append(content).append("\n\n");
                totalLength += content.length();
                if (totalLength > 3000) {
                    break;
                }
            }
            systemPrompt += docContext;
        }

        return chatClient.prompt()
                .system(systemPrompt)
                .user(msg)
                .advisors(a -> a.param(CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    @Override
    public Mono<List<Message>> history(String id) {
        List<Message> messages = chatMemoryRepository.findByConversationId(id);
        return Mono.just(messages);
    }

    /**
     * 从用户消息中提取关键词，用于文档召回
     */
    private List<String> extractKeywords(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }

        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "什么", "怎么", "多少", "哪里", "谁", "为什么", "如何", "请", "的", "了", "是", "我",
                "你", "他", "她", "它", "我们", "你们", "他们", "这", "那", "这些", "那些",
                "一个", "一些", "这个", "那个", "可以", "知道", "告诉", "一下", "吗", "呢",
                "what", "how", "who", "where", "when", "why", "which", "the", "a", "an",
                "is", "are", "was", "were", "do", "does", "did", "can", "could", "would"
        ));

        List<String> keywords = new ArrayList<>();
        // 匹配 2 个及以上中文字符，或 2 个及以上英文单词字符
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z0-9]{2,}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (!stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }
}
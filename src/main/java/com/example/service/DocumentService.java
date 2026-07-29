package com.example.service;

import com.example.domain.UploadedDocument;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DocumentService {
    /**
     * 知识库训练
     * @param files 文件列表
     * @param userId 当前用户ID
     * @return 上传结果
     */
    Mono<String> upload(List<MultipartFile> files, Long userId);

    /**
     * 查询当前用户的已上传文档
     * @param userId 当前用户ID
     * @return 文档列表
     */
    Mono<List<UploadedDocument>> listUploadedDocuments(Long userId);

    /**
     * 根据关键词搜索当前用户的文档内容（用于聊天上下文召回）
     * @param keywords 关键词列表
     * @param userId 当前用户ID
     * @return 匹配的文档列表
     */
    Mono<List<UploadedDocument>> searchDocumentsByKeywords(List<String> keywords, Long userId);
}

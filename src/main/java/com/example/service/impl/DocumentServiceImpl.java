package com.example.service.impl;

import com.example.domain.UploadedDocument;
import com.example.service.DocumentService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void ensureTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS uploaded_document (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT,
                    filename VARCHAR(255) NOT NULL,
                    content_type VARCHAR(120),
                    file_size BIGINT,
                    content LONGTEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        // 兼容旧表：如果 user_id 字段不存在则添加
        addColumnIfNotExists("uploaded_document", "user_id", "BIGINT");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnType) {
        try {
            String checkSql = """
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = ?
                      AND column_name = ?
                    """;
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            }
        } catch (Exception e) {
            // 字段已存在或其他异常，忽略
        }
    }

    @Override
    public Mono<String> upload(List<MultipartFile> files, Long userId) {
        if (StringUtils.isEmpty(files) || files.isEmpty()) {
            return Mono.empty();
        }

        StringBuilder result = new StringBuilder();
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            Resource resource = file.getResource();

            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> docs = reader.get();

            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withKeepSeparator(true)
                    .withMaxNumChunks(330)
                    .build();

            List<Document> documents = splitter.split(docs);
            // 注意：当前使用 DeepSeek，不支持 Embedding，暂时不写入向量库
            // vectorStore.add(documents);
            saveUploadRecord(file, docs, userId);
            result.append(filename).append(" 上传成功，已经存入数据库啦\n");
        }
        return Mono.just(result.toString());
    }

    @Override
    public Mono<List<UploadedDocument>> listUploadedDocuments(Long userId) {
        ensureTable();
        String sql = """
                SELECT id, user_id, filename, content_type, file_size, content, created_at
                FROM uploaded_document
                WHERE user_id = ? OR user_id IS NULL
                ORDER BY created_at DESC
                """;
        return Mono.just(jdbcTemplate.query(sql, new UploadedDocumentRowMapper(), userId));
    }

    @Override
    public Mono<List<UploadedDocument>> searchDocumentsByKeywords(List<String> keywords, Long userId) {
        if (keywords == null || keywords.isEmpty() || userId == null) {
            return Mono.just(List.of());
        }
        ensureTable();

        // 构建动态 WHERE 条件：user_id = ? AND (content LIKE '%keyword1%' OR content LIKE '%keyword2%')
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append("content LIKE ?");
        }

        String sql = """
                SELECT id, user_id, filename, content_type, file_size, content, created_at
                FROM uploaded_document
                WHERE (user_id = ? OR user_id IS NULL) AND (%s)
                ORDER BY created_at DESC
                """.formatted(where.toString());

        Object[] keywordParams = keywords.stream()
                .map(k -> "%" + k + "%")
                .toArray();

        // 合并参数：第一个是 userId，后面是关键词
        Object[] params = new Object[keywordParams.length + 1];
        params[0] = userId;
        System.arraycopy(keywordParams, 0, params, 1, keywordParams.length);

        return Mono.just(jdbcTemplate.query(sql, new UploadedDocumentRowMapper(), params));
    }

    private void saveUploadRecord(MultipartFile file, List<Document> docs, Long userId) {
        ensureTable();
        String content = docs.stream()
                .map(Document::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n\n"));

        jdbcTemplate.update("""
                INSERT INTO uploaded_document (user_id, filename, content_type, file_size, content)
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                content);
    }

    private static class UploadedDocumentRowMapper implements RowMapper<UploadedDocument> {
        @Override
        public UploadedDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
            UploadedDocument document = new UploadedDocument();
            document.setId(rs.getLong("id"));
            document.setUserId(rs.getLong("user_id"));
            document.setFilename(rs.getString("filename"));
            document.setContentType(rs.getString("content_type"));
            document.setFileSize(rs.getLong("file_size"));
            document.setContent(rs.getString("content"));
            document.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return document;
        }
    }
}

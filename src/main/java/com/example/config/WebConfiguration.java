package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源映射：把 URL /avatars/** 映射到本地上传目录（role.avatar.upload-dir），
 * 保证用户头像上传后能直接通过 /avatars/{filename} 访问。
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${role.avatar.upload-dir:./upload/avatars}")
    private String uploadDir;

    @PostConstruct
    public void ensureUploadDir() {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            System.out.println("[avatar] 上传目录就绪: " + dir);
        } catch (Exception e) {
            System.err.println("[avatar] 创建上传目录失败: " + e.getMessage());
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(absolutePath);
    }
}

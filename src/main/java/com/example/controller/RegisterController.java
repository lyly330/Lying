package com.example.controller;

import com.example.domain.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
public class RegisterController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String email = params.get("email");
        String tel = params.get("tel");

        // 1. 非空校验（用户名和密码必填）
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码不能为空"));
        }

        // 2. 检查用户名是否已存在
        Optional<User> existingUser = userService.findByUsername(username.trim());
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名已存在"));
        }

        // 3. 创建新用户
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password.trim()));  // 生产环境需加密
        newUser.setEmail(email != null ? email.trim() : null);
        newUser.setTel(tel != null ? tel.trim() : null);
        newUser.setCreateTime(LocalDateTime.now());

        // 4. 保存到数据库
        userService.save(newUser);

        // 5. 返回成功响应
        return ResponseEntity.ok(Map.of("success", true, "message", "注册成功"));
    }
}
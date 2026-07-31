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

        // 1. 必填校验
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名不能为空"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码不能为空"));
        }
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "邮箱不能为空"));
        }

// 邮箱格式校验
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!email.trim().matches(emailRegex)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "邮箱格式不正确"));
        }

// 电话号格式校验（如果有值）
        String telValue = tel != null ? tel.trim() : "";
        if (!telValue.isEmpty()) {
            String phoneRegex = "^1[3-9]\\d{9}$";
            if (!telValue.matches(phoneRegex)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "手机号格式不正确（需11位，以1开头）"));
            }
        }
        // 2. 检查用户名是否已存在
        Optional<User> existingUser = userService.findByUsername(username.trim());
        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名已存在"));
        }

        // 3. 检查邮箱是否已存在
        Optional<User> emailUser = userService.findByEmail(email.trim());
        if (emailUser.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "该邮箱已被注册"));
        }

        // 4. 检查电话号是否已存在（若填写了电话号）
        if (tel != null && !tel.trim().isEmpty()) {
            Optional<User> telUser = userService.findByTel(tel.trim());
            if (telUser.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "该电话号已被注册"));
            }
        }

        // 5. 创建新用户（密码加密存储）
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password.trim()));
        newUser.setEmail(email.trim());
        newUser.setTel(tel != null ? tel.trim() : null);
        newUser.setCreateTime(LocalDateTime.now());

        // 6. 保存到数据库
        userService.save(newUser);

        return ResponseEntity.ok(Map.of("success", true, "message", "注册成功"));
    }
}
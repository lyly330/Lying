package com.example.controller;

import com.example.domain.User;
import com.example.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;   // 用于密码校验

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> creds,
                                                     HttpSession session) {
        String email = creds.get("email");
        String tel = creds.get("tel");
        String password = creds.get("password");

        // 密码不能为空
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码不能为空"));
        }

        // 根据邮箱或电话查找用户
        Optional<User> userOpt = Optional.empty();
        if (email != null && !email.trim().isEmpty()) {
            userOpt = userService.findByEmail(email.trim());
        } else if (tel != null && !tel.trim().isEmpty()) {
            userOpt = userService.findByTel(tel.trim());
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入邮箱或电话号"));
        }

        // 用户不存在或密码错误
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "邮箱/电话号不存在，或密码错误"));
        }

        User user = userOpt.get();
        // 验证密码（BCrypt 比对）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "邮箱/电话号不存在，或密码错误"));
        }

        // 兼容旧用户：如果没有随机 userId，则生成并保存
        if (user.getUserId() == null) {
            Long oldId = user.getId();
            Long newUserId = userService.generateUniqueUserId();
            user.setUserId(newUserId);
            user = userService.save(user);
            // 迁移旧关联数据（pet、document 等）
            migrateOldUserData(oldId, newUserId);
        }

        // 存入 Session
        session.setAttribute("loggedUser", user.getUsername());
        session.setAttribute("userId", user.getId());   // 存主键
        // 返回登录成功信息
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "登录成功");
        resp.put("username", user.getUsername());
        resp.put("id", user.getUserId());
        resp.put("userId", user.getUserId());
        resp.put("token", "dummy-token"); // 可按需改为 JWT
        return ResponseEntity.ok(resp);
    }

    /**
     * 迁移旧数据：将 pet、uploaded_document 等表中关联的旧主键 id 替换为新的 userId
     */
    private void migrateOldUserData(Long oldId, Long newUserId) {
        try {
            jdbcTemplate.update("UPDATE pet SET user_id = ? WHERE user_id = ?", newUserId, oldId);
            jdbcTemplate.update("UPDATE uploaded_document SET user_id = ? WHERE user_id = ?", newUserId, oldId);
        } catch (Exception e) {
            // 迁移失败不影响登录，记录日志即可
            System.err.println("迁移旧用户数据失败: " + e.getMessage());
        }
    }
}
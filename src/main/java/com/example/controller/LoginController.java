package com.example.controller;

import com.example.domain.User;
import com.example.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> creds,
                                                     HttpSession session) {
        String username = creds.get("username");
        String password = creds.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }

        Optional<User> userOpt = userService.login(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 兼容旧用户：如果还没有随机 userId，则生成并保存
            if (user.getUserId() == null) {
                Long oldId = user.getId();
                Long newUserId = userService.generateUniqueUserId();
                user.setUserId(newUserId);
                user = userService.save(user);
                // 把旧数据（按老自增 id 关联的）迁移到新 userId
                migrateOldUserData(oldId, newUserId);
            }
            session.setAttribute("loggedUser", user.getUsername());
            session.setAttribute("userId", user.getUserId());

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("message", "登录成功");
            resp.put("username", user.getUsername());
            resp.put("id", user.getUserId());
            resp.put("userId", user.getUserId());
            resp.put("token", "dummy-token"); // 可按需改为JWT
            return ResponseEntity.ok(resp);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }
    }

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
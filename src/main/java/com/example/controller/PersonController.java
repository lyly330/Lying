package com.example.controller;

import com.example.domain.User;
import com.example.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PersonController {

    @Autowired
    private UserService userService;

    @GetMapping("/person")
    public String personPage(Model model, HttpSession session) {
        String username = (String) session.getAttribute("loggedUser");
        if (username == null) {
            return "redirect:/";
        }
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        model.addAttribute("user", user);
        return "person/person";
    }

    @PostMapping("/person/updateJson")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateUserJson(
            @RequestBody Map<String, String> params, HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = params.get("username");
        String password = params.get("password");
        String email = params.get("email");
        String tel = params.get("tel");
        String newPassword = (password == null || password.trim().isEmpty()) ? null : password;

        try {
            User updated = userService.updateUser(userId, username, newPassword, email, tel);
            session.setAttribute("loggedUser", updated.getUsername());
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("user", updated);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
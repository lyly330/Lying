package com.example.controller;

import com.example.domain.Role;
import com.example.service.RoleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class RoleController {

    @Autowired
    private RoleService roleService;   // 注入接口，而非实现类

    @GetMapping("/roles")
    public String roles(Map<String, Object> map) {
        map.put("pageTitle", "角色集");
        map.put("currentPath", "/roles");
        map.put("content", "role/role");   // 对应 templates/role/role.html
        return "common/layout";
    }

    /**
     * 按层分组返回该用户的全部角色（首次访问自动初始化默认角色）
     * 返回格式：{ "layers": { "1": [...], "2": [...], ... } }
     */
    @GetMapping("/api/roles")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoles(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        // ✅ 如果该用户还没有默认角色，自动初始化（第一层 5 个预设 + 第二层默认卡片）
        roleService.initDefaultRoles(userId);

        Map<Integer, List<Role>> layers = new TreeMap<>();
        for (int layer = 1; ; layer++) {
            List<Role> roles = roleService.getRolesByLayer(userId, layer);
            if (roles.isEmpty()) break;
            layers.put(layer, roles);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("layers", layers);
        return ResponseEntity.ok(data);
    }

    /**
     * 在指定层追加一张默认卡片（二层区域右上角 in.png 添加按钮）
     */
    @PostMapping("/api/roles/add")
    @ResponseBody
    public ResponseEntity<?> addRole(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        int layer = body.get("layer") == null ? 2 : Integer.parseInt(body.get("layer").toString());
        if (layer < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "第一层为系统固定角色，不能添加"));
        }
        return ResponseEntity.ok(roleService.addDefaultRole(userId, layer));
    }

    /**
     * 修改卡片信息（点击卡片展开编辑后保存）
     */
    @PutMapping("/api/roles/{id}")
    @ResponseBody
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Role role, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(roleService.updateRole(userId, id, role));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 互动一次：好感度 +5（测试用按钮）
     */
    @PostMapping("/api/roles/{id}/favor")
    @ResponseBody
    public ResponseEntity<?> increaseFavor(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(roleService.increaseFavorability(userId, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 点击 unlock.png 解锁指定角色（路径变量为被解锁者的 id）
     */
    @PostMapping("/api/roles/{targetId}/unlock-next")
    @ResponseBody
    public ResponseEntity<?> unlockNext(@PathVariable Long targetId,
                                        @RequestBody Map<String, Object> body,
                                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        int layer = body.get("layer") == null ? 1 : Integer.parseInt(body.get("layer").toString());
        try {
            return ResponseEntity.ok(roleService.unlockNextRole(userId, layer, targetId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

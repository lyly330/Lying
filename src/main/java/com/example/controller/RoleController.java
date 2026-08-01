package com.example.controller;

import com.example.domain.Role;
import com.example.service.RoleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
public class RoleController {

    @Autowired
    private RoleService roleService;   // 注入接口，而非实现类

    @Value("${role.avatar.upload-dir:./upload/avatars}")
    private String avatarUploadDir;

    /** 仅允许常见图片后缀，防止被利用上传可执行文件 */
    private static final Pattern IMAGE_EXT = Pattern.compile("\\.(png|jpe?g|gif|webp|bmp|svg)$", Pattern.CASE_INSENSITIVE);

    @GetMapping("/roles")
    public String roles(Map<String, Object> map) {
        map.put("pageTitle", "角色集");
        map.put("currentPath", "/roles");
        map.put("content", "role/role");   // 对应 templates/role/role.html
        return "common/layout";
    }

    /**
     * 上传角色头像图片。仅支持 image/* 类型，保存至 role.avatar.upload-dir，
     * 返回可被 <img src="..."> 直接访问的 URL /avatars/{filename}
     */
    @PostMapping("/api/roles/upload-avatar")
    @ResponseBody
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "未选择文件"));
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持图片文件"));
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
        }
        if (!IMAGE_EXT.matcher(ext).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的图片格式（仅 png/jpg/gif/webp/bmp/svg）"));
        }
        try {
            Path dir = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String filename = UUID.randomUUID() + ext.toLowerCase();
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/avatars/" + filename;
            Map<String, String> resp = new HashMap<>();
            resp.put("url", url);
            return ResponseEntity.ok(resp);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "保存失败：" + e.getMessage()));
        }
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

    /**
     * 调整指定层的卡片数量（1-10）
     */
    @PostMapping("/api/roles/adjust-count")
    @ResponseBody
    public ResponseEntity<?> adjustCount(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        if (body.get("layer") == null || body.get("count") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 layer 或 count 参数"));
        }
        int layer = Integer.parseInt(body.get("layer").toString());
        int count = Integer.parseInt(body.get("count").toString());
        try {
            List<Role> roles = roleService.adjustLayerCount(userId, layer, count);
            return ResponseEntity.ok(Map.of("layer", layer, "roles", roles));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

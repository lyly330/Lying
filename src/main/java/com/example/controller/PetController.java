package com.example.controller;

import com.example.domain.Pet;
import com.example.service.PetService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pet")
public class PetController {

    @Autowired
    private PetService petService;

    // 获取当前用户的宠物信息
    @GetMapping
    public ResponseEntity<Pet> getPet(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        Pet pet = petService.getPetByUserId(userId);
        if (pet == null) {
            // 返回空，前端可显示默认新建表单
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pet);
    }

    // 保存或更新宠物信息
    @PostMapping
    public ResponseEntity<?> savePet(@RequestBody Pet pet, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        try {
            Pet saved = petService.saveOrUpdatePet(userId, pet);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // 更新体力（例如游戏互动后）
    @PatchMapping("/energy")
    public ResponseEntity<Pet> updateEnergy(@RequestParam int energy, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        Pet pet = petService.getPetByUserId(userId);
        if (pet == null) return ResponseEntity.notFound().build();
        pet.setEnergy(Math.min(100, Math.max(0, energy)));
        Pet updated = petService.saveOrUpdatePet(userId, pet);
        return ResponseEntity.ok(updated);
    }
}

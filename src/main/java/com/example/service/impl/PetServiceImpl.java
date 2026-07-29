package com.example.service.impl;

import com.example.domain.Pet;
import com.example.repository.PetRepository;
import com.example.service.PetService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetServiceImpl implements PetService {

    @Autowired
    private PetRepository petRepository;   // 必须注入

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Pet getPetByUserId(Long userId) {
        return petRepository.findByUserId(userId).orElse(null);
    }

    @PostConstruct
    public void fixPetForeignKey() {
        try {
            // 1. 检查当前外键是否引用 member.id（旧约束）
            String checkFkSql = """
                    SELECT COUNT(*) FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                        ON tc.constraint_name = kcu.constraint_name
                        AND tc.table_schema = kcu.table_schema
                    WHERE tc.table_schema = DATABASE()
                      AND tc.table_name = 'pet'
                      AND tc.constraint_type = 'FOREIGN KEY'
                      AND kcu.column_name = 'user_id'
                      AND kcu.referenced_table_name = 'member'
                      AND kcu.referenced_column_name = 'id'
                    """;
            Integer count = jdbcTemplate.queryForObject(checkFkSql, Integer.class);
            if (count != null && count > 0) {
                // 2. 删除旧外键
                jdbcTemplate.execute("ALTER TABLE pet DROP FOREIGN KEY pet_ibfk_1");
                // 3. 添加新外键引用 member.user_id
                jdbcTemplate.execute("""
                        ALTER TABLE pet
                        ADD CONSTRAINT fk_pet_user_id
                        FOREIGN KEY (user_id) REFERENCES member(user_id)
                        ON DELETE CASCADE
                        """);
            }
        } catch (Exception e) {
            // 外键已修正或其他异常，忽略
            System.err.println("修复 pet 外键失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Pet saveOrUpdatePet(Long userId, Pet pet) {
        // 关联用户
        pet.setUserId(userId);
        // 如果头像为空，设默认
        if (pet.getAvatar() == null || pet.getAvatar().isEmpty()) {
            pet.setAvatar("noerool.png");
        }
        // 保证体力在0-100
        if (pet.getEnergy() == null) pet.setEnergy(100);
        if (pet.getEnergy() < 0) pet.setEnergy(0);
        if (pet.getEnergy() > 100) pet.setEnergy(100);
        // 查找是否已有记录
        Pet existing = petRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            pet.setId(existing.getId());
            pet.setCreateTime(existing.getCreateTime());
        }
        return petRepository.save(pet);
    }
}
package com.example.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet")   // 对应数据库表名（小写）
public class Pet {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        private String name;
        private Integer age;
        private String breed;
        private String hobby;
        private String personality;
        private String avatar;

        @Column(columnDefinition = "INT DEFAULT 100")
        private Integer energy;

        @Column(name = "create_time", updatable = false)
        private LocalDateTime createTime;

        @Column(name = "update_time")
        private LocalDateTime updateTime;

        // ===== 无参构造（JPA 需要） =====
        public Pet() {}

        // ===== 有参构造（方便创建） =====
        public Pet(Long userId, String name, Integer age, String breed,
                   String hobby, String personality, String avatar, Integer energy) {
                this.userId = userId;
                this.name = name;
                this.age = age;
                this.breed = breed;
                this.hobby = hobby;
                this.personality = personality;
                this.avatar = avatar;
                this.energy = energy != null ? energy : 100;
        }

        // ===== Getter / Setter =====
        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public Long getUserId() {
                return userId;
        }

        public void setUserId(Long userId) {
                this.userId = userId;
        }

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public Integer getAge() {
                return age;
        }

        public void setAge(Integer age) {
                this.age = age;
        }

        public String getBreed() {
                return breed;
        }

        public void setBreed(String breed) {
                this.breed = breed;
        }

        public String getHobby() {
                return hobby;
        }

        public void setHobby(String hobby) {
                this.hobby = hobby;
        }

        public String getPersonality() {
                return personality;
        }

        public void setPersonality(String personality) {
                this.personality = personality;
        }

        public String getAvatar() {
                return avatar;
        }

        public void setAvatar(String avatar) {
                this.avatar = avatar;
        }

        public Integer getEnergy() {
                return energy;
        }

        public void setEnergy(Integer energy) {
                this.energy = energy;
        }

        public LocalDateTime getCreateTime() {
                return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
                this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
                return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
                this.updateTime = updateTime;
        }

        // ===== 可选：在保存前自动填充时间（使用 JPA 回调） =====
        @PrePersist
        protected void onCreate() {
                if (createTime == null) {
                        createTime = LocalDateTime.now();
                }
                if (updateTime == null) {
                        updateTime = LocalDateTime.now();
                }
                if (energy == null) {
                        energy = 100;
                }
        }

        @PreUpdate
        protected void onUpdate() {
                updateTime = LocalDateTime.now();
        }
}
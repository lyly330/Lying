package com.example.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "member")
public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id", unique = true)
        private Long userId;

        @Column(unique = true, nullable = false)
        private String username;

        @Column(nullable = false)
        private String password;

        private String email;
        private String tel;

        @Column(name = "create_time")
        private LocalDateTime createTime;

        // getter/setter（必须）
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTel() { return tel; }
        public void setTel(String tel) { this.tel = tel; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

}

package com.example.service.impl;

import com.example.domain.User;
import com.example.repository.UserRepository;
import com.example.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword())); // 生产用BCrypt
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateUser(Long id, String username, String password, String email, String tel) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (username != null && !username.isEmpty()) {
            user.setUsername(username);
        }
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (tel != null) {
            user.setTel(tel);
        }
        return userRepository.save(user);   // 必须调用 save
    }
    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            user.setUserId(generateUniqueUserId());
        }
        return userRepository.save(user);
    }

    @Override
    public Long generateUniqueUserId() {
        Long userId;
        int attempts = 0;
        do {
            userId = 100_000_000L + ThreadLocalRandom.current().nextLong(900_000_000L);
            attempts++;
        } while (userRepository.existsByUserId(userId) && attempts < 10);
        return userId;
    }

    @PostConstruct
    public void ensureUserIdColumn() {
        addColumnIfNotExists("member", "user_id", "BIGINT");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnType) {
        try {
            String checkSql = """
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = ?
                      AND column_name = ?
                    """;
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            }
        } catch (Exception e) {
            // 字段已存在或其他异常，忽略
        }
    }
}

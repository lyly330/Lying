package com.example.init;  // 包名根据你的项目结构调整

import com.example.domain.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        System.out.println("开始迁移明文密码...");
        int count = 0;
        for (User user : userRepository.findAll()) {
            // 如果密码不是以 $2a$ 开头（即不是 BCrypt 格式），则加密后更新
            if (!user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
                userRepository.save(user);
                count++;
                System.out.println(" 已迁移用户: " + user.getUsername());
            }
        }
        System.out.println("密码迁移完成，共迁移 " + count + " 个用户");
    }
}

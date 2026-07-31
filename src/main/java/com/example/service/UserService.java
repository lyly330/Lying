package com.example.service;

import com.example.domain.User;
import java.util.Optional;

public interface UserService {
    Optional<User> login(String username, String password);
    Optional<User> findByUsername(String username);
    User updateUser(Long id, String username, String password, String email, String tel);
    User save(User user);
    Long generateUniqueUserId();
    Optional<User> findByEmail(String email);
    Optional<User> findByTel(String tel);
}
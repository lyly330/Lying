package com.example.repository;

import com.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByTel(String tel);
}

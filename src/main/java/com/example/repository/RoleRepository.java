package com.example.repository;

import com.example.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByUserIdAndLayerAndParentIdOrderByOrderIndex(Long userId, Integer layer, Long parentId);
    List<Role> findByUserIdAndLayerOrderByOrderIndex(Long userId, Integer layer);
}
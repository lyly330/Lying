package com.example.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String name;
    private String description;
    private String avatar;
    private Integer layer;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    /** 是否为“未解锁角色卡片” */
    @Column(name = "locked")
    private Boolean locked = false;

    /** 好感度最大值（一层默认 50） */
    @Column(name = "favor_max")
    private Integer favorMax = 50;

    /** 当前好感度 */
    @Column(name = "affection_value")
    private Integer affectionValue = 0;

    /** 好感度满后置位，前端据此永久不渲染进度条 */
    @Column(name = "favor_bar_shattered")
    private Boolean favorBarShattered = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ===== Getter / Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Integer getLayer() { return layer; }
    public void setLayer(Integer layer) { this.layer = layer; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getLocked() { return locked; }
    public void setLocked(Boolean locked) { this.locked = locked; }

    public Integer getFavorMax() { return favorMax; }
    public void setFavorMax(Integer favorMax) { this.favorMax = favorMax; }

    public Integer getAffectionValue() { return affectionValue; }
    public void setAffectionValue(Integer affectionValue) { this.affectionValue = affectionValue; }

    public Boolean getFavorBarShattered() { return favorBarShattered; }
    public void setFavorBarShattered(Boolean favorBarShattered) { this.favorBarShattered = favorBarShattered; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
package com.example.service.impl;

import com.example.domain.Role;
import com.example.repository.RoleRepository;
import com.example.service.RoleService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 启动时确保 role 表存在（项目未开启 JPA ddl-auto）
     * avatar 等文本列需 utf8mb4 才能存储 4 字节 emoji（🦜🐰🦝🙂）
     */
    @PostConstruct
    public void initTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS role (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                    "description VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                    "avatar VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, " +
                    "layer INT NOT NULL DEFAULT 1, " +
                    "parent_id BIGINT DEFAULT 0, " +
                    "order_index INT DEFAULT 0, " +
                    "locked TINYINT(1) DEFAULT 0, " +
                    "favor_max INT DEFAULT 50, " +
                    "affection_value INT DEFAULT 0, " +
                    "favor_bar_shattered TINYINT(1) DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_role_user_layer (user_id, layer)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

            // 兼容旧表：缺 locked / favor_max / affection_value / favor_bar_shattered / parent_id 列则补上
            String[][] cols = {
                    {"locked",              "TINYINT(1) DEFAULT 0"},
                    {"favor_max",           "INT DEFAULT 50"},
                    {"affection_value",     "INT DEFAULT 0"},
                    {"favor_bar_shattered", "TINYINT(1) DEFAULT 0"},
                    {"parent_id",           "BIGINT DEFAULT 0"}
            };
            for (String[] c : cols) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'role' AND column_name = '" + c[0] + "'",
                        Integer.class);
                if (count != null && count == 0) {
                    jdbcTemplate.execute("ALTER TABLE role ADD COLUMN " + c[0] + " " + c[1]);
                }
            }

            // 兼容旧表：将原 favorability 列重命名为 affection_value（保留数据）
            Integer hasOld = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'role' AND column_name = 'favorability'",
                    Integer.class);
            if (hasOld != null && hasOld > 0) {
                try {
                    jdbcTemplate.execute("ALTER TABLE role RENAME COLUMN favorability TO affection_value");
                } catch (Exception e) {
                    // 低版本 MySQL 不支持 RENAME COLUMN，走“新增—拷贝—删除”路径
                    jdbcTemplate.execute("UPDATE role SET affection_value = favorability WHERE affection_value = 0 AND favorability <> 0");
                    jdbcTemplate.execute("ALTER TABLE role DROP COLUMN favorability");
                }
            }

            // 兼容旧表：若 avatar 等列仍为 utf8（utf8mb3），强制转换为 utf8mb4
            String curCharset = jdbcTemplate.queryForObject(
                    "SELECT CHARACTER_SET_NAME FROM information_schema.columns " +
                    "WHERE table_schema = DATABASE() AND table_name = 'role' AND column_name = 'avatar'",
                    String.class);
            if (curCharset != null && !"utf8mb4".equalsIgnoreCase(curCharset)) {
                jdbcTemplate.execute("ALTER TABLE role CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                // 旧 charset 下插入的 emoji 默认角色一定是失败的，清空后让 initDefaultRoles 重新写入
                jdbcTemplate.execute("DELETE FROM role");
            }

            // 数据迁移：二层及以上角色上限从旧值（20 或 50）统一升为 100
            jdbcTemplate.execute("UPDATE role SET favor_max = 100 WHERE layer >= 2 AND favor_max <> 100");
            // 兜底：即使历史代码错误地给 layer>=2 写入了 favor_max=50，也强制拉到 100
            Integer wrongMax = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM role WHERE layer >= 2 AND favor_max < 100", Integer.class);
            if (wrongMax != null && wrongMax > 0) {
                jdbcTemplate.execute("UPDATE role SET favor_max = 100 WHERE layer >= 2 AND favor_max < 100");
            }

            // 数据迁移：历史中 affection_value >= favor_max 的卡片永久标记为 shattered
            jdbcTemplate.execute("UPDATE role SET favor_bar_shattered = 1 " +
                    "WHERE favor_bar_shattered = 0 AND affection_value >= favor_max");

            // 仅对未初始化的新用户：由 initDefaultRoles 在首次访问 /roles 时创建默认角色。
            // 启动时不再重置已有用户的 affection_value/locked，避免服务器重启抹掉好感度进度。
        } catch (Exception e) {
            System.err.println("初始化 role 表失败: " + e.getMessage());
        }
    }

    @Override
    public List<Role> getRolesByLayer(Long userId, Integer layer) {
        return roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, layer);
    }

    @Override
    public List<Role> getChildren(Long userId, Integer layer, Long parentId) {
        return roleRepository.findByUserIdAndLayerAndParentIdOrderByOrderIndex(userId, layer, parentId);
    }

    @Override
    public Role saveRole(Role role) {
        if (role.getCreatedAt() == null) {
            role.setCreatedAt(LocalDateTime.now());
        }
        return roleRepository.save(role);
    }

    /**
     * 在指定层末尾追加一张默认卡片（点击 in.png 添加按钮时调用）
     */
    @Override
    @Transactional
    public Role addDefaultRole(Long userId, Integer layer) {
        List<Role> existing = roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, layer);
        int nextIndex = existing.stream()
                .mapToInt(r -> r.getOrderIndex() == null ? 0 : r.getOrderIndex())
                .max().orElse(-1) + 1;

        Role role = new Role();
        role.setUserId(userId);
        role.setName("新角色");
        role.setDescription("点击卡片编辑角色信息");
        role.setAvatar("🙂");
        role.setLayer(layer);
        role.setParentId(0L);
        role.setOrderIndex(nextIndex);
        role.setLocked(true);         // 新增的卡片默认锁定，需要上一层好感度满才能解锁
        role.setFavorMax(100);
        role.setAffectionValue(0);
        role.setCreatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    /**
     * 修改卡片信息（头像/名称/描述），未解锁卡片与他人卡片不可修改
     */
    @Override
    @Transactional
    public Role updateRole(Long userId, Long roleId, Role newData) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        if (!role.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改该角色");
        }
        if (Boolean.TRUE.equals(role.getLocked())) {
            throw new IllegalArgumentException("未解锁角色暂不可修改，我们快点见面吧");
        }
        if (newData.getName() != null && !newData.getName().isBlank()) {
            role.setName(newData.getName().trim());
        }
        if (newData.getDescription() != null) {
            role.setDescription(newData.getDescription().trim());
        }
        if (newData.getAvatar() != null && !newData.getAvatar().isBlank()) {
            role.setAvatar(newData.getAvatar().trim());
        }
        return roleRepository.save(role);
    }

    /**
     * 互动一次，好感度 +5（测试用按钮）；达到上限后不再增长。
     */
    @Override
    @Transactional
    public Role increaseFavorability(Long userId, Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        if (!role.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权互动该角色");
        }
        if (Boolean.TRUE.equals(role.getLocked())) {
            throw new IllegalArgumentException("未解锁角色暂不可互动，我们快点见面吧");
        }
        int max = role.getFavorMax() == null ? 50 : role.getFavorMax();
        int cur = role.getAffectionValue() == null ? 0 : role.getAffectionValue();
        if (cur < max) {
            int updated = Math.min(cur + 5, max);
            role.setAffectionValue(updated);
            // 好感度打满后仅在前端展示“我和你最好了”弹跳文字，
            // favorBarShattered 由 unlockNextRole 在用户实际点击解锁下一张时置位
        }
        return roleRepository.save(role);
    }

    /**
     * 解锁指定锁定角色 targetRoleId：
     * 校验同层中 orderIndex 紧挨着它的前一张“已解锁”角色好感度是否已达上限；
     * 达标则将 target 置为 unlocked、affection_value 重置为 0。
     * 若 target 已解锁则幂等返回 target 自身。
     */
    @Override
    @Transactional
    public Role unlockNextRole(Long userId, Integer layer, Long targetRoleId) {
        Role target = roleRepository.findById(targetRoleId)
                .orElseThrow(() -> new IllegalArgumentException("目标角色不存在"));
        if (!target.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权解锁该角色");
        }
        // 幂等：已解锁直接返回
        if (!Boolean.TRUE.equals(target.getLocked())) {
            return target;
        }

        List<Role> roles = roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, layer);
        // 优先找 target 前面同层中 orderIndex 最接近的"已解锁"角色作为触发者
        Role prev = null;
        for (Role r : roles) {
            if (r.getId().equals(targetRoleId)) break;
            if (!Boolean.TRUE.equals(r.getLocked())) {
                prev = r;
            }
        }

        // 同层找不到触发者 → 允许跨层触发（layer >= 2 的首卡，从上一层最后一张 shattered 卡片触发）
        if (prev == null && layer != null && layer >= 2) {
            List<Role> prevLayer = roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, layer - 1);
            for (int i = prevLayer.size() - 1; i >= 0; i--) {
                Role p = prevLayer.get(i);
                if (!Boolean.TRUE.equals(p.getLocked()) && Boolean.TRUE.equals(p.getFavorBarShattered())) {
                    prev = p;
                    break;
                }
            }
        }

        if (prev == null) {
            throw new IllegalArgumentException("无可触发解锁的前置角色");
        }
        int max = prev.getFavorMax() == null ? 50 : prev.getFavorMax();
        int cur = prev.getAffectionValue() == null ? 0 : prev.getAffectionValue();
        if (cur < max) {
            System.err.println("[unlock-next] 好感度未满：prevId=" + prev.getId()
                    + " (" + prev.getName() + "), cur=" + cur + ", max=" + max
                    + ", targetId=" + target.getId());
            throw new IllegalArgumentException("好感度未满，暂不能解锁下一位角色");
        }
        // 用户主动解锁下一张：把触发者（上一张卡）永久标记为 shattered，
        // 前端据此不再渲染其进度条与“我和你最好了”弹跳文字
        if (!Boolean.TRUE.equals(prev.getFavorBarShattered())) {
            prev.setFavorBarShattered(true);
            roleRepository.save(prev);
        }
        target.setLocked(false);
        target.setAffectionValue(0);
        return roleRepository.save(target);
    }

    /**
     * 调整指定层的角色卡片数量（1-10）：
     * - 增加：按 orderIndex 追加默认锁定卡片
     * - 减少：从 orderIndex 最大开始删除 locked=true 的卡片；已解锁的卡片不删
     */
    @Override
    @Transactional
    public List<Role> adjustLayerCount(Long userId, Integer layer, int targetCount) {
        if (layer < 2) {
            throw new IllegalArgumentException("第一层为系统固定角色，不能调整数量");
        }
        if (targetCount < 1) targetCount = 1;
        if (targetCount > 10) targetCount = 10;

        List<Role> roles = roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, layer);
        int currentCount = roles.size();

        if (targetCount > currentCount) {
            // 增加：按顺序补默认卡片
            int maxIndex = roles.stream()
                    .mapToInt(r -> r.getOrderIndex() == null ? 0 : r.getOrderIndex())
                    .max().orElse(-1);
            for (int i = 0; i < targetCount - currentCount; i++) {
                Role role = new Role();
                role.setUserId(userId);
                role.setName("新角色");
                role.setDescription("点击卡片编辑角色信息");
                role.setAvatar("🙂");
                role.setLayer(layer);
                role.setParentId(0L);
                role.setOrderIndex(maxIndex + 1 + i);
                role.setLocked(true);
                role.setFavorMax(100);
                role.setAffectionValue(0);
                role.setCreatedAt(LocalDateTime.now());
                roleRepository.save(role);
            }
        } else if (targetCount < currentCount) {
            // 减少：从 orderIndex 最大开始删除，只删 locked=true 的卡片
            int toRemove = currentCount - targetCount;
            List<Role> sorted = new ArrayList<>(roles);
            sorted.sort((a, b) -> {
                int oa = a.getOrderIndex() == null ? 0 : a.getOrderIndex();
                int ob = b.getOrderIndex() == null ? 0 : b.getOrderIndex();
                return Integer.compare(ob, oa);
            });
            for (Role r : sorted) {
                if (toRemove <= 0) break;
                if (Boolean.TRUE.equals(r.getLocked())) {
                    roleRepository.delete(r);
                    toRemove--;
                }
            }
            if (toRemove > 0) {
                throw new IllegalArgumentException("仍有 " + toRemove + " 张已解锁卡片不能被删除，请先解锁更多角色或增加数量");
            }
        }

        return roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, layer);
    }

    /**
     * 初始化默认角色：
     * 第一层固定 5 个预设角色，仅 orderIndex=0 的小猫默认解锁，其余锁定；
     * 第二层默认 1 张“人类/动物”（锁定）+ 1 张未解锁卡片。
     */
    @Override
    @Transactional
    public void initDefaultRoles(Long userId) {
        // ---- 第一层：固定预设角色 ----
        List<Role> firstLayer = roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, 1);
        if (firstLayer.isEmpty()) {
            String[] names   = {"小猫", "小狗", "小鹦鹉", "小兔子", "浣熊"};
            String[] avatars = {"/images/cat.png", "/images/dog.png", "🦜", "🐰", "🦝"};
            String[] descs   = {
                    "优雅慵懒的猫咪伙伴",
                    "忠诚热情的狗狗伙伴",
                    "能说会道的鹦鹉伙伴",
                    "温柔乖巧的兔子伙伴",
                    "机灵调皮的浣熊伙伴"
            };
            for (int i = 0; i < names.length; i++) {
                Role role = new Role();
                role.setUserId(userId);
                role.setName(names[i]);
                role.setDescription(descs[i]);
                role.setAvatar(avatars[i]);
                role.setLayer(1);
                role.setParentId(0L);
                role.setOrderIndex(i);
                role.setLocked(i != 0);      // 仅首角色（小猫）初始解锁
                role.setFavorMax(50);
                role.setAffectionValue(0);
                role.setCreatedAt(LocalDateTime.now());
                roleRepository.save(role);
            }
        }

        // ---- 第二层：默认卡片 + 未解锁卡片（均锁定） ----
        List<Role> secondLayer = roleRepository.findByUserIdAndLayerOrderByOrderIndex(userId, 2);
        if (secondLayer.isEmpty()) {
            Role human = new Role();
            human.setUserId(userId);
            human.setName("新角色");
            human.setDescription("你的默认角色，点击卡片编辑");
            human.setAvatar("🙂");
            human.setLayer(2);
            human.setParentId(0L);
            human.setOrderIndex(0);
            human.setLocked(true);
            human.setFavorMax(100);
            human.setAffectionValue(0);
            human.setCreatedAt(LocalDateTime.now());
            roleRepository.save(human);

            Role lockedCard = new Role();
            lockedCard.setUserId(userId);
            lockedCard.setName("未解锁角色");
            lockedCard.setDescription("我们快点见面吧");
            lockedCard.setLayer(2);
            lockedCard.setParentId(0L);
            lockedCard.setOrderIndex(1);
            lockedCard.setLocked(true);
            lockedCard.setFavorMax(100);
            lockedCard.setAffectionValue(0);
            lockedCard.setCreatedAt(LocalDateTime.now());
            roleRepository.save(lockedCard);
        }
    }
}

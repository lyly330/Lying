package com.example.service;

import com.example.domain.Role;
import java.util.List;

public interface RoleService {

    List<Role> getRolesByLayer(Long userId, Integer layer);

    List<Role> getChildren(Long userId, Integer layer, Long parentId);

    Role saveRole(Role role);

    /** 在指定层末尾追加一张默认卡片 */
    Role addDefaultRole(Long userId, Integer layer);

    /** 修改卡片（头像/名称/描述），未解锁卡片不可修改 */
    Role updateRole(Long userId, Long roleId, Role newData);

    /** 互动一次，好感度 +5（测试用按钮），达到上限后不再增长 */
    Role increaseFavorability(Long userId, Long roleId);

    /** 解锁指定层中“下一个”锁定角色（由好感度满的卡片触发） */
    Role unlockNextRole(Long userId, Integer layer, Long prevRoleId);

    void initDefaultRoles(Long userId);

}
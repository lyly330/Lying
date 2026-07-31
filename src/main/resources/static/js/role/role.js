// ================= 角色集页面逻辑 =================

let roleLayersData = {};   // { "1": [...], "2": [...] }

document.addEventListener('DOMContentLoaded', () => {
    if (!checkRoleLogin()) return;
    loadRoles();
});

// 与其他页面保持一致：用 localStorage 的 userInfo 判断登录态
function checkRoleLogin() {
    const userInfo = localStorage.getItem('userInfo');
    if (!userInfo) {
        window.location.href = '/login';
        return false;
    }
    return true;
}

// ---------- 加载并渲染全部层级 ----------
async function loadRoles() {
    try {
        const resp = await fetch('/api/roles');
        if (resp.status === 401) {
            window.location.href = '/login';
            return;
        }
        const data = await resp.json();
        roleLayersData = data.layers || {};
        renderLayers();
    } catch (e) {
        document.getElementById('roleLayers').innerHTML =
            '<div class="role-loading">加载失败，请刷新重试</div>';
        console.error('加载角色集失败:', e);
    }
}

const LAYER_NAMES = { 1: '第一层 · 固定角色', 2: '第二层 · 我的角色' };

function layerTitle(layer) {
    return LAYER_NAMES[layer] || `第${layer}层`;
}

function renderLayers() {
    const container = document.getElementById('roleLayers');
    const layerNums = Object.keys(roleLayersData).map(Number).sort((a, b) => a - b);
    if (layerNums.length === 0) {
        container.innerHTML = '<div class="role-loading">暂无角色数据</div>';
        return;
    }

    container.innerHTML = layerNums.map(layer => {
        const roles = roleLayersData[layer] || [];
        const cards = roles.map(role => renderCard(role, layer, roles)).join('');
        // 第一层固定，不显示添加按钮；二层及以上右上角显示 in.png 添加按钮
        const addBtn = layer >= 2
            ? `<button class="role-add-btn" title="添加角色" onclick="addRole(${layer})">
                   <img src="/images/in.png" alt="添加">
               </button>`
            : '';
        return `
            <section class="role-layer" data-layer="${layer}">
                <div class="role-layer-header">
                    <h3 class="role-layer-title">${layerTitle(layer)}
                        <span class="layer-tag">${layer === 1 ? '系统预置' : '可自由扩展'}</span>
                    </h3>
                    ${addBtn}
                </div>
                <div class="role-card-grid">${cards}</div>
            </section>`;
    }).join('');
}

function renderCard(role, layer, siblings) {
    const locked   = !!role.locked;
    const favor    = role.affectionValue == null ? 0 : role.affectionValue;
    const favorMax = role.favorMax     == null ? 50 : role.favorMax;
    const percent  = favorMax > 0 ? Math.min(100, Math.round(favor / favorMax * 100)) : 0;
    const canUnlock = !locked && favor >= favorMax;     // 好感度满，下一张可解锁

    // 好感度进度条（锁定卡片也显示进度条，CSS 会将其置灰）
    const favorBar = `
        <div class="role-card-favor">
            <div class="role-favor-bar">
                <div class="role-favor-fill" style="width:${percent}%"></div>
            </div>
            <div class="role-favor-text">${favor} / ${favorMax}</div>
        </div>`;

    // 锁定卡片：左上角 lock.png，中间大问号，底部小字
    if (locked) {
        // 若前置角色好感度已满，则在该卡片右上角显示 unlock.png 解锁图标
        const unlockIcon = canUnlockPrev(role, siblings)
            ? `<img class="role-card-unlock"
                    src="/images/unlock.png"
                    alt="解锁"
                    onclick="event.stopPropagation(); this.style.pointerEvents='none'; this.style.opacity=0.5; unlockRole(${role.id}, ${layer});">`
            : '';
        return `
            <div class="role-card locked">
                ${unlockIcon}
                <img class="role-card-lock" src="/images/lock.png" alt="未解锁">
                <div class="role-card-question">？</div>
                <div class="role-card-locked-tip">我们快点见面吧</div>
                ${favorBar}
            </div>`;
    }

    // 已解锁卡片
    const avatar = role.avatar || '🙂';
    const avatarHtml = avatar.startsWith('/') || avatar.startsWith('http')
        ? `<img src="${escapeHtml(avatar)}" alt="">`
        : escapeHtml(avatar);

    // 测试用 +5 好感度按钮（仅已解锁卡片可见）
    const testBtn = `
        <button class="role-favor-test-btn"
                onclick="event.stopPropagation(); addFavor(${role.id});">
            互动 +5
        </button>`;

    return `
        <div class="role-card" onclick="openRoleModal(${role.id})">
            <div class="role-card-avatar">${avatarHtml}</div>
            <div class="role-card-name">${escapeHtml(role.name || '')}</div>
            <div class="role-card-desc">${escapeHtml(role.description || '')}</div>
            ${favorBar}
            ${testBtn}
        </div>`;
}

// 判断 role 前面的角色好感度是否已满（用于在锁定卡片右上角显示 unlock.png）
function canUnlockPrev(role, siblings) {
    const prev = siblings.find(r =>
        !r.locked && r.orderIndex === (role.orderIndex - 1)
    );
    if (!prev) return false;
    const favor    = prev.affectionValue == null ? 0 : prev.affectionValue;
    const favorMax = prev.favorMax     == null ? 50 : prev.favorMax;
    return favor >= favorMax;
}

// ---------- 添加默认卡片（in.png 按钮） ----------
async function addRole(layer) {
    try {
        const resp = await fetch('/api/roles/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ layer })
        });
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            alert(err.error || '添加失败');
            return;
        }
        await loadRoles();
    } catch (e) {
        console.error('添加角色失败:', e);
        alert('添加失败，请重试');
    }
}

// ---------- 测试用 +5 好感度 ----------
async function addFavor(roleId) {
    try {
        const resp = await fetch(`/api/roles/${roleId}/favor`, { method: 'POST' });
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            alert(err.error || '互动失败');
            return;
        }
        await loadRoles();
    } catch (e) {
        console.error('互动失败:', e);
        alert('互动失败，请重试');
    }
}

// ---------- 点击 unlock.png 解锁指定角色（传入被解锁者 id） ----------
let roleUnlocking = false;   // 防抖：避免连续点击重复提交

async function unlockRole(targetRoleId, layer) {
    if (roleUnlocking) return;
    roleUnlocking = true;
    try {
        const resp = await fetch(`/api/roles/${targetRoleId}/unlock-next`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ layer })
        });
        const data = await resp.json().catch(() => ({}));
        if (!resp.ok) {
            alert(data.error || '解锁失败');
            return;
        }
        await loadRoles();
    } catch (e) {
        console.error('解锁失败:', e);
        alert('解锁失败，请重试');
    } finally {
        roleUnlocking = false;
    }
}

// ---------- 点击卡片展开编辑 ----------
function findRole(roleId) {
    for (const layer of Object.keys(roleLayersData)) {
        const found = (roleLayersData[layer] || []).find(r => r.id === roleId);
        if (found) return found;
    }
    return null;
}

function openRoleModal(roleId) {
    const role = findRole(roleId);
    if (!role || role.locked) return;   // 锁定卡片不响应编辑
    document.getElementById('editRoleId').value = role.id;
    document.getElementById('editRoleAvatar').value = role.avatar || '';
    document.getElementById('editRoleName').value = role.name || '';
    document.getElementById('editRoleDesc').value = role.description || '';
    document.getElementById('roleModalMask').style.display = 'flex';
}

function closeRoleModal() {
    document.getElementById('roleModalMask').style.display = 'none';
}

async function saveRoleEdit() {
    const id = document.getElementById('editRoleId').value;
    const name = document.getElementById('editRoleName').value.trim();
    if (!name) {
        alert('名称不能为空');
        return;
    }
    const payload = {
        name,
        avatar: document.getElementById('editRoleAvatar').value.trim(),
        description: document.getElementById('editRoleDesc').value.trim()
    };
    try {
        const resp = await fetch(`/api/roles/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            alert(err.error || '保存失败');
            return;
        }
        closeRoleModal();
        await loadRoles();
    } catch (e) {
        console.error('保存角色失败:', e);
        alert('保存失败，请重试');
    }
}

// 点击遮罩空白处关闭弹层
document.addEventListener('click', (e) => {
    if (e.target && e.target.id === 'roleModalMask') closeRoleModal();
});

function escapeHtml(str) {
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;');
}

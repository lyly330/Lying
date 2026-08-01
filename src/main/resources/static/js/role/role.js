// ================= 角色集页面逻辑 =================

let roleLayersData = {};   // { "1": [...], "2": [...] }

// 已播放破碎动画的角色 id 集合，用于控制解锁按钮延迟出现
const shatterAnimationDoneSet = new Set();

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
        // 预置：后端已标记 favorBarShattered 的角色，说明其破碎动画在以往会话已播过
        roles.forEach(r => {
            if (r.favorBarShattered) shatterAnimationDoneSet.add(r.id);
        });
        const cards = roles.map(role => renderCard(role, layer, roles)).join('');
        // 第一层固定，不显示任何操作；二层及以上右上角显示卡片数量调节器
        const headerRight = layer >= 2
            ? `<div class="role-layer-actions">
                   <label class="role-count-label">卡片数
                       <input type="number"
                              class="role-count-input"
                              min="1" max="10"
                              value="${roles.length}"
                              onchange="adjustLayerCount(${layer}, this)"
                              oninput="clampCountInput(this)">
                   </label>
               </div>`
            : '';
        return `
            <section class="role-layer" data-layer="${layer}">
                <div class="role-layer-header">
                    <h3 class="role-layer-title">${layerTitle(layer)}
                        <span class="layer-tag">${layer === 1 ? '系统预置' : '可自由扩展'}</span>
                    </h3>
                    ${headerRight}
                </div>
                <div class="role-card-grid">${cards}</div>
            </section>`;
    }).join('');

    // 好感度满的进度条播放破碎动画后自动从 DOM 移除
    scheduleFavorBarShatter();
}

function scheduleFavorBarShatter() {
    // 好感度已满（favor >= favorMax）且尚未破碎的进度条播放破碎动画
    document.querySelectorAll('.role-favor-bar').forEach(bar => {
        if (bar.classList.contains('shattered') || bar.dataset.shatterBound === '1') return;
        const roleId = Number(bar.dataset.roleId);
        if (shatterAnimationDoneSet.has(roleId)) return;   // 已播过，跳过

        // 查找当前角色是否好感度已满
        let isFull = false;
        for (const k of Object.keys(roleLayersData)) {
            const found = (roleLayersData[k] || []).find(r => r.id === roleId);
            if (found) {
                const fv  = found.affectionValue == null ? 0 : found.affectionValue;
                const fmx = found.favorMax       == null ? 50 : found.favorMax;
                if (fv >= fmx) isFull = true;
                break;
            }
        }
        if (!isFull) return;

        bar.classList.add('shattered');
        bar.dataset.shatterBound = '1';
        bar.addEventListener('animationend', (e) => {
            if (e.animationName !== 'favorShatter') return;
            // 仅移除进度条元素；弹跳文字保留在原位继续展示
            const favor = bar.parentElement;
            if (favor && favor.parentElement) {
                // 保留外层 .role-card-favor，仅移除 .role-favor-bar
                if (favor === bar) {
                    // 不应该发生；防御性处理
                    favor.parentElement.removeChild(favor);
                } else {
                    favor.removeChild(bar);
                }
            }
            shatterAnimationDoneSet.add(roleId);
        });
    });
}

function renderCard(role, layer, siblings) {
    const locked   = !!role.locked;
    const favor    = role.affectionValue == null ? 0 : role.affectionValue;
    const favorMax = role.favorMax     == null ? 50 : role.favorMax;
    const percent  = favorMax > 0 ? Math.min(100, Math.round(favor / favorMax * 100)) : 0;
    const canUnlock = !locked && favor >= favorMax;     // 好感度满，下一张可解锁
    const shattered = !!role.favorBarShattered;         // 服务端永久标记：不再渲染进度条
    // 查找下一张卡片，用于判断“我和你最好了”文字是否应该继续显示
    const nextCard = siblings.find(r => r.orderIndex === (role.orderIndex + 1));
    // 好感度已满且尚未破碎：显示弹跳文字（与进度条同存）
    const isFull = !shattered && favor >= favorMax;
    // 文字持续显示直到用户解锁下一张卡片（nextCard 不存在或仍锁定）
    const showBestTip = isFull && (!nextCard || nextCard.locked);

    // 好感度进度条：服务端标记为 shattered 则完全不出 DOM
    // 好感度刚打满时，进度条与弹跳文字同屏展示（进度条会在后续破碎消失，文字保留）
    const favorBar = shattered ? '' : `
        <div class="role-card-favor ${isFull ? 'role-card-favor-best' : ''}">
            <div class="role-favor-bar" data-role-id="${role.id}">
                <div class="role-favor-fill" style="width:${percent}%"></div>
            </div>
            ${showBestTip
                ? `<span class="role-favor-best-tip">我和你最好了</span>`
                : `<div class="role-favor-text">${favor} / ${favorMax}</div>`}
        </div>`;

    // 锁定卡片：左上角 lock.png，中间大问号，底部小字
    if (locked) {
        // 若前置角色的破碎动画已播完，则在该卡片右上角显示 unlock.png 解锁图标
        const unlockIcon = canUnlockPrev(role, siblings, layer)
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

    // 测试用 +5 好感度按钮（已 shattered 或好感度已满的卡片不再提供互动按钮）
    const testBtn = (shattered || favor >= favorMax) ? '' : `
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
// 跨层支持：layer >= 2 的首卡（orderIndex === 0）从上一层最后一张卡片判断
function canUnlockPrev(role, siblings, layer) {
    if (role.orderIndex === 0 && layer >= 2) {
        const prevLayerRoles = roleLayersData[layer - 1] || [];
        if (prevLayerRoles.length === 0) return false;
        const last = prevLayerRoles[prevLayerRoles.length - 1];
        const fv  = last.affectionValue == null ? 0 : last.affectionValue;
        const fmx = last.favorMax       == null ? 50 : last.favorMax;
        return fv >= fmx;
    }
    const prev = siblings.find(r =>
        !r.locked && r.orderIndex === (role.orderIndex - 1)
    );
    if (!prev) return false;
    const fv  = prev.affectionValue == null ? 0 : prev.affectionValue;
    const fmx = prev.favorMax       == null ? 50 : prev.favorMax;
    return fv >= fmx;
}

// ---------- 调整指定层的卡片数量（1-10） ----------
let roleCountAdjustTimer = null;

function clampCountInput(input) {
    let v = parseInt(input.value);
    if (isNaN(v)) return;
    if (v < 1)  input.value = 1;
    if (v > 10) input.value = 10;
}

async function adjustLayerCount(layer, input) {
    if (roleCountAdjustTimer) clearTimeout(roleCountAdjustTimer);
    roleCountAdjustTimer = setTimeout(async () => {
        const count = Math.max(1, Math.min(10, parseInt(input.value) || 1));
        input.value = count;
        input.disabled = true;
        try {
            const resp = await fetch('/api/roles/adjust-count', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ layer, count })
            });
            const data = await resp.json().catch(() => ({}));
            if (!resp.ok) {
                alert(data.error || '调整失败');
                await loadRoles();   // 回滚到实际数量
                return;
            }
            await loadRoles();
        } catch (e) {
            console.error('调整数量失败:', e);
            alert('调整失败，请重试');
            await loadRoles();
        } finally {
            input.disabled = false;
        }
    }, 300);
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

// ---------- 头像上传：触发本地文件选择 ----------
function pickAvatar() {
    const input = document.getElementById('avatarFileInput');
    if (!input) return;
    input.value = '';             // 重置，保证同一张图再次选择也能触发 change
    input.click();
}

// 文件选定后自动上传，成功后将 URL 写入头像输入框
async function uploadAvatar(input) {
    const file = input.files && input.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        alert('仅支持图片文件（jpg/png/gif/webp/bmp/svg）');
        input.value = '';
        return;
    }
    const btn = document.querySelector('.role-avatar-upload-btn');
    if (btn) btn.disabled = true;
    try {
        const form = new FormData();
        form.append('file', file);
        const resp = await fetch('/api/roles/upload-avatar', {
            method: 'POST',
            body: form                // 不能手动设置 Content-Type，浏览器会自动填充 boundary
        });
        const data = await resp.json().catch(() => ({}));
        if (!resp.ok) {
            alert(data.error || '上传失败');
            return;
        }
        if (data.url) {
            document.getElementById('editRoleAvatar').value = data.url;
        }
    } catch (e) {
        console.error('上传头像失败:', e);
        alert('上传失败，请重试');
    } finally {
        if (btn) btn.disabled = false;
        input.value = '';
    }
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

// person.js - 个人中心完整交互

document.addEventListener('DOMContentLoaded', function() {

    // ============================================================
    // 1. 用户头像管理（同原有）
    // ============================================================
    const AVATAR_KEY = 'userAvatar';
    function loadUserAvatar() {
        const img = document.getElementById('avatarImg');
        if (!img) return;
        const saved = localStorage.getItem(AVATAR_KEY);
        img.src = saved ? '/images/' + saved : '/images/noerool.png';
    }
    window.showAvatarPicker = function(event) {
        if (event) event.stopPropagation();
        document.getElementById('avatarPicker').style.display = 'flex';
    };
    window.closeAvatarPicker = function() {
        document.getElementById('avatarPicker').style.display = 'none';
    };
    window.closeAvatarPickerOutside = function(event) {
        if (event.target === event.currentTarget) closeAvatarPicker();
    };
    window.selectAvatar = function(filename) {
        localStorage.setItem(AVATAR_KEY, filename);
        document.getElementById('avatarImg').src = '/images/' + filename;
        closeAvatarPicker();
        // 可调用后端接口保存头像（可选）
        // saveUserAvatar(filename);
    };
    loadUserAvatar();

    // ============================================================
    // 2. 用户个人信息编辑（原有功能）
    // ============================================================
    const editIcon = document.getElementById('editIcon');
    const infoDisplay = document.getElementById('infoDisplay');
    const infoEditForm = document.getElementById('infoEditForm');

    editIcon.addEventListener('click', function() {
        // 切换显示
        if (infoEditForm.style.display === 'none') {
            // 填充当前值到表单
            document.getElementById('editInfoUsername').value = document.getElementById('displayUsername').textContent.trim();
            document.getElementById('editInfoEmail').value = document.getElementById('displayEmail').textContent.trim() === '-' ? '' : document.getElementById('displayEmail').textContent.trim();
            document.getElementById('editInfoTel').value = document.getElementById('displayTel').textContent.trim() === '-' ? '' : document.getElementById('displayTel').textContent.trim();
            infoDisplay.style.display = 'none';
            infoEditForm.style.display = 'block';
        } else {
            cancelInfoEdit();
        }
    });

    window.saveInfo = function() {
        const username = document.getElementById('editInfoUsername').value.trim();
        const password = document.getElementById('editInfoPassword').value;
        const email = document.getElementById('editInfoEmail').value.trim();
        const tel = document.getElementById('editInfoTel').value.trim();
        if (!username) { alert('用户名不能为空'); return; }
        fetch('/person/updateJson', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email, tel })
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    document.getElementById('displayUsername').textContent = data.user.username;
                    document.getElementById('displayEmail').textContent = data.user.email || '-';
                    document.getElementById('displayTel').textContent = data.user.tel || '-';
                    document.querySelector('.greeting').textContent = '欢迎回来，' + data.user.username + ' ☀️';
                    alert('个人信息已更新！');
                    cancelInfoEdit();
                } else {
                    alert('更新失败：' + data.message);
                }
            })
            .catch(err => alert('网络错误'));
    };

    window.cancelInfoEdit = function() {
        infoEditForm.style.display = 'none';
        infoDisplay.style.display = 'block';
    };

    // ============================================================
    // 3. 宠物数据加载与展示
    // ============================================================
    function loadPet() {
        fetch('/api/pet')
            .then(res => {
                if (res.status === 204) {
                    // 没有宠物数据，显示默认空状态
                    resetPetDisplay();
                    return null;
                }
                if (!res.ok) throw new Error('加载宠物失败');
                return res.json();
            })
            .then(pet => {
                if (pet) {
                    updatePetDisplay(pet);
                }
            })
            .catch(err => console.warn('加载宠物信息失败:', err));
    }

    function resetPetDisplay() {
        document.getElementById('petNameDisplay').textContent = '我的宠物';
        document.getElementById('petNameSpan').textContent = '未设置';
        document.getElementById('petAgeSpan').textContent = '-';
        document.getElementById('petBreedSpan').textContent = '-';
        document.getElementById('petHobbySpan').textContent = '-';
        document.getElementById('petPersonalitySpan').textContent = '-';
        document.getElementById('petAvatarImg').src = '/images/noerool.png';
        document.getElementById('energyFill').style.width = '100%';
        document.getElementById('energyText').textContent = '100%';
    }

    function updatePetDisplay(pet) {
        document.getElementById('petNameDisplay').textContent = pet.name || '我的宠物';
        document.getElementById('petNameSpan').textContent = pet.name || '未设置';
        document.getElementById('petAgeSpan').textContent = pet.age || '-';
        document.getElementById('petBreedSpan').textContent = pet.breed || '-';
        document.getElementById('petHobbySpan').textContent = pet.hobby || '-';
        document.getElementById('petPersonalitySpan').textContent = pet.personality || '-';
        if (pet.avatar) {
            document.getElementById('petAvatarImg').src = '/images/' + pet.avatar;
        } else {
            document.getElementById('petAvatarImg').src = '/images/noerool.png';
        }
        const energy = pet.energy !== undefined ? pet.energy : 100;
        const clamped = Math.min(100, Math.max(0, energy));
        document.getElementById('energyFill').style.width = clamped + '%';
        document.getElementById('energyText').textContent = clamped + '%';
    }

    // ============================================================
    // 4. 宠物编辑（点击右卡片编辑图标）
    // ============================================================
    const petEditIcon = document.getElementById('petEditIcon');
    const petInfoDisplay = document.getElementById('petInfoDisplay');
    const petEditForm = document.getElementById('petEditForm');

    petEditIcon.addEventListener('click', function() {
        if (petEditForm.style.display === 'none') {
            // 填充当前值到表单
            document.getElementById('editPetName').value = document.getElementById('petNameSpan').textContent === '未设置' ? '' : document.getElementById('petNameSpan').textContent;
            const ageText = document.getElementById('petAgeSpan').textContent;
            document.getElementById('editPetAge').value = (ageText === '-' || ageText === '') ? '' : ageText;
            document.getElementById('editPetBreed').value = document.getElementById('petBreedSpan').textContent === '-' ? '' : document.getElementById('petBreedSpan').textContent;
            document.getElementById('editPetHobby').value = document.getElementById('petHobbySpan').textContent === '-' ? '' : document.getElementById('petHobbySpan').textContent;
            document.getElementById('editPetPersonality').value = document.getElementById('petPersonalitySpan').textContent === '-' ? '' : document.getElementById('petPersonalitySpan').textContent;
            petInfoDisplay.style.display = 'none';
            petEditForm.style.display = 'block';
        } else {
            hidePetEdit();
        }
    });

    window.savePetInfo = function() {
        const name = document.getElementById('editPetName').value.trim();
        const age = parseInt(document.getElementById('editPetAge').value) || 0;
        const breed = document.getElementById('editPetBreed').value.trim();
        const hobby = document.getElementById('editPetHobby').value.trim();
        const personality = document.getElementById('editPetPersonality').value.trim();
        if (!name) { alert('宠物名字不能为空'); return; }

        const petData = { name, age, breed, hobby, personality };
        // 如果已有宠物，保留原来的头像和体力（后端会处理）
        fetch('/api/pet', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(petData)
        })
            .then(res => {
                if (!res.ok) throw new Error('保存失败');
                return res.json();
            })
            .then(updated => {
                updatePetDisplay(updated);
                alert('宠物信息已保存！');
                hidePetEdit();
            })
            .catch(err => alert('保存失败：' + err.message));
    };

    window.hidePetEdit = function() {
        petEditForm.style.display = 'none';
        petInfoDisplay.style.display = 'block';
    };

    // ============================================================
    // 5. 宠物头像上传（转base64并存储到后端）
    // ============================================================
    window.uploadPetAvatar = function(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = function(e) {
            const base64 = e.target.result;
            // 1. 先显示预览
            document.getElementById('petAvatarImg').src = base64;
            // 2. 发送给后端保存（可选）
            fetch('/api/pet/avatar', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ avatar: base64 })  // 或只传文件名，但这里我们简单传base64
            })
                .then(res => {
                    if (!res.ok) throw new Error('上传头像失败');
                    return res.json();
                })
                .then(data => {
                    // 后端返回新的宠物信息，更新显示
                    if (data.avatar) {
                        document.getElementById('petAvatarImg').src = '/images/' + data.avatar;
                    }
                    alert('头像上传成功！');
                })
                .catch(err => alert('头像上传失败：' + err.message));
        };
        reader.readAsDataURL(file);
        // 重置input以便重复上传同一文件
        event.target.value = '';
    };

    // ============================================================
    // 6. 原有卡片点击弹窗（排除点击头像和编辑图标）
    // ============================================================
    const cards = document.querySelectorAll('.card');
    cards.forEach(card => {
        card.addEventListener('click', function(e) {
            if (e.target.closest('.avatar-img') || e.target.closest('.avatar-container') ||
                e.target.closest('.edit-icon') || e.target.closest('.avatar-picker-overlay') ||
                e.target.closest('button') || e.target.closest('input')) {
                return;
            }
            const messages = [
                '☕ 生活很苦，自己加糖 ~',
                '🌸 你笑起来就是好天气',
                '🌻 永远热爱，永远浪漫',
                '🍂 慢慢来，会好的',
                '✨ 今天也是限量版的一天'
            ];
            alert('💛 ' + messages[Math.floor(Math.random() * messages.length)]);
        });
    });

    // ============================================================
    // 7. 时钟（在个人卡片底部添加）
    // ============================================================
    const infoFooter = document.querySelector('.info-card .footer');
    if (infoFooter) {
        const timeSpan = document.createElement('div');
        timeSpan.style.marginTop = '8px';
        timeSpan.style.fontSize = '13px';
        timeSpan.style.opacity = '0.7';
        timeSpan.style.color = '#a88462';
        timeSpan.id = 'clock-display';
        infoFooter.appendChild(timeSpan);
        function updateClock() {
            const now = new Date();
            document.getElementById('clock-display').textContent =
                `🕰️ 当前时间：${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}:${String(now.getSeconds()).padStart(2,'0')}`;
        }
        updateClock();
        setInterval(updateClock, 1000);
    }

    // ============================================================
    // 8. 彩蛋（问候语悬停）
    // ============================================================
    const greeting = document.querySelector('.greeting');
    if (greeting) {
        const orig = greeting.textContent.trim();
        const hoverTexts = ['✨ 抱抱你', '💛 你超棒', '🌟 好运来', '🍀 加油鸭'];
        let idx = 0;
        greeting.addEventListener('mouseenter', function() {
            this.textContent = hoverTexts[idx % hoverTexts.length];
            idx++;
        });
        greeting.addEventListener('mouseleave', function() {
            this.textContent = orig;
        });
    }

    // ============================================================
    // 9. 装饰小点（pulse动画）
    // ============================================================
    document.querySelectorAll('.label').forEach(label => {
        const dot = document.createElement('span');
        dot.textContent = ' ●';
        dot.style.color = '#eaceb0';
        dot.style.fontSize = '12px';
        dot.style.animation = 'pulse 1.5s infinite';
        label.appendChild(dot);
    });
    const styleSheet = document.createElement('style');
    styleSheet.textContent = `
        @keyframes pulse {
            0% { opacity: 0.3; }
            50% { opacity: 1; }
            100% { opacity: 0.3; }
        }
    `;
    document.head.appendChild(styleSheet);

    // ============================================================
    // 10. 初始化加载宠物数据
    // ============================================================
    loadPet();

});
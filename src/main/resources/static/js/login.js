async function handleLogin(event) {
    event.preventDefault();

    const account = document.getElementById('loginAccount').value.trim();
    const password = document.getElementById('password').value.trim();
    const errorMessage = document.getElementById('errorMessage');

    if (!account || !password) {
        errorMessage.textContent = '请填写完整信息';
        errorMessage.style.display = 'block';
        return false;
    }

    // 根据当前选中的方式构建请求体
    const method = currentMethod; // 'email' 或 'phone'（在全局定义）
    const payload = {
        password: password
    };
    if (method === 'email') {
        payload.email = account;
    } else {
        payload.tel = account;
    }

    try {
        const response = await fetch('/', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('userInfo', JSON.stringify(data));
            window.location.href = '/wehome';
            return false;
        }

        const error = await response.json();
        errorMessage.textContent = error.message || '登录失败，请检查账号和密码。';
        errorMessage.style.display = 'block';
    } catch (error) {
        errorMessage.textContent = '网络好像开小差了，请稍后再试。';
        errorMessage.style.display = 'block';
    }

    return false;
}
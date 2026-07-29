async function handleLogin(event) {
    event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');

    try {
        const response = await fetch('/', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('userInfo', JSON.stringify(data));
            window.location.href = '/chat';
            return false; // 阻止继续执行
        }

        // 只有登录失败才会执行到这里
        const error = await response.json();
        errorMessage.textContent = error.message || '登录失败啦，请检查用户名和密码。';
        errorMessage.style.display = 'block';
    } catch (error) {
        errorMessage.textContent = '网络好像开小差了，请稍后再试。';
        errorMessage.style.display = 'block';
    }

    return false;
}
const FLOOR_BOUNDS = {
    1: { left: 15, top: 68, width: 70, height: 25 },
    2: { left: 15, top: 40, width: 70, height: 25 },
    3: { left: 15, top: 15, width: 70, height: 22 }
};

let currentView = 'overview';
let isNight = false;
let lastInteractTime = Date.now();

function viewOverview() {
    currentView = 'overview';
    document.getElementById('world').className = 'world view-overview';
    updateStatus('总览模式');
}

function viewFloor(n) {
    currentView = 'floor' + n;
    document.getElementById('world').className = 'world view-floor' + n;
    updateStatus('查看第 ' + n + ' 层');
}

function toggleDayNight() {
    isNight = !isNight;
    const stage = document.getElementById('stage');
    const btn = document.getElementById('btn-daynight');
    if (isNight) {
        stage.classList.add('night');
        btn.textContent = '☀️ 切换白天';
    } else {
        stage.classList.remove('night');
        btn.textContent = '🌙 切换夜晚';
    }
    updateStatus(isNight ? '夜晚' : '白天');
}

function updateStatus(text) {
    document.getElementById('status-bar').textContent = '当前：' + text;
}

class Character {
    constructor(id, floor, x, y) {
        this.el = document.getElementById(id);
        this.floor = floor;
        this.x = x; this.y = y;
        this.targetX = x; this.targetY = y;
        this.speed = 0.15;
        this.el.addEventListener('mouseenter', () => this.onHover());
        this.el.addEventListener('click', () => this.onInteract());
        this.updatePosition();
    }

    moveRandom() {
        const b = FLOOR_BOUNDS[this.floor];
        this.targetX = b.left + 5 + Math.random() * (b.width - 10);
        this.targetY = b.top + 5 + Math.random() * (b.height - 15);
    }

    update() {
        this.x += (this.targetX - this.x) * this.speed;
        this.y += (this.targetY - this.y) * this.speed;
        if (Math.abs(this.targetX - this.x) < 0.5 && Math.abs(this.targetY - this.y) < 0.5) {
            if (Math.random() < 0.02) this.moveRandom();
        }
        this.updatePosition();
    }

    updatePosition() {
        this.el.style.left = this.x + '%';
        this.el.style.top = this.y + '%';
    }

    onHover() {
        this.setMood('happy');
        this.el.classList.add('show-mood');
        setTimeout(() => this.el.classList.remove('show-mood'), 1500);
        resetIdle();
    }

    onInteract() {
        this.setMood('happy');
        resetIdle();
        this.el.style.transform = 'translateY(-10px)';
        setTimeout(() => this.el.style.transform = '', 200);
    }

    setMood(m) {
        this.el.classList.remove('happy', 'angry', 'hungry', 'show-mood');
        const bubble = this.el.querySelector('.mood-bubble');
        if (m === 'happy') { this.el.classList.add('happy'); bubble.textContent = '😊 开心'; }
        else if (m === 'angry') { this.el.classList.add('angry'); bubble.textContent = '😠 生气'; }
        else if (m === 'hungry') { this.el.classList.add('hungry'); bubble.textContent = '😫 饿了'; }
    }
}

const chars = [
    new Character('char-cat', 1, 30, 75),
    new Character('char-dog', 2, 50, 48)
];

function resetIdle() {
    lastInteractTime = Date.now();
    chars.forEach(c => { if (c.mood !== 'happy') c.setMood('happy'); });
}

setInterval(() => {
    if ((Date.now() - lastInteractTime) / 1000 > 10) {
        chars.forEach(c => {
            if (c.mood === 'happy') {
                c.setMood(Math.random() > 0.5 ? 'angry' : 'hungry');
            }
        });
    }
}, 1000);

function animate() {
    chars.forEach(c => c.update());
    requestAnimationFrame(animate);
}

window.onload = () => {
    viewOverview();
    chars.forEach(c => c.moveRandom());
    animate();
};

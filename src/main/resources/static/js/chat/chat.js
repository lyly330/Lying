let chatContainer = document.getElementById("chatContainer");
let userInput = document.getElementById("userInput");
let sendButton = document.getElementById("sendButton");

sendButton.addEventListener("click", sendMsg);
userInput.addEventListener("keypress", function (event) {
    if (event.keyCode === 13 && !event.shiftKey) {
        event.preventDefault();
        sendMsg(event);
    }
});

const token = localStorage.getItem('token');
const userInfo = localStorage.getItem('userInfo');

function sendMsg(event) {
    const msg = userInput.value.trim();
    if (!msg) {
        addMessage("先写点内容吧，我已经准备好帮你啦。", 'ASSISTANT', ' ');
        scrollToBottom();
        return;
    }

    addMessage(msg, 'USER', '我');
    userInput.value = '';
    showTypingIndicator();

    let contextDiv = '';
    const eventSource = new EventSource(`/chat/chat?id=${JSON.parse(userInfo).id}&msg=${encodeURIComponent(msg)}`, {});
    eventSource.onopen = function () {
        removeTypingIndicator();
        contextDiv = addMessage('', 'ASSISTANT', ' ');
    };
    eventSource.onmessage = function (event) {
        contextDiv.innerHTML += event.data;
        scrollToBottom();
    };
    eventSource.onerror = function (event) {
        eventSource.close();
        event.preventDefault();
    };
}

function addMessage(message, type, role) {
    const messageContainer = document.createElement('div');
    messageContainer.classList.add('message', type);
    messageContainer.innerHTML = `
        <div class="message-header">${role}：</div>
        <div class="message-content">
            <div class="message-text">${message}</div>
        </div>
    `;
    chatContainer.appendChild(messageContainer);
    return messageContainer.querySelector('.message-text');
}

function showTypingIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'message ASSISTANT typing-indicator';
    indicator.id = 'typingIndicator';
    indicator.innerHTML = `
        <div class="message-header">我正在认真想...</div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
    `;
    chatContainer.appendChild(indicator);
    scrollToBottom();
}

function removeTypingIndicator() {
    let indicator = document.getElementById('typingIndicator');
    if (indicator) {
        indicator.remove();
    }
}

async function showMessageHistory() {
    const response = await fetch(`/chat/messageHistory?id=${JSON.parse(userInfo).id}`);
    const data = await response.json();
    for (let datum of data) {
        let role = datum.messageType === "USER" ? "我" : " ";
        addMessage(datum.text, datum.messageType, role);
    }
    scrollToBottom();
}

function scrollToBottom() {
    window.scrollTo({
        top: document.body.scrollHeight,
        behavior: 'smooth'
    });
}

showMessageHistory();

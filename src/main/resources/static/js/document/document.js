let filesToUpload = new Map();

const uploadSection = document.getElementById('uploadSection');
const fileList = document.getElementById('fileList');
const clearAllBtn = document.getElementById('clearAllBtn');
const uploadAllBtn = document.getElementById('uploadAllBtn');

uploadSection.addEventListener('dragover', (event) => {
    event.preventDefault();
    uploadSection.classList.add('dragover');
});

uploadSection.addEventListener('dragleave', () => {
    uploadSection.classList.remove('dragover');
});

uploadSection.addEventListener('drop', (event) => {
    event.preventDefault();
    uploadSection.classList.remove('dragover');
    handleFiles(event.dataTransfer.files);
});

function handleFileSelect(input) {
    if (input.files.length > 0) {
        handleFiles(input.files);
    }
}

function handleFiles(files) {
    Array.from(files).forEach(file => {
        const allowedTypes = [
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'text/plain'
        ];

        if (!allowedTypes.includes(file.type)) {
            alert(`文件 ${file.name} 暂不支持，请上传 PDF、Word 或 TXT 文件`);
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            alert(`文件 ${file.name} 超过 10MB 啦，请换一个小一点的文件`);
            return;
        }

        if (filesToUpload.has(file.name)) {
            alert(`文件 ${file.name} 已经在列表里啦`);
            return;
        }

        filesToUpload.set(file.name, file);
        addFileToList(file);
        clearAllBtn.style.display = 'block';
    });
}

function addFileToList(file) {
    const fileItem = document.createElement('div');
    fileItem.className = 'file-item';
    fileItem.innerHTML = `
        <div class="file-name">${file.name}</div>
        <div class="file-size">${formatFileSize(file.size)}</div>
        <div class="file-status status-pending">待上传</div>
        <button type="button" class="remove-file" onclick="removeFile('${file.name}')" aria-label="移除文件">
            <i class="bi bi-x-circle"></i>
        </button>
        <div class="upload-progress">
            <div class="progress-bar" id="progress-${file.name}"></div>
        </div>
    `;
    fileList.appendChild(fileItem);
}

function removeFile(fileName) {
    filesToUpload.delete(fileName);
    const fileItem = Array.from(fileList.children).find(item =>
        item.querySelector('.file-name').textContent === fileName
    );
    if (fileItem) {
        fileItem.remove();
    }
    if (filesToUpload.size === 0) {
        clearAllBtn.style.display = 'none';
    }
}

function clearAllFiles() {
    if (confirm('确定要清除所有待上传文件吗？')) {
        filesToUpload.clear();
        fileList.innerHTML = '';
        clearAllBtn.style.display = 'none';
    }
}

function formatFileSize(bytes) {
    if (!bytes) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

async function uploadAllFiles() {
    if (filesToUpload.size === 0) {
        alert('先选择要上传的文件吧');
        return;
    }

    uploadAllBtn.disabled = true;
    uploadAllBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 上传中...';

    const formData = new FormData();
    for (const [, file] of filesToUpload) {
        formData.append('file', file);
    }

    try {
        const response = await fetch('/document/upload', {
            method: 'POST',
            body: formData
        });

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        const result = await response.text();
        result.split('\n').forEach(message => {
            if (!message.trim()) {
                return;
            }
            const fileName = message.split(' ')[0];
            const status = message.includes('成功') ? 'success' : 'error';
            updateFileStatus(fileName, status, status === 'success' ? '已入库' : '上传失败');
        });
        await loadStoredDocuments();
    } catch (error) {
        console.error('Upload error:', error);
        for (const [fileName] of filesToUpload) {
            updateFileStatus(fileName, 'error', '上传失败');
        }
    } finally {
        uploadAllBtn.disabled = false;
        uploadAllBtn.innerHTML = '<i class="bi bi-cloud-upload"></i> 开始上传';
    }
}

function updateFileStatus(fileName, status, message) {
    const fileItem = Array.from(fileList.children).find(item =>
        item.querySelector('.file-name').textContent === fileName
    );
    if (fileItem) {
        const statusDiv = fileItem.querySelector('.file-status');
        statusDiv.className = `file-status status-${status}`;
        statusDiv.textContent = message;
    }
}

function checkLoginStatus() {
    const userInfo = localStorage.getItem('userInfo');
    if (!userInfo) {
        window.location.href = '/login';
    }
}

async function loadStoredDocuments() {
    const container = document.getElementById('storedDocumentList');
    if (!container) {
        return;
    }

    try {
        const response = await fetch('/document/list');

        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }

        const documents = await response.json();
        if (!documents.length) {
            container.innerHTML = '<div class="empty-documents">还没有入库资料，上传一份试试看吧。</div>';
            return;
        }

        container.innerHTML = documents.map(document => `
            <div class="stored-document-item">
                <div class="stored-document-name">${document.filename}</div>
                <div class="stored-document-meta">${formatFileSize(document.fileSize || 0)} · ${formatDate(document.createdAt)}</div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Load documents error:', error);
        container.innerHTML = '<div class="empty-documents">数据库资料暂时读取失败，请稍后再试。</div>';
    }
}

function formatDate(value) {
    if (!value) {
        return '刚刚';
    }
    return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

checkLoginStatus();
uploadAllBtn.onclick = uploadAllFiles;
loadStoredDocuments();

// API基础路径
const API_BASE = '';

// 获取存储的token
function getToken() {
    return localStorage.getItem('token') || '';
}

// 获取当前用户信息
function getCurrentUser() {
    return {
        userId: localStorage.getItem('userId') || '',
        username: localStorage.getItem('username') || '',
        nickname: localStorage.getItem('nickname') || '',
        avatar: localStorage.getItem('avatar') || '',
        role: parseInt(localStorage.getItem('role') || '0')
    };
}

// 是否已登录
function isLoggedIn() {
    return !!getToken();
}

// 是否管理员
function isAdmin() {
    return getCurrentUser().role === 2;
}

// 是否运营
function isOperator() {
    const role = getCurrentUser().role;
    return role === 1 || role === 2;
}

// 保存登录信息
function saveLoginInfo(data) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('userId', data.userId);
    localStorage.setItem('username', data.username);
    localStorage.setItem('nickname', data.nickname || '');
    localStorage.setItem('avatar', data.avatar || '');
    localStorage.setItem('role', data.role);
}

// 退出登录
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('nickname');
    localStorage.removeItem('avatar');
    localStorage.removeItem('role');
    window.location.href = '/page/login';
}

// 封装请求
async function request(url, options = {}) {
    const token = getToken();
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    const response = await fetch(API_BASE + url, { ...options, headers });
    if (!response.ok) {
        if (response.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/page/login';
            throw new Error('登录已过期');
        }
        const err = await response.json().catch(() => ({ message: '请求失败' }));
        throw new Error(err.message || '请求失败');
    }
    const res = await response.json();
    if (res.code !== 200) {
        if (res.code === 401) {
            localStorage.removeItem('token');
            window.location.href = '/page/login';
        }
        throw new Error(res.message || '请求失败');
    }
    return res.data;
}

// GET请求
function get(url) {
    return request(url, { method: 'GET' });
}

// POST请求
function post(url, data) {
    return request(url, { method: 'POST', body: JSON.stringify(data) });
}

// PUT请求
function put(url, data) {
    return request(url, { method: 'PUT', body: JSON.stringify(data || {}) });
}

// DELETE请求
function del(url) {
    return request(url, { method: 'DELETE' });
}

// 文件上传请求
async function postFile(url, formData) {
    const token = getToken();
    const headers = { ...(formData instanceof FormData ? {} : { 'Content-Type': 'application/json' }) };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    const response = await fetch(API_BASE + url, { method: 'POST', body: formData, headers });
    if (!response.ok) {
        if (response.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/page/login';
            throw new Error('登录已过期');
        }
        const err = await response.json().catch(() => ({ message: '请求失败' }));
        throw new Error(err.message || '请求失败');
    }
    const res = await response.json();
    if (res.code !== 200) {
        if (res.code === 401) {
            localStorage.removeItem('token');
            window.location.href = '/page/login';
        }
        throw new Error(res.message || '请求失败');
    }
    return res.data;
}

// 格式化时间
function formatTime(time) {
    if (!time) return '';
    return time.replace('T', ' ').substring(0, 19);
}

// 格式化金额
function formatMoney(amount) {
    return '¥' + (parseFloat(amount) || 0).toFixed(2);
}

// 获取URL参数
function getQueryParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

// 活动状态文本
function activityStatusText(status) {
    const map = { 0: '待审核', 1: '审核通过', 2: '已驳回', 3: '已上架', 4: '已下架' };
    return map[status] || '未知';
}

// 活动状态标签类型
function activityStatusType(status) {
    const map = { 0: 'info', 1: '', 2: 'danger', 3: 'success', 4: 'warning' };
    return map[status] || 'info';
}

// 订单状态文本
function orderStatusText(status) {
    const map = { 0: '待支付', 1: '已完成', 2: '已取消', 3: '已过期' };
    return map[status] || '未知';
}

// 订单状态标签类型
function orderStatusType(status) {
    const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' };
    return map[status] || 'info';
}

// 消息类型
function messageTypeText(type) {
    const map = { 1: '系统通知', 2: '预约通知', 3: '订单通知', 4: '活动通知', 5: '平台公告' };
    return map[type] || '系统消息';
}

// 规范化图片URL（处理空格、换行、路径前缀）
function normalizeImageUrl(url) {
    if (!url) return '';
    const cleaned = url.trim().split(/\s+/)[0];
    if (cleaned.startsWith('http://') || cleaned.startsWith('https://')) {
        return cleaned;
    }
    return cleaned.startsWith('/') ? cleaned : '/' + cleaned;
}

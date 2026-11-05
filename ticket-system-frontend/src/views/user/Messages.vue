<template>
  <el-card shadow="never" class="messages-card">
    <template #header>
      <div class="header-row">
        <span class="card-title">消息中心</span>
        <div class="header-actions">
          <el-button type="primary" text size="small" @click="handleMarkAllRead">全部已读</el-button>
          <el-button type="danger" text size="small" @click="handleClear">清空消息</el-button>
        </div>
      </div>
    </template>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="未读" name="unread" />
      <el-tab-pane label="已读" name="read" />
    </el-tabs>

    <div v-loading="loading" class="message-body">
    <div v-if="messages.length" class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="{ unread: !msg.isRead }"
        @click="handleRead(msg)"
      >
        <div class="message-left">
          <span v-if="!msg.isRead" class="unread-dot"></span>
          <el-tag :type="typeTagType(msg.type)" size="small" class="msg-type">
            {{ typeText(msg.type) }}
          </el-tag>
        </div>
        <div class="message-content">
          <div class="message-title" :class="{ 'bold': !msg.isRead }">{{ msg.title }}</div>
          <div class="message-text">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
    </div>

    <!-- 加载中不显示空状态，避免"暂无消息"误导 -->
    <el-empty v-else-if="!loading" :description="activeTab === 'unread' ? '没有未读消息' : '暂无消息'" />

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchMessages"
      />
    </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMessages, markAsRead, markAllRead, clearMessages } from '@/api/message'
import { ElMessage, ElMessageBox } from 'element-plus'

const messages = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('all')
const loading = ref(false)

const typeMap = {
  0: '系统通知',
  1: '抢票提醒',
  2: '排队更新',
  3: '订单通知',
  4: '活动通知'
}

const typeTagTypeMap = {
  0: '',
  1: 'warning',
  2: 'success',
  3: 'danger',
  4: 'info'
}

const typeText = (type) => typeMap[type] || '系统通知'
const typeTagType = (type) => typeTagTypeMap[type] || ''

const formatTime = (time) => {
  if (!time) return ''
  return String(time).replace('T', ' ').substring(0, 16)
}

const fetchMessages = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value
    }
    if (activeTab.value === 'unread') {
      params.isRead = false
    } else if (activeTab.value === 'read') {
      params.isRead = true
    }
    const res = await getMessages(params)
    const records = res.data.records || res.data.list || []
    messages.value = records
    total.value = res.data.total || 0
    // 当前页读完后变空，自动回退一页
    if (!records.length && total.value > 0 && page.value > 1) {
      page.value -= 1
      return fetchMessages()
    }
  } catch {
    // 错误提示由 axios 拦截器统一弹出；这里清空列表并停止 loading，
    // 避免把"加载失败"误显示成"暂无消息"
    messages.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  page.value = 1
  fetchMessages()
}

const handleRead = async (msg) => {
  if (msg.isRead) return
  try {
    await markAsRead(msg.id)
    msg.isRead = true
  } catch {
    // 静默处理
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllRead()
    ElMessage.success('已全部标记为已读')
    fetchMessages()
  } catch {
    // 静默处理
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有消息吗？此操作不可撤销。', '清空确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearMessages()
    ElMessage.success('消息已清空')
    fetchMessages()
  } catch {
    // 取消或失败
  }
}

onMounted(() => {
  fetchMessages()
})
</script>

<style scoped>
.messages-card {
  min-height: 400px;
}

.message-body {
  min-height: 200px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.message-list {
  display: flex;
  flex-direction: column;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.message-item:hover {
  background: #f5f7fa;
}

.message-item.unread {
  background: #ecf5ff;
}

.message-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-top: 2px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  flex-shrink: 0;
}

.msg-type {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.message-title.bold {
  font-weight: 700;
}

.message-text {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  line-height: 1.5;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .messages-card {
    min-height: 0;
  }

  .header-row {
    flex-wrap: wrap;
    gap: 6px;
  }

  .header-actions {
    gap: 2px;
  }

  .message-item {
    padding: 12px 10px;
    gap: 8px;
  }

  .message-title {
    font-size: 13px;
  }

  .message-text {
    font-size: 12px;
  }

  .pagination-wrapper {
    overflow-x: auto;
  }
}
</style>

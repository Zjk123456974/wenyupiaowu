<template>
  <el-card shadow="never" class="waitlist-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">候补排队</span>
        <el-tag type="info" size="small">按候补时间顺序自动补票</el-tag>
      </div>
    </template>

    <div v-loading="loading" class="wait-body">
    <div v-if="waitList.length" class="wait-list">
      <el-card
        v-for="item in waitList"
        :key="item.activityId"
        shadow="hover"
        class="wait-item"
      >
        <div class="wait-item-body">
          <div class="wait-cover" @click="goDetail(item.activityId)">
            <el-image :src="item.coverImage || defaultCover" fit="cover" lazy>
              <template #error>
                <div class="wait-cover-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <div class="wait-info">
            <div class="wait-title" @click="goDetail(item.activityId)">{{ item.activityTitle }}</div>
            <div class="wait-meta">
              <el-icon><Location /></el-icon>
              <span>{{ item.venue }}</span>
            </div>
            <div class="wait-meta">
              <el-icon><Calendar /></el-icon>
              <span>{{ formatTime(item.startTime) }}</span>
            </div>
          </div>
          <div class="wait-position">
            <div class="position-badge">
              <span class="position-label">当前排位</span>
              <span class="position-value">第 {{ item.queuePosition }} 位</span>
            </div>
            <div class="position-total">共 {{ item.totalWaiters }} 人候补</div>
            <div class="position-tip">有退票时按顺序自动补票</div>
          </div>
          <div class="wait-action">
            <el-button type="danger" plain size="default" @click="handleLeave(item.activityId)">
              退出候补
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-else-if="!loading" description="暂无候补记录">
      <template #description>
        <div class="empty-tip">
          <p>您还没有候补任何活动</p>
          <p style="color: #909399; font-size: 13px; margin-top: 4px;">热门活动售罄后可加入候补，有退票时按顺序自动补票</p>
        </div>
      </template>
    </el-empty>

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchWaitList"
      />
    </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyWaitList, leaveWaitQueue } from '@/api/waitlist'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Calendar, Picture } from '@element-plus/icons-vue'
import { PLACEHOLDER_IMAGE } from '@/utils/placeholder'

const router = useRouter()

const defaultCover = PLACEHOLDER_IMAGE

const waitList = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
let refreshTimer = null

const formatTime = (time) => {
  if (!time) return ''
  return String(time).replace('T', ' ').substring(0, 16)
}

const fetchWaitList = async () => {
  loading.value = true
  try {
    // 后端参数名是 size（不是 pageSize），传错会导致每页条数永远走默认值
    const res = await getMyWaitList({
      page: page.value,
      size: pageSize.value
    })
    waitList.value = res.data?.records || res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    waitList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleLeave = async (activityId) => {
  try {
    await ElMessageBox.confirm('确定要退出该活动的候补队列吗？', '退出确认', {
      confirmButtonText: '确定退出',
      cancelButtonText: '再等等',
      type: 'warning'
    })
    await leaveWaitQueue(activityId)
    ElMessage.success('已退出候补队列')
    fetchWaitList()
  } catch {
    // 用户取消
  }
}

const goDetail = (activityId) => {
  router.push(`/activity/${activityId}`)
}

// 每30秒刷新一次候补位置
const startRefresh = () => {
  stopRefresh()
  refreshTimer = setInterval(() => {
    fetchWaitList()
  }, 30000)
}

const stopRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  fetchWaitList()
  startRefresh()
})

onUnmounted(() => {
  stopRefresh()
})
</script>

<style scoped>
.waitlist-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.wait-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wait-item {
  border-radius: 12px;
  transition: all 0.25s ease;
}

.wait-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.wait-item :deep(.el-card__body) {
  padding: 16px 20px;
}

.wait-item-body {
  display: flex;
  align-items: center;
  gap: 20px;
}

.wait-cover {
  width: 120px;
  height: 75px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.wait-cover:hover {
  transform: scale(1.05);
}

.wait-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wait-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
  display: block;
}

/* 封面加载失败兜底 */
.wait-cover-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 22px;
}

.wait-info {
  flex: 1;
  min-width: 0;
}

.wait-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}

.wait-title:hover {
  color: var(--primary-color, #409eff);
}

.wait-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.wait-meta .el-icon {
  font-size: 14px;
}

.wait-position {
  text-align: center;
  flex-shrink: 0;
  min-width: 140px;
}

.position-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 16px;
  background: linear-gradient(135deg, #fff7f8, #fff0e8);
  border-radius: 10px;
  border: 1px solid #ffd6cc;
}

.position-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.position-value {
  font-size: 22px;
  font-weight: 800;
  color: #ff4d6d;
}

.position-total {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.position-tip {
  margin-top: 2px;
  font-size: 11px;
  color: #c0c4cc;
}

.wait-action {
  flex-shrink: 0;
}

.empty-tip {
  text-align: center;
  color: #606266;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .waitlist-card {
    min-height: 0;
  }

  .card-header {
    flex-wrap: wrap;
    gap: 6px;
  }

  /* 横排（封面+信息+排位+按钮）在窄屏会溢出，改为两行结构 */
  .wait-item-body {
    flex-wrap: wrap;
    gap: 12px;
  }

  .wait-cover {
    width: 88px;
    height: 60px;
  }

  .wait-info {
    flex: 1 1 60%;
  }

  .wait-title {
    font-size: 14px;
  }

  .wait-position {
    flex: 1 1 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
    text-align: left;
  }

  .position-badge {
    flex-direction: row;
    gap: 6px;
    padding: 6px 10px;
  }

  .position-value {
    font-size: 16px;
  }

  .position-total {
    margin-top: 0;
  }

  .position-tip {
    display: none;
  }

  .wait-action {
    flex: 1 1 100%;
  }

  .wait-action :deep(.el-button) {
    width: 100%;
  }

  .pagination-wrapper {
    overflow-x: auto;
  }
}
</style>

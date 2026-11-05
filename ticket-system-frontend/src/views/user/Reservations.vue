<template>
  <el-card shadow="never" class="reservations-card">
    <template #header>
      <span class="card-title">我的预约</span>
    </template>

    <div v-loading="loading" class="reservation-body-wrap">
    <div v-if="reservations.length" class="reservation-list">
      <el-card
        v-for="item in reservations"
        :key="item.id"
        shadow="hover"
        class="reservation-item"
      >
        <div class="reservation-body">
          <div class="reservation-info">
            <div class="activity-name">{{ item.activityTitle }}</div>
            <div class="reservation-time">
              <el-icon><Clock /></el-icon>
              <span>预约时间：{{ formatTime(item.createTime) }}</span>
            </div>
          </div>
          <div class="queue-info">
            <div class="queue-number">
              <span class="queue-label">排队序号</span>
              <span class="queue-value">{{ item.queueNumber }}</span>
            </div>
            <div class="queue-total">总排队人数：{{ item.totalQueueCount }}</div>
            <div class="queue-seat" v-if="item.seatType">预约票档：{{ item.seatType }}</div>
          </div>
          <div class="reservation-status">
            <el-tag :type="statusTagType(item.status)" size="large">
              {{ statusText(item.status) }}
            </el-tag>
          </div>
          <div class="reservation-action">
            <el-button
              v-if="item.status === 0"
              type="danger"
              plain
              size="small"
              @click="handleCancel(item.activityId)"
            >取消预约</el-button>
          </div>
        </div>
        <div class="reservation-countdown" v-if="item.status === 0">
          <div class="countdown-label">距离开售</div>
          <div class="countdown-value">{{ getSaleCountdown(item.grabStartTime) }}</div>
          <div class="countdown-tip">开售前请提前进入活动页等待，开抢后系统将优先出票</div>
        </div>
      </el-card>
    </div>

    <el-empty v-else-if="!loading" description="暂无预约" />

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchReservations"
      />
    </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getMyReservations, cancelReservation } from '@/api/reservation'
import { ElMessage, ElMessageBox } from 'element-plus'

const reservations = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const nowTick = ref(Date.now())
let tickTimer = null

const statusMap = {
  0: '排队中',
  1: '已出票',
  2: '已取消'
}

const statusTagTypeMap = {
  0: 'warning',
  1: 'success',
  2: 'info'
}

const statusText = (status) => statusMap[status] || '未知'
const statusTagType = (status) => statusTagTypeMap[status] || 'info'

const formatTime = (time) => {
  if (!time) return ''
  return String(time).replace('T', ' ').substring(0, 16)
}

// 距离开售倒计时（天 D / 时 H / 分 M / 秒 S）
const getSaleCountdown = (grabStartTime) => {
  if (!grabStartTime) return '待定'
  const target = new Date(grabStartTime).getTime()
  if (isNaN(target)) return '待定'
  const diff = target - nowTick.value
  if (diff <= 0) return '已开售'
  const s = Math.floor(diff / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  return d > 0 ? `${d}天${pad(h)}:${pad(m)}:${pad(sec)}` : `${pad(h)}:${pad(m)}:${pad(sec)}`
}

const fetchReservations = async () => {
  loading.value = true
  try {
    // 后端参数名是 size（不是 pageSize），传错会导致每页条数永远走默认值
    const res = await getMyReservations({
      page: page.value,
      size: pageSize.value
    })
    reservations.value = res.data.records || res.data.list || []
    total.value = res.data.total || 0
  } catch {
    reservations.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleCancel = async (activityId) => {
  try {
    await ElMessageBox.confirm('确定要取消此预约吗？', '取消确认', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      type: 'warning'
    })
    await cancelReservation(activityId)
    ElMessage.success('预约已取消')
    fetchReservations()
  } catch {
    // 取消或失败
  }
}

onMounted(() => {
  fetchReservations()
  tickTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (tickTimer) clearInterval(tickTimer)
})
</script>

<style scoped>
.reservations-card {
  min-height: 400px;
}

.reservation-body-wrap {
  min-height: 180px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reservation-item {
  border-radius: 8px;
}

.reservation-item :deep(.el-card__body) {
  padding: 16px 20px;
}

.reservation-body {
  display: flex;
  align-items: center;
  gap: 24px;
}

/* 预约倒计时提醒 */
.reservation-countdown {
  margin: 12px 0 2px;
  padding: 10px 16px;
  background: linear-gradient(135deg, #fff7e6, #fff2d8);
  border: 1px solid #ffe6a8;
  border-radius: 8px;
  text-align: center;
}

.reservation-countdown .countdown-label {
  font-size: 12px;
  color: #b88230;
  margin-bottom: 4px;
}

.reservation-countdown .countdown-value {
  font-size: 24px;
  font-weight: 800;
  color: #e6a23c;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.reservation-countdown .countdown-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #c0a06a;
}

.reservation-info {
  flex: 1;
  min-width: 0;
}

.activity-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reservation-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.queue-info {
  text-align: center;
  flex-shrink: 0;
}

.queue-number {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.queue-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.queue-value {
  font-size: 28px;
  font-weight: 700;
  color: #e6a23c;
}

.queue-total {
  margin-top: 4px;
  font-size: 12px;
  color: #c0c4cc;
}

.queue-seat {
  margin-top: 2px;
  font-size: 12px;
  color: #e6a23c;
}

.reservation-status {
  flex-shrink: 0;
}

.reservation-action {
  flex-shrink: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .reservations-card {
    min-height: 0;
  }

  /* 卡片内四段横排改为纵向堆叠，避免窄屏挤压/溢出 */
  .reservation-body {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .queue-info,
  .reservation-status,
  .reservation-action {
    text-align: left;
  }

  .reservation-action :deep(.el-button) {
    width: 100%;
  }

  .reservation-countdown {
    padding: 8px 12px;
    margin-top: 10px;
  }

  .pagination-wrapper {
    overflow-x: auto;
  }
}
</style>

<template>
  <el-card shadow="never" class="orders-card">
    <template #header>
      <span class="card-title">我的订单</span>
    </template>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待支付" name="0" />
      <el-tab-pane label="已完成" name="1" />
      <el-tab-pane label="已取消" name="2" />
      <el-tab-pane label="已过期" name="3" />
      <el-tab-pane label="已退款" name="5" />
    </el-tabs>

    <!-- 首屏骨架屏：与真实订单卡片数量一致，避免加载完成后的布局跳动 -->
    <div v-if="loading && orders.length === 0" class="order-skeleton">
      <el-skeleton :count="5" animated>
        <template #template>
          <div class="skeleton-order">
            <el-skeleton-item variant="text" style="width: 40%" />
            <el-skeleton-item variant="image" style="width: 120px; height: 80px" />
            <el-skeleton-item variant="text" style="width: 55%" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else-if="orders.length" class="order-list" v-loading="loading">
      <el-card
        v-for="order in orders"
        :key="order.id"
        shadow="hover"
        class="order-item"
      >
        <div class="order-header">
          <div class="order-info">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ order.createTime }}</span>
          </div>
          <el-tag :type="statusTagType(order.status)" size="small">
            {{ statusText(order.status) }}
          </el-tag>
        </div>

        <div class="order-body">
          <div class="activity-info">
            <el-image
              class="activity-cover"
              :src="order.coverImage || defaultCover"
              fit="cover"
              lazy
            >
              <template #error>
                <div class="activity-cover activity-cover-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="activity-detail">
              <div class="activity-title">{{ order.activityTitle }}</div>
              <div class="activity-meta">
                <el-icon><Location /></el-icon>
                <span>{{ order.venue }}</span>
              </div>
              <div class="activity-meta">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(order.activityStartTime) }}</span>
              </div>
            </div>
          </div>
          <div class="order-summary">
            <div class="ticket-count">× {{ order.quantity }} 张</div>
            <div class="order-amount">¥ {{ order.totalAmount }}</div>
          </div>
        </div>

        <div class="order-footer">
          <div v-if="order.status === 0" class="countdown pending-countdown">
            <el-icon class="countdown-icon"><Clock /></el-icon>
            <span class="countdown-label">剩余支付时间</span>
            <span class="countdown-digits">{{ getCountdown(order.expireTime) }}</span>
            <span class="countdown-tip">超时自动取消</span>
          </div>
          <div class="order-actions">
            <el-button
              v-if="order.status === 0"
              class="pay-btn-grab"
              type="danger"
              size="default"
              @click="handlePay(order.id)"
            >立即支付</el-button>
            <el-button
              v-if="order.status === 0"
              @click="handleCancel(order.id)"
            >取消</el-button>
            <el-button
              v-if="order.status === 1"
              @click="handleViewTicket(order.id)"
            >查看电子票</el-button>
            <el-button
              v-if="order.status === 1"
              type="warning"
              @click="showRefundDialog(order)"
            >申请退票</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-else description="暂无订单" />

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchOrders"
      />
    </div>

    <!-- 电子票对话框 -->
    <el-dialog
      v-model="ticketDialogVisible"
      title="电子票务凭证"
      width="500px"
      destroy-on-close
    >
      <div v-loading="ticketLoading">
        <el-descriptions :column="1" border v-if="ticketData">
          <el-descriptions-item label="订单编号">{{ ticketData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="活动名称">{{ ticketData.activityTitle }}</el-descriptions-item>
          <el-descriptions-item label="举办地点">{{ ticketData.venue }}</el-descriptions-item>
          <el-descriptions-item label="活动时间">{{ ticketData.startTime }}</el-descriptions-item>
          <el-descriptions-item label="购票数量">{{ ticketData.quantity }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ ticketData.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ ticketData.payTime }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="ticketData && ticketData.tickets && ticketData.tickets.length" class="tickets-list">
          <div class="tickets-list-title">票券明细（共 {{ ticketData.tickets.length }} 张）</div>
          <div v-for="ticket in ticketData.tickets" :key="ticket.id" class="ticket-row">
            <div class="ticket-info">
              <span class="ticket-no">票号：{{ ticket.ticketNo }}</span>
              <span v-if="ticket.seatType" class="ticket-seat">{{ ticket.seatType }}</span>
              <span v-if="ticket.seatInfo" class="ticket-seat">座位：{{ ticket.seatInfo }}</span>
              <span v-if="ticket.attendeeName" class="ticket-attendee">{{ ticket.attendeeName }}</span>
            </div>
            <div class="ticket-verify">
              <span class="verify-code">{{ ticket.verifyCode }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="ticketDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退票申请对话框 -->
    <el-dialog
      v-model="refundDialogVisible"
      title="申请退票"
      width="500px"
      destroy-on-close
    >
      <el-alert
        v-if="refundForm.refundable === false"
        type="warning"
        :closable="false"
        show-icon
        class="refund-dialog-tip"
        :title="refundForm.blockReason || '本场活动暂不可退票'"
      />
      <template v-else>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="refund-dialog-tip"
          :title="refundForm.feeTierDesc || '实际退款将扣除手续费，具体金额以审核结果为准'"
        />
        <el-form :model="refundForm" label-width="100px">
          <el-form-item label="订单编号">
            <span>{{ refundForm.orderNo }}</span>
          </el-form-item>
          <el-form-item label="活动名称">
            <span>{{ refundForm.activityTitle }}</span>
          </el-form-item>
          <el-form-item label="订单金额">
            <span class="refund-amount">¥{{ formatMoney(refundForm.refundAmount) }}</span>
          </el-form-item>
          <el-form-item label="退票手续费">
            <span class="refund-fee">
              {{ refundForm.feeRate != null ? (Number(refundForm.feeRate) * 100).toFixed(0) : '0' }}%
            </span>
            <span class="refund-amount-tip">（¥{{ formatMoney(refundForm.feeAmount) }}）</span>
          </el-form-item>
          <el-form-item label="预计实退">
            <span class="refund-actual">¥{{ formatMoney(refundForm.actualAmount) }}</span>
            <span class="refund-amount-tip">（扣除手续费后到账金额）</span>
          </el-form-item>
          <el-form-item label="退票原因" required>
            <el-select v-model="refundForm.reason" placeholder="请选择退票原因" style="width: 100%">
              <el-option label="行程冲突，无法参加" value="行程冲突，无法参加" />
              <el-option label="身体不适，无法前往" value="身体不适，无法前往" />
              <el-option label="购票错误，需要退票" value="购票错误，需要退票" />
              <el-option label="其他原因" value="其他原因" />
            </el-select>
          </el-form-item>
          <el-form-item label="补充说明">
            <el-input
              v-model="refundForm.description"
              type="textarea"
              :rows="3"
              placeholder="请补充退票详细说明（选填）"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button
          v-if="refundForm.refundable !== false"
          type="warning"
          :loading="refundSubmitting"
          @click="handleSubmitRefund"
        >提交退票申请</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyOrders, payOrder, cancelOrder, getElectronicTicket } from '@/api/order'
import { applyRefund, previewRefund } from '@/api/refund'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Clock, Picture } from '@element-plus/icons-vue'
import { PLACEHOLDER_IMAGE } from '@/utils/placeholder'

const router = useRouter()

// 无封面时的占位图（内联 SVG，全站共享常量）
const defaultCover = PLACEHOLDER_IMAGE

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const formatMoney = (val) => {
  const num = Number(val || 0)
  return num.toFixed(2)
}

const activeTab = ref('all')
const orders = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 首屏加载态：用于骨架屏展示，翻页时仍用 v-loading 遮罩
const loading = ref(false)

let countdownTimer = null

const ticketDialogVisible = ref(false)
const ticketLoading = ref(false)
const ticketData = ref(null)

const refundDialogVisible = ref(false)
const refundSubmitting = ref(false)
const refundForm = ref({
  orderId: null,
  orderNo: '',
  activityTitle: '',
  refundable: true,
  blockReason: '',
  refundAmount: 0,
  feeRate: null,
  feeAmount: 0,
  actualAmount: 0,
  feeTierDesc: '',
  daysLeft: null,
  refundPolicy: 1,
  reason: '',
  description: ''
})

const statusMap = {
  0: '待支付',
  1: '已完成',
  2: '已取消',
  3: '已过期',
  5: '已退款'
}

const statusTagTypeMap = {
  0: 'warning',
  1: 'success',
  2: 'info',
  3: 'danger',
  5: 'warning'
}

const statusText = (status) => statusMap[status] || '未知'
const statusTagType = (status) => statusTagTypeMap[status] || 'info'

const getCountdown = (expireTime) => {
  if (!expireTime) return ''
  const diff = new Date(expireTime).getTime() - Date.now()
  if (diff <= 0) return '已过期'
  const minutes = Math.floor(diff / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)
  return `${minutes}分${seconds}秒`
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value
    }
    if (activeTab.value !== 'all') {
      params.status = parseInt(activeTab.value)
    }
    const res = await getMyOrders(params)
    orders.value = res.data.records || res.data.list || []
    total.value = res.data.total || 0
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  page.value = 1
  fetchOrders()
}

const handlePay = async (id) => {
  try {
    await ElMessageBox.confirm('确定要支付此订单吗？', '支付确认', {
      confirmButtonText: '确定支付',
      cancelButtonText: '取消',
      type: 'info'
    })
    await payOrder(id)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch {
    // 取消或失败
  }
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消此订单吗？', '取消确认', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      type: 'warning'
    })
    await cancelOrder(id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch {
    // 取消或失败
  }
}

const handleViewTicket = async (id) => {
  ticketDialogVisible.value = true
  ticketLoading.value = true
  ticketData.value = null
  try {
    const res = await getElectronicTicket(id)
    ticketData.value = res.data || res || null
  } catch {
    ElMessage.error('获取电子票信息失败')
  } finally {
    ticketLoading.value = false
  }
}

const showRefundDialog = async (order) => {
  // 退前透明展示手续费与实退金额（对标大麦“退票前明确告知”）
  refundDialogVisible.value = true
  try {
    const res = await previewRefund({ orderId: order.id })
    const d = res?.data || res || {}
    refundForm.value = {
      orderId: order.id,
      orderNo: order.orderNo,
      activityTitle: order.activityTitle || d.activityTitle || '',
      refundable: d.refundable,
      blockReason: d.reason,
      refundAmount: d.totalAmount != null ? d.totalAmount : (order.totalAmount || 0),
      feeRate: d.feeRate,
      feeAmount: d.feeAmount,
      actualAmount: d.actualAmount,
      feeTierDesc: d.feeTierDesc,
      daysLeft: d.daysLeft,
      refundPolicy: d.refundPolicy,
      reason: '',
      description: ''
    }
    if (d.refundable === false) {
      ElMessage.warning(d.reason || '该订单暂不可退票')
    }
  } catch {
    ElMessage.error('获取退票信息失败')
  }
}

const handleSubmitRefund = async () => {
  if (!refundForm.value.reason) {
    ElMessage.warning('请选择退票原因')
    return
  }
  refundSubmitting.value = true
  try {
    // 补充说明并入退票原因，接口仅接受 {orderId, reason}
    let reason = refundForm.value.reason
    if (refundForm.value.description && refundForm.value.description.trim()) {
      reason = `${reason}；${refundForm.value.description.trim()}`
    }
    await applyRefund({
      orderId: refundForm.value.orderId,
      reason
    })
    ElMessage.success('退票申请已提交，请等待审核')
    refundDialogVisible.value = false
    fetchOrders()
  } catch {
    ElMessage.error('退票申请提交失败')
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(() => {
  fetchOrders()
  countdownTimer = setInterval(() => {
    // 触发倒计时更新
    orders.value = [...orders.value]
  }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.orders-card {
  min-height: 400px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  border-radius: 12px;
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.order-item:hover {
  transform: translateY(-2px);
}

/* 待支付订单高亮 */
.order-item:has(.pending-countdown) {
  border: 1px solid #ffd6cc;
  box-shadow: 0 4px 16px rgba(255, 77, 109, 0.1);
}

.order-item :deep(.el-card__body) {
  padding: 16px 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.order-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.order-no {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.order-time {
  font-size: 13px;
  color: #909399;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.activity-info {
  display: flex;
  gap: 14px;
  flex: 1;
}

.activity-cover {
  width: 120px;
  height: 80px;
  border-radius: 6px;
  flex-shrink: 0;
}

.activity-detail {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.activity-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.activity-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.order-summary {
  text-align: right;
  flex-shrink: 0;
  margin-left: 20px;
}

.ticket-count {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.order-amount {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.countdown {
  font-size: 13px;
  color: #909399;
}

.countdown-time {
  color: #e6a23c;
  font-weight: 600;
}

/* 待支付订单倒计时增强样式 */
.pending-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #fff7f8, #fff0e8);
  border-radius: 8px;
  border: 1px solid #ffe0d6;
}

.countdown-icon {
  color: #ff7a45;
  font-size: 16px;
}

.countdown-label {
  font-size: 13px;
  color: #606266;
}

.countdown-digits {
  font-size: 18px;
  font-weight: 800;
  color: #ff4d6d;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
  padding: 2px 8px;
  background: rgba(255, 77, 109, 0.1);
  border-radius: 6px;
  min-width: 80px;
  text-align: center;
}

.countdown-tip {
  font-size: 12px;
  color: #c0c4cc;
}

.pay-btn-grab {
  background: var(--gradient-grab);
  border: none;
  font-weight: 700;
  padding: 0 20px;
  box-shadow: 0 4px 12px rgba(255, 77, 109, 0.3);
  transition: all 0.3s var(--ease-out);
}

.pay-btn-grab:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(255, 77, 109, 0.4);
  background: var(--gradient-grab-deep);
}

.order-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.verify-code {
  font-size: 16px;
  font-weight: bold;
  color: #F56C6C;
  letter-spacing: 1px;
  word-break: break-all;
}

.tickets-list {
  margin-top: 16px;
}

.tickets-list-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.ticket-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fafafa;
}

.ticket-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ticket-no {
  font-size: 13px;
  color: #606266;
  font-family: monospace;
}

.ticket-seat,
.ticket-attendee {
  font-size: 12px;
  color: #909399;
}

.ticket-verify {
  text-align: right;
}

.refund-dialog-tip {
  margin-bottom: 16px;
}

.refund-amount-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.refund-amount {
  font-size: 18px;
  font-weight: 600;
  color: #e6a23c;
}

/* 骨架屏：与真实订单卡片结构一致 */
.order-skeleton {
  padding: 12px 0;
}

.skeleton-order {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
}

/* 订单封面加载失败的兜底 */
.activity-cover-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 22px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .orders-card {
    min-height: 0;
  }

  .orders-card :deep(.el-card__body) {
    padding: 12px;
  }

  /* Tab 横向滚动，避免标签换行挤压 */
  .orders-card :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }

  .orders-card :deep(.el-tabs__item) {
    font-size: 13px;
    padding: 0 10px;
  }

  /* 订单头部：订单号与时间换行 */
  .order-header {
    flex-wrap: wrap;
    gap: 6px;
  }

  .order-info {
    flex-wrap: wrap;
    gap: 4px 12px;
  }

  .order-no {
    font-size: 13px;
  }

  .order-time {
    font-size: 12px;
  }

  /* 订单主体：封面与信息纵向排列 */
  .order-body {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 12px 0;
  }

  .activity-info {
    gap: 10px;
  }

  .activity-cover {
    width: 88px;
    height: 60px;
  }

  .activity-title {
    font-size: 14px;
  }

  /* 数量与金额：改为左对齐横排 */
  .order-summary {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    text-align: left;
    margin-left: 0;
    padding-top: 10px;
    border-top: 1px dashed #f0f0f0;
  }

  .order-amount {
    font-size: 18px;
  }

  /* 底部：倒计时与按钮组纵向堆叠，按钮撑满 */
  .order-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .pending-countdown {
    flex-wrap: wrap;
    padding: 8px 10px;
  }

  .countdown-digits {
    font-size: 16px;
  }

  .order-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .order-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .pagination-wrapper {
    overflow-x: auto;
  }

  /* 退票弹窗：表单项改为纵向排列 */
  .orders-card :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
  }
}
</style>

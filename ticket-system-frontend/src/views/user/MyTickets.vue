<template>
    <el-card shadow="never" class="tickets-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">我的电子票</span>
          <div class="card-head-actions">
            <el-button type="primary" plain size="small" @click="openMyTransfers">转赠记录</el-button>
            <el-button type="success" plain size="small" @click="acceptDialogVisible = true">接收转赠</el-button>
          </div>
        </div>
      </template>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="未核销" name="0" />
      <el-tab-pane label="已核销" name="1" />
      <el-tab-pane label="已作废" name="2" />
    </el-tabs>

    <div v-if="tickets.length" class="ticket-list">
      <el-card
        v-for="ticket in tickets"
        :key="ticket.id"
        shadow="hover"
        class="ticket-item"
        @click="handleViewDetail(ticket.id)"
      >
        <div class="ticket-header">
          <div class="ticket-info">
            <span class="ticket-no">电子票编号：{{ ticket.verifyCode || ticket.ticketNo || ticket.id }}</span>
            <span class="ticket-time">{{ formatTime(ticket.createTime) }}</span>
          </div>
          <el-tag :type="statusTagType(ticket.status)" size="small" effect="light">
            {{ statusText(ticket.status) }}
          </el-tag>
          <el-tag
            v-if="transferBadge(ticket)"
            :type="transferBadge(ticket).type"
            size="small"
            effect="plain"
          >
            {{ transferBadge(ticket).text }}
          </el-tag>
          <el-tag
            v-if="refundBadge(ticket)"
            :type="refundBadge(ticket).type"
            size="small"
            effect="plain"
          >
            {{ refundBadge(ticket).text }}
          </el-tag>
        </div>

        <div class="ticket-body">
          <!-- 左侧活动信息 -->
          <div class="activity-info">
            <div class="activity-title">{{ ticket.activityTitle }}</div>
            <div class="activity-meta">
              <el-icon><Location /></el-icon>
              <span>{{ ticket.venue }}</span>
            </div>
            <div class="activity-meta">
              <el-icon><Clock /></el-icon>
              <span>{{ formatTime(ticket.showTime || ticket.startTime) }}</span>
            </div>
            <div class="activity-meta">
              <el-icon><Ticket /></el-icon>
              <span>座位类型：{{ ticket.seatType || ticket.seatTypeName || '通用座' }}</span>
            </div>
            <div class="activity-meta" v-if="ticket.seatInfo">
              <el-icon><Aim /></el-icon>
              <span>座位定位：{{ ticket.seatInfo }}</span>
            </div>
            <div class="activity-meta">
              <el-icon><User /></el-icon>
              <span>观演人：{{ ticket.attendeeName || ticket.viewerName || '本人' }}</span>
            </div>
          </div>

          <!-- 右侧二维码（后端签名时效令牌，真实可扫码） -->
          <div class="qrcode-area">
            <div class="qrcode-wrapper">
              <QrCode :text="ticket.qrToken || ticket.verifyCode || ticket.ticketNo || String(ticket.id)" :size="96" />
              <div class="qrcode-text">{{ ticket.verifyCode || ticket.ticketNo || ticket.id }}</div>
            </div>
            <div class="qrcode-tip">
              <el-icon><Aim /></el-icon>
              <span>入场核销码</span>
            </div>
          </div>
        </div>

        <div class="ticket-footer">
          <span class="ticket-hint">点击卡片查看完整电子票详情</span>
          <div class="footer-actions">
            <el-button
              v-if="canTransfer(ticket)"
              type="warning"
              text
              size="small"
              @click.stop="openTransfer(ticket)"
            >转赠</el-button>
            <el-button type="primary" text size="small" @click.stop="handleViewDetail(ticket.id)">
              查看详情
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-else description="暂无电子票" />

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchTickets"
      />
    </div>

    <!-- 电子票详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="电子票详情"
      width="560px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <div v-if="detailData" class="detail-content">
          <!-- 二维码区域（后端签名时效令牌，真实可扫码） -->
          <div class="detail-qrcode">
            <QrCode :text="detailData.qrToken || detailData.verifyCode || detailData.ticketNo || String(detailData.id)" :size="180" />
            <div class="detail-code">{{ detailData.verifyCode || detailData.ticketNo || detailData.id }}</div>
            <el-tag :type="statusTagType(detailData.status)" size="large" effect="light">
              {{ statusText(detailData.status) }}
            </el-tag>
            <el-tag
              v-if="refundBadge(detailData)"
              :type="refundBadge(detailData).type"
              size="large"
              effect="plain"
            >
              {{ refundBadge(detailData).text }}
            </el-tag>
          </div>

          <el-descriptions :column="1" border class="detail-desc">
            <el-descriptions-item label="活动名称">{{ detailData.activityTitle }}</el-descriptions-item>
            <el-descriptions-item label="举办场馆">{{ detailData.venue }}</el-descriptions-item>
            <el-descriptions-item label="演出时间">{{ formatTime(detailData.showTime || detailData.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="座位类型">{{ detailData.seatType || detailData.seatTypeName || '通用座' }}</el-descriptions-item>
            <el-descriptions-item label="座位定位" v-if="detailData.seatInfo">{{ detailData.seatInfo }}</el-descriptions-item>
            <el-descriptions-item label="观演人">{{ detailData.attendeeName || detailData.viewerName || '本人' }}</el-descriptions-item>
            <el-descriptions-item label="订单编号">{{ detailData.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关联订单状态">{{ orderStatusText(detailData.orderStatus) }}</el-descriptions-item>
            <el-descriptions-item label="退票状态">{{ refundStatusText(detailData.refundStatus) }}</el-descriptions-item>
            <el-descriptions-item label="电子票编号">{{ detailData.verifyCode || detailData.ticketNo || detailData.id }}</el-descriptions-item>
            <el-descriptions-item label="核销时间" v-if="detailData.checkTime">
              {{ formatTime(detailData.checkTime) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeDetail">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 发起转赠：填写接收方手机号，生成转赠码 -->
    <el-dialog v-model="transferDialogVisible" title="转赠电子票" width="480px" destroy-on-close>
      <div v-if="transferTarget" class="transfer-box">
        <div class="transfer-activity">{{ transferTarget.activityTitle }}</div>
        <div class="transfer-meta">
          <span><el-icon><Location /></el-icon> {{ transferTarget.venue }}</span>
          <span><el-icon><Ticket /></el-icon> {{ transferTarget.seatType || '通用座' }}</span>
        </div>
        <el-alert
          v-if="!transferResultCode"
          type="info"
          :closable="false"
          show-icon
          title="转赠后该票将暂不可用，待对方接收后转移所有权；对方 24 小时内未接收将自动取消。"
          style="margin: 12px 0"
        />
        <el-form v-if="!transferResultCode" label-width="92px" style="margin-top: 8px">
          <el-form-item label="接收方手机" required>
            <el-input
              v-model="recipientPhone"
              maxlength="11"
              placeholder="请输入接收方注册手机号"
              @input="recipientPhone = recipientPhone.replace(/[^\d]/g, '')"
            />
          </el-form-item>
        </el-form>
        <div v-else class="transfer-code-box">
          <el-alert type="success" :closable="false" show-icon title="转赠发起成功！请将下方转赠码发送给接收方" />
          <div class="transfer-code">{{ transferResultCode }}</div>
          <div class="transfer-code-tip">接收方在「我的电子票 → 接收转赠」中输入此码即可领取</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeTransfer">关闭</el-button>
        <el-button
          v-if="transferTarget && !transferResultCode"
          type="warning"
          :loading="transferLoading"
          @click="confirmTransfer"
        >确认转赠</el-button>
        <el-button v-else type="primary" @click="copyTransferCode">复制转赠码</el-button>
      </template>
    </el-dialog>

    <!-- 接收转赠：输入转赠码领取 -->
    <el-dialog v-model="acceptDialogVisible" title="接收转赠" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="转赠码" required>
          <el-input
            v-model="acceptCode"
            placeholder="请输入好友发给你的转赠码"
            :maxlength="20"
            @input="acceptCode = acceptCode.trim()"
          />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="接收后，该电子票将归入你的票夹，原持有者将无法使用。"
      />
      <template #footer>
        <el-button @click="acceptDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="acceptLoading" @click="confirmAccept">确认接收</el-button>
      </template>
    </el-dialog>

    <!-- 我的转赠记录 -->
    <el-dialog v-model="myTransferVisible" title="我的转赠记录" width="560px" destroy-on-close>
      <div v-loading="myTransferLoading">
        <el-empty v-if="!myTransfers.length" description="暂无转赠记录" :image-size="70" />
        <div v-else class="my-transfer-list">
          <div v-for="t in myTransfers" :key="t.id" class="my-transfer-item">
            <div class="mt-info">
              <div class="mt-title">{{ t.activityTitle || ('活动#' + t.activityId) }}</div>
              <div class="mt-meta">
                转赠码：<span class="mt-code">{{ t.transferCode }}</span>
                · 状态：{{ transferStatusText(t.status) }}
                · {{ formatTime(t.createTime) }}
              </div>
            </div>
            <el-button
              v-if="t.status === 0"
              type="danger"
              text
              size="small"
              @click="cancelMyTransfer(t)"
            >取消转赠</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="myTransferVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getMyTickets, getTicketDetail } from '@/api/ticketCheck'
import { initiateTransfer, acceptTransfer, cancelTransfer, getMySentTransfers } from '@/api/transfer'
import { ElMessage, ElMessageBox } from 'element-plus'
import QrCode from '@/components/QrCode.vue'

const activeTab = ref('all')
const tickets = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const statusMap = {
  0: '未核销',
  1: '已核销',
  2: '已作废'
}

const statusTagTypeMap = {
  0: '',
  1: 'success',
  2: 'info'
}

const statusText = (status) => statusMap[status] ?? '未知'
const statusTagType = (status) => statusTagTypeMap[status] ?? 'info'

// 退票工单状态：null无申请/0待审核/1处理中/2驳回/3已退款
const refundStatusMap = { 0: '退票待审核', 1: '退款处理中', 2: '退款被驳回', 3: '已退票' }
const refundStatusText = (rs) => (rs == null ? '无退票申请' : refundStatusMap[rs] ?? '未知')
const orderStatusMap = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已超时', 5: '已退款' }
const orderStatusText = (os) => (os == null ? '-' : orderStatusMap[os] ?? '未知')

// 需要额外亮出“退票处理中/已退票/被驳回”等联动提示时返回角标
const refundBadge = (t) => {
  const rs = t?.refundStatus
  if (rs == null) return null
  if (rs === 3) return { text: '已退票', type: 'info' }
  if (rs === 0 || rs === 1) return { text: '退票处理中', type: 'warning' }
  if (rs === 2) return { text: '退款被驳回', type: 'danger' }
  return null
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const fetchTickets = async () => {
  try {
    const res = await getMyTickets()
    let list = res.data?.records || res.data?.list || res.data || []
    if (!Array.isArray(list)) list = []
    // 前端按状态过滤
    if (activeTab.value !== 'all') {
      list = list.filter(item => String(item.status) === activeTab.value)
    }
    total.value = list.length
    // 简单分页
    const start = (page.value - 1) * pageSize.value
    tickets.value = list.slice(start, start + pageSize.value)
  } catch {
    // 静默处理
  }
}

const handleTabChange = () => {
  page.value = 1
  fetchTickets()
}

const handleViewDetail = async (id) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await getTicketDetail(id)
    detailData.value = res.data || null
  } catch {
    ElMessage.error('获取电子票详情失败')
  } finally {
    detailLoading.value = false
  }
  // 详情长时间打开时，定时刷新令牌避免二维码过期
  startDetailRefresh()
}

// 关闭详情时清除刷新定时器
const closeDetail = () => {
  detailDialogVisible.value = false
  stopDetailRefresh()
}

// 令牌刷新：列表每 9 分钟整体刷新（默认有效期 10 分钟），详情每 5 分钟刷新
let listTimer = null
let detailTimer = null
const LIST_REFRESH_MS = 9 * 60 * 1000
const DETAIL_REFRESH_MS = 5 * 60 * 1000

const startDetailRefresh = () => {
  stopDetailRefresh()
  detailTimer = setInterval(async () => {
    if (detailDialogVisible.value && detailData.value?.id) {
      try {
        const res = await getTicketDetail(detailData.value.id)
        if (res?.data) detailData.value = res.data
      } catch {
        // 静默处理
      }
    }
  }, DETAIL_REFRESH_MS)
}
const stopDetailRefresh = () => {
  if (detailTimer) {
    clearInterval(detailTimer)
    detailTimer = null
  }
}

// ===== 电子票转赠（借鉴大麦「转赠」能力） =====
const transferDialogVisible = ref(false)
const transferLoading = ref(false)
const transferTarget = ref(null)
const recipientPhone = ref('')
const transferResultCode = ref('')

const acceptDialogVisible = ref(false)
const acceptLoading = ref(false)
const acceptCode = ref('')

const myTransferVisible = ref(false)
const myTransferLoading = ref(false)
const myTransfers = ref([])

const transferStatusTextMap = { 0: '转出中', 1: '已转出', 2: '已取消', 3: '已过期' }
const transferStatusText = (s) => transferStatusTextMap[s] ?? '未知'

// 票夹中展示：转出中 / 已转出 角标（持有中不展示）
const transferBadge = (t) => {
  const ts = t?.transferStatus
  if (ts == null || ts === 0) return null
  if (ts === 1) return { text: '转出中', type: 'warning' }
  if (ts === 2) return { text: '已转出', type: 'info' }
  return null
}

// 可发起转赠：未核销 + 仍持有（未转出）+ 订单已支付
const canTransfer = (t) =>
  t && t.status === 0 && (t.transferStatus == null || t.transferStatus === 0) && t.orderStatus === 1

const openTransfer = (ticket) => {
  transferTarget.value = ticket
  recipientPhone.value = ''
  transferResultCode.value = ''
  transferDialogVisible.value = true
}

const confirmTransfer = async () => {
  if (!/^1[3-9]\d{9}$/.test(recipientPhone.value)) {
    ElMessage.warning('请输入正确的 11 位接收方手机号')
    return
  }
  transferLoading.value = true
  try {
    const res = await initiateTransfer({
      orderId: transferTarget.value.orderId,
      recipientPhone: recipientPhone.value,
      ticketIds: [transferTarget.value.id],
    })
    transferResultCode.value = res.data?.transferCode || ''
    ElMessage.success('转赠发起成功')
    fetchTickets()
  } catch {
    // 错误信息由请求拦截器统一提示
  } finally {
    transferLoading.value = false
  }
}

const closeTransfer = () => {
  transferDialogVisible.value = false
  transferTarget.value = null
  recipientPhone.value = ''
  transferResultCode.value = ''
}

const copyTransferCode = async () => {
  try {
    await navigator.clipboard.writeText(transferResultCode.value)
    ElMessage.success('转赠码已复制')
  } catch {
    ElMessage.info('请手动复制转赠码：' + transferResultCode.value)
  }
}

const confirmAccept = async () => {
  if (!acceptCode.value) {
    ElMessage.warning('请输入转赠码')
    return
  }
  acceptLoading.value = true
  try {
    await acceptTransfer({ transferCode: acceptCode.value })
    ElMessage.success('转赠接收成功，电子票已归入你的票夹')
    acceptDialogVisible.value = false
    acceptCode.value = ''
    fetchTickets()
  } catch {
    // 错误信息由请求拦截器统一提示
  } finally {
    acceptLoading.value = false
  }
}

const openMyTransfers = async () => {
  myTransferVisible.value = true
  myTransferLoading.value = true
  try {
    const res = await getMySentTransfers()
    myTransfers.value = res.data || []
  } catch {
    myTransfers.value = []
  } finally {
    myTransferLoading.value = false
  }
}

const cancelMyTransfer = async (t) => {
  try {
    await ElMessageBox.confirm('确定取消该转赠？取消后电子票将恢复为可用状态。', '取消转赠', { type: 'warning' })
    await cancelTransfer({ transferId: t.id })
    ElMessage.success('已取消转赠')
    openMyTransfers()
    fetchTickets()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(() => {
  fetchTickets()
  listTimer = setInterval(fetchTickets, LIST_REFRESH_MS)
})

onUnmounted(() => {
  if (listTimer) clearInterval(listTimer)
  stopDetailRefresh()
})
</script>

<style scoped>
.tickets-card {
  min-height: 400px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ticket-item {
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.ticket-item:hover {
  transform: translateY(-2px);
}

.ticket-item :deep(.el-card__body) {
  padding: 16px 20px;
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.ticket-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.ticket-no {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.ticket-time {
  font-size: 13px;
  color: #909399;
}

.ticket-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  gap: 20px;
}

.activity-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.activity-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.activity-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

/* 二维码区域 */
.qrcode-area {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.qrcode-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.fake-qrcode {
  display: flex;
  flex-direction: column;
  width: 96px;
  height: 96px;
  padding: 4px;
  background: #fff;
}

.qr-row {
  display: flex;
  flex: 1;
}

.qr-cell {
  flex: 1;
  min-width: 0;
  min-height: 0;
}

.qr-cell-on {
  background: #303133;
}

.qrcode-text {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qrcode-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.ticket-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.ticket-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 详情对话框 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 0;
  background: #fafafa;
  border-radius: 6px;
}

.fake-qrcode.big {
  width: 180px;
  height: 180px;
  padding: 8px;
}

.detail-code {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  font-family: monospace;
  letter-spacing: 1px;
}

.detail-desc {
  margin-top: 8px;
}

/* 卡头：标题与操作按钮同行 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-head-actions {
  display: flex;
  gap: 8px;
}

.ticket-footer .footer-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 发起转赠 */
.transfer-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.transfer-activity {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.transfer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: 13px;
  color: #606266;
}

.transfer-meta .el-icon {
  margin-right: 2px;
  vertical-align: -2px;
}

.transfer-code-box {
  margin-top: 8px;
  text-align: center;
}

.transfer-code {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 3px;
  color: #ff4d6d;
  font-family: monospace;
  background: #fff5f7;
  border: 1px dashed #ffb3c0;
  border-radius: 8px;
  padding: 12px;
  margin: 12px 0 6px;
}

.transfer-code-tip {
  font-size: 12px;
  color: #909399;
}

/* 我的转赠记录 */
.my-transfer-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.my-transfer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
}

.mt-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.mt-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.mt-code {
  color: #ff4d6d;
  font-family: monospace;
  font-weight: 600;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .tickets-card {
    min-height: 0;
  }

  .tickets-card :deep(.el-card__body) {
    padding: 12px;
  }

  /* 卡头：标题与按钮换行堆叠 */
  .card-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .card-head-actions {
    width: 100%;
  }

  .card-head-actions :deep(.el-button) {
    flex: 1;
  }

  /* Tab 横向滚动 */
  .tickets-card :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }

  .tickets-card :deep(.el-tabs__item) {
    font-size: 13px;
    padding: 0 10px;
  }

  /* 票券头部：编号与时间换行 */
  .ticket-header {
    flex-wrap: wrap;
    gap: 6px;
  }

  .ticket-info {
    flex-wrap: wrap;
    gap: 4px 12px;
  }

  .ticket-no {
    font-size: 13px;
  }

  .ticket-time {
    font-size: 12px;
  }

  /* 票券主体：二维码区域移到信息下方居中 */
  .ticket-body {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 12px 0;
  }

  .activity-title {
    font-size: 15px;
  }

  .qrcode-area {
    align-self: center;
  }

  /* 底部提示与按钮换行 */
  .ticket-footer {
    flex-wrap: wrap;
    gap: 6px;
  }

  .pagination-wrapper {
    overflow-x: auto;
  }

  /* 详情弹窗：二维码缩小 */
  .detail-qrcode :deep(.el-avatar),
  .detail-qrcode canvas {
    max-width: 140px;
  }
}
</style>

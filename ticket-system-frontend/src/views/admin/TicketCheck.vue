<template>
  <div class="ticket-check-page">
    <!-- 核销功能区 -->
    <el-card>
      <template #header><span>核销电子票</span></template>
      <div class="verify-area">
        <el-input
          v-model="verifyCode"
          placeholder="请输入或粘贴核销码（32位）"
          clearable
          style="width: 400px"
          size="large"
          @keyup.enter="handleVerify"
        >
          <template #prefix><el-icon><Ticket /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" :loading="verifyLoading" @click="handleVerify">
          <el-icon style="margin-right: 4px"><CircleCheck /></el-icon>核销
        </el-button>
        <el-button size="large" :loading="verifyLoading" @click="startScanner">
          <el-icon style="margin-right: 4px"><Camera /></el-icon>扫码核销
        </el-button>
        <el-badge :value="offlineCount" :hidden="offlineCount === 0" type="warning">
          <el-button size="large" :disabled="offlineCount === 0" @click="flushOfflineQueue">
            <el-icon style="margin-right: 4px"><Refresh /></el-icon>同步离线
          </el-button>
        </el-badge>
        <el-select
          v-model="activityId"
          placeholder="按活动筛选（统计与记录联动）"
          clearable
          filterable
          size="large"
          style="width: 280px"
          @change="handleActivityChange"
        >
          <el-option
            v-for="item in activityOptions"
            :key="item.id"
            :label="item.title"
            :value="item.id"
          />
        </el-select>
      </div>
      <div class="verify-tip">
        <el-tag size="small" :type="isAdmin ? 'danger' : 'warning'" effect="plain">
          {{ isAdmin ? '管理员' : '活动运营' }}
        </el-tag>
        <span v-if="isAdmin">可核销全部活动的电子票</span>
        <span v-else>仅可核销本人负责的 {{ activityOptions.length }} 个活动</span>
        <span class="verify-tip-sub">支持摄像头扫码或手动录入核销码，回车即可核销；核销成功会展示票券详情供二次确认。离线时核销请求将缓存，恢复网络后自动同步。</span>
      </div>
      <el-alert
        v-if="!isAdmin && activityOptions.length === 0"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 12px"
        title="当前账号暂无负责的活动"
        description="请联系管理员在活动管理中把活动指派给你，或使用管理员账号核销。"
      />
    </el-card>

    <!-- 入场统计卡片 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalTickets || 0 }}</div>
              <div class="stat-label">总购票人数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon :size="28"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.checkedTickets || 0 }}</div>
              <div class="stat-label">已核销人数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C">
              <el-icon :size="28"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.uncheckedTickets || 0 }}</div>
              <div class="stat-label">未入场人数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 入场进度条 -->
    <el-card style="margin-top: 20px" v-if="(statistics.totalTickets || 0) > 0">
      <div class="progress-row">
        <span class="progress-label">入场率</span>
        <el-progress
          :percentage="entryRate"
          :stroke-width="18"
          :format="() => entryRate + '%'"
          class="progress-bar"
        />
        <span class="progress-text">
          已入场 {{ statistics.checkedTickets || 0 }} / {{ statistics.totalTickets || 0 }} 人
          <template v-if="activityName">（{{ activityName }}）</template>
        </span>
      </div>
    </el-card>

    <!-- 核销记录表格 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>核销记录</span>
          <el-input
            v-model="recordKeyword"
            placeholder="搜索票号 / 活动标题"
            clearable
            style="width: 240px"
            @input="handleKeywordSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="filteredRecords" v-loading="recordsLoading" stripe>
          <el-table-column label="电子票编号" prop="ticketNo" width="200" />
          <el-table-column label="活动标题" prop="activityTitle" show-overflow-tooltip min-width="200" />
          <el-table-column label="观演人" width="120">
            <template #default="{ row }">{{ row.attendeeName || '-' }}</template>
          </el-table-column>
          <el-table-column label="核销人员" prop="checkUsername" width="120" />
          <el-table-column label="核销方式" width="100">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.checkDevice || '扫码/输入' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="核销时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.checkTime) }}
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="filter.page"
          v-model:page-size="filter.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchRecords"
          @current-change="fetchRecords"
        />
      </div>
    </el-card>

    <!-- 核销结果对话框 -->
    <el-dialog v-model="resultVisible" width="440px" :title="resultSuccess ? '✅ 核销成功' : '❌ 核销失败'" align-center>
      <div v-if="resultSuccess && resultRecord" class="result-body">
        <div class="result-activity">{{ resultRecord.activityTitle }}</div>
        <div class="result-grid">
          <div class="result-item"><span class="result-label">票号</span><span class="result-value">{{ resultRecord.ticketNo }}</span></div>
          <div class="result-item"><span class="result-label">观演人</span><span class="result-value">{{ resultRecord.attendeeName || '未实名' }}</span></div>
          <div class="result-item"><span class="result-label">核销时间</span><span class="result-value">{{ formatTime(resultRecord.checkTime) }}</span></div>
          <div class="result-item"><span class="result-label">核销员</span><span class="result-value">{{ resultRecord.checkUsername }}</span></div>
        </div>
        <div class="result-tip">请引导观演人入场，祝观演愉快！</div>
      </div>
      <div v-else class="result-body">
        <div class="result-error">{{ resultError }}</div>
        <div class="result-tip">请核对核销码后重试；如票已作废/已核销，请联系现场负责人</div>
      </div>
      <template #footer>
        <el-button @click="resultVisible = false">关闭</el-button>
        <el-button type="primary" @click="continueVerify">继续核销下一张</el-button>
      </template>
    </el-dialog>

    <!-- 摄像头扫码核销弹窗 -->
    <el-dialog v-model="scannerVisible" title="扫码核销" width="420px" align-center :close-on-click-modal="false" @close="stopScanner">
      <div class="scanner-box">
        <div id="qr-reader" style="width: 100%"></div>
        <div class="scanner-tip">将二维码对准摄像头，系统会自动识别并完成核销</div>
      </div>
      <template #footer>
        <el-button @click="stopScanner">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { User, CircleCheck, Clock, Search, Ticket, Camera, Refresh } from '@element-plus/icons-vue'
import { Html5Qrcode } from 'html5-qrcode'
import { verifyTicket, getCheckRecords, getCheckStatistics, getMyCheckActivities } from '@/api/ticketCheck'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 管理员可核销全部活动；活动运营仅可核销本人负责的活动（后端同样做强制校验）
const isAdmin = computed(() => userStore.role === 2)

const verifyCode = ref('')
const verifyLoading = ref(false)
const activityId = ref(null)
const activityOptions = ref([])
const device = ref('网页核销端')

const statistics = ref({})
const records = ref([])
const total = ref(0)
const recordsLoading = ref(false)
const recordKeyword = ref('')

// 核销结果弹窗
const resultVisible = ref(false)
const resultSuccess = ref(false)
const resultRecord = ref(null)
const resultError = ref('')

const filter = ref({
  page: 1,
  size: 10
})

// 入场率
const entryRate = computed(() => {
  const totalTickets = Number(statistics.value.totalTickets || 0)
  if (totalTickets === 0) return 0
  return Math.round((Number(statistics.value.checkedTickets || 0) / totalTickets) * 100)
})

// 当前筛选的活动名
const activityName = computed(() => {
  if (!activityId.value) return ''
  const act = activityOptions.value.find(a => a.id === activityId.value)
  return act ? act.title : ''
})

// 前端关键词过滤（票号/活动标题）
const filteredRecords = computed(() => {
  const kw = recordKeyword.value.trim().toLowerCase()
  if (!kw) return records.value
  return records.value.filter(r =>
    (r.ticketNo || '').toLowerCase().includes(kw) ||
    (r.activityTitle || '').toLowerCase().includes(kw)
  )
})

const handleKeywordSearch = () => {
  // 本地过滤，无需请求
}

const fetchActivityOptions = async () => {
  try {
    // 后端按身份返回：管理员=全部已发布活动，活动运营=本人负责的活动
    const res = await getMyCheckActivities()
    const data = res?.data || res
    activityOptions.value = Array.isArray(data) ? data : []
    // 活动运营仅负责一场活动时自动锁定，减少现场误操作
    if (!isAdmin.value && activityOptions.value.length === 1) {
      activityId.value = activityOptions.value[0].id
      fetchStatistics()
      fetchRecords()
    }
  } catch {
    activityOptions.value = []
  }
}

const fetchStatistics = async () => {
  try {
    const res = await getCheckStatistics(activityId.value || undefined)
    statistics.value = res?.data || res || {}
  } catch {
    // 静默处理
  }
}

const fetchRecords = async () => {
  recordsLoading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      activityId: activityId.value || undefined
    }
    const res = await getCheckRecords(params)
    const data = res?.data || res
    records.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 静默处理
  } finally {
    recordsLoading.value = false
  }
}

const handleActivityChange = () => {
  filter.value.page = 1
  fetchStatistics()
  fetchRecords()
}

const handleVerify = async (presetCode) => {
  const code = (presetCode != null ? presetCode : verifyCode.value).trim()
  if (!code) {
    ElMessage.warning('请输入或扫描核销码')
    return
  }
  verifyLoading.value = true
  try {
    // 区分设备来源：扫码核销记为“摄像头扫码”，手动录入记为“网页核销端”
    device.value = presetCode != null ? '摄像头扫码' : '网页核销端'
    const data = { verifyCode: code, device: device.value }
    if (activityId.value) data.activityId = activityId.value
    const res = await verifyTicket(data)
    const record = res?.data || res
    // 核销成功：弹详情卡片供现场二次确认
    resultSuccess.value = true
    resultRecord.value = record
    resultVisible.value = true
    verifyCode.value = ''
    fetchStatistics()
    fetchRecords()
    // 联网核销成功后，顺带把离线缓存的核销补同步上去
    flushOfflineQueue()
  } catch (err) {
    // 只有真正的网络错误（AxiosError 且无响应）才进入离线队列；
    // 业务错误（已核销/已作废/无权限等）必须直接提示，避免反复重试无效请求
    const isNetwork = err?.isAxiosError === true && !err?.response
    if (isNetwork) {
      // 断网/弱网：缓存到本地，待恢复网络后批量同步
      enqueueOffline(code)
      resultSuccess.value = false
      resultRecord.value = null
      resultError.value = '网络异常，已加入离线待同步队列，恢复网络后将自动核销'
    } else {
      // 业务错误（已核销/已作废/不属于当前场次等）：直接提示
      resultSuccess.value = false
      resultRecord.value = null
      resultError.value = err?.message || '核销失败，请核对核销码'
    }
    resultVisible.value = true
  } finally {
    verifyLoading.value = false
  }
}

/* ===================== 摄像头扫码核销 ===================== */
const scannerVisible = ref(false)
let html5Qr = null

const startScanner = async () => {
  scannerVisible.value = true
  await nextTick()
  try {
    html5Qr = new Html5Qrcode('qr-reader', { verbose: false })
    await html5Qr.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: 240 },
      async (decodedText) => {
        // 扫描成功：停止摄像头并核销（扫码默认记为摄像头扫码设备）
        device.value = '摄像头扫码'
        await stopScanner()
        await handleVerify(decodedText)
      },
      () => { /* 忽略逐帧识别错误 */ }
    )
  } catch (e) {
    ElMessage.warning('无法开启摄像头，请检查浏览器权限，或改用手动录入')
    await stopScanner()
  }
}

const stopScanner = async () => {
  if (html5Qr) {
    try { await html5Qr.stop(); await html5Qr.clear() } catch (e) { /* 忽略 */ }
    html5Qr = null
  }
  scannerVisible.value = false
}

/* ===================== 离线兜底队列 ===================== */
const OFFLINE_KEY = 'ticket_check_offline_queue'
const offlineCount = ref(0)

const loadOffline = () => {
  try { return JSON.parse(localStorage.getItem(OFFLINE_KEY) || '[]') } catch (e) { return [] }
}
const saveOffline = (q) => {
  localStorage.setItem(OFFLINE_KEY, JSON.stringify(q))
  offlineCount.value = q.length
}
const enqueueOffline = (code) => {
  const q = loadOffline()
  const exists = q.find(x => x.code === code && x.activityId === (activityId.value || null))
  if (!exists) {
    q.push({ code, activityId: activityId.value || null, device: '离线缓存', ts: Date.now() })
    saveOffline(q)
  }
  ElMessage.warning('网络异常，已缓存到本地，恢复后自动核销')
}

const flushOfflineQueue = async () => {
  const q = loadOffline()
  if (!q.length) return
  const remain = []
  for (const item of q) {
    try {
      const data = { verifyCode: item.code, device: item.device }
      if (item.activityId) data.activityId = item.activityId
      await verifyTicket(data)
      // 核销成功：不保留
    } catch (err) {
      if (err?.isAxiosError === true && !err?.response) {
        remain.push(item) // 仍是网络错误，保留继续重试
      }
      // 业务错误（已核销/已作废/无权限等）视为已处理，丢弃避免死循环
    }
  }
  saveOffline(remain)
  if (remain.length !== q.length) {
    fetchStatistics()
    fetchRecords()
    if (remain.length === 0) ElMessage.success('离线缓存的核销已全部同步')
  }
}

// 继续核销：关闭弹窗并聚焦输入框
const continueVerify = () => {
  resultVisible.value = false
  verifyCode.value = ''
}

const formatTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

let syncTimer = null

onMounted(() => {
  fetchActivityOptions()
  fetchStatistics()
  fetchRecords()
  // 离线兜底：初始化待同步数量，并监听网络恢复自动同步
  offlineCount.value = loadOffline().length
  window.addEventListener('online', flushOfflineQueue)
  syncTimer = setInterval(() => {
    if (typeof navigator !== 'undefined' && navigator.onLine) {
      flushOfflineQueue()
    }
  }, 15000)
})

onUnmounted(() => {
  window.removeEventListener('online', flushOfflineQueue)
  if (syncTimer) clearInterval(syncTimer)
  stopScanner()
})
</script>

<style scoped>
.verify-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.verify-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.verify-tip-sub {
  color: #909399;
}

.stat-card {
  margin-bottom: 10px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

/* 入场进度 */
.progress-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.progress-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex-shrink: 0;
}

.progress-bar {
  flex: 1;
}

.progress-text {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 核销结果弹窗 */
.result-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-activity {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: #f9fafc;
  border-radius: 8px;
  padding: 10px 12px;
}

.result-label {
  font-size: 12px;
  color: #909399;
}

.result-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.result-tip {
  font-size: 13px;
  color: #909399;
  text-align: center;
}

.result-error {
  font-size: 15px;
  font-weight: 600;
  color: #f56c6c;
  text-align: center;
  padding: 8px 0;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .ticket-check-page {
    max-width: 100%;
    box-sizing: border-box;
  }
  /* 卡片头部与筛选区改为纵向堆叠 */
  .card-header,
  .top-bar,
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .filter-area {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .filter-area :deep(.el-input),
  .filter-area :deep(.el-select),
  .filter-area :deep(.el-date-editor) {
    width: 100% !important;
  }

  .filter-area :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }

  /* 统计小卡改为两列 */
  .stat-card {
    min-width: 0;
  }

  .stat-strip {
    flex-wrap: wrap;
  }

  /* 表格容器横向滚动，列宽保持不变 */
  .table-scroll {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .table-scroll :deep(.el-table) {
    min-width: 720px;
  }

  .pagination-wrap,
  .pagination-wrapper {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>

<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-info">
        <div class="welcome-title">
          你好，{{ userStore.nickname || userStore.username }} 👋
        </div>
        <div class="welcome-sub">今天是 {{ today }}，经营好自己的每一场演出，票卖得好全靠用心运营。</div>
      </div>
      <el-button type="primary" class="publish-btn" :icon="Plus" @click="router.push('/operator/activity/add')">
        发布新活动
      </el-button>
    </div>

    <!-- 指标卡 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg,#409eff,#35c1f0)">
            <el-icon :size="26"><Tickets /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="stat-value">{{ stats.totalActivities }}</div>
            <div class="stat-label">活动总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg,#ff9f43,#f7c948)">
            <el-icon :size="26"><Clock /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="stat-value">{{ stats.pendingAudit }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg,#34c77b,#67c23a)">
            <el-icon :size="26"><CircleCheck /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="stat-value">{{ stats.onlineActivities }}</div>
            <div class="stat-label">已上架</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg,#ff4d6d,#ff7849)">
            <el-icon :size="26"><User /></el-icon>
          </div>
          <div class="stat-meta">
            <div class="stat-value">{{ stats.totalSoldTickets }}</div>
            <div class="stat-label">累计售票</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 营收横幅 -->
    <div class="revenue-banner">
      <div class="revenue-item">
        <div class="revenue-label">预估票房流水</div>
        <div class="revenue-value">¥{{ formatMoney(stats.revenue) }}</div>
      </div>
      <div class="revenue-divider"></div>
      <div class="revenue-item">
        <div class="revenue-label">已核销票数</div>
        <div class="revenue-value">{{ stats.checkedTickets }}</div>
      </div>
      <div class="revenue-divider"></div>
      <div class="revenue-item">
        <div class="revenue-label">平均每场上座售票</div>
        <div class="revenue-value">{{ stats.avgSoldPerActivity }}</div>
      </div>
      <div class="revenue-item tip">
        <div class="revenue-label">运营提示</div>
        <div class="revenue-tip">{{ tipText }}</div>
      </div>
    </div>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <el-col :span="15">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">门票销量 Top5</span>
              <span class="card-sub">按活动累计已售门票</span>
            </div>
          </template>
          <div ref="salesChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">活动状态分布</span>
              <span class="card-sub">各状态活动占比</span>
            </div>
          </template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">最近活动</span>
          <el-button text type="primary" @click="router.push('/operator/activity')">查看全部 →</el-button>
        </div>
      </template>
      <div class="table-scroll">
        <el-table :data="recentActivities" style="width: 100%">
          <el-table-column label="活动" min-width="260">
            <template #default="{ row }">
              <div class="act-cell" @click="goDetail(row)">
                <el-image
                  v-if="row.coverImage"
                  :src="row.coverImage"
                  fit="cover"
                  class="act-cover"
                  :preview-src-list="[row.coverImage]"
                  lazy
                  >
                  <template #error>
                    <div class="act-cover act-cover-empty">
                      <el-icon :size="18"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="act-cover act-cover-empty">
                  <el-icon :size="18"><Picture /></el-icon>
                </div>
                <div class="act-info">
                  <div class="act-title ellipsis">{{ row.title }}</div>
                  <div class="act-sub ellipsis">{{ row.categoryName }} · {{ row.venue }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="票价" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ row.price }} 起</span>
            </template>
          </el-table-column>
          <el-table-column label="售票进度" width="220">
            <template #default="{ row }">
              <div class="progress-wrap">
                <el-progress
                  :percentage="progressPercent(row)"
                  :stroke-width="8"
                  :color="progressColor(row)"
                  />
                <span class="progress-text">{{ row.soldTickets }}/{{ row.totalTickets }} 张</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light" round>{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="抢票时间" min-width="170">
            <template #default="{ row }">{{ row.grabStartTime }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goDetail(row)">详情</el-button>
              <el-button link type="primary" :disabled="![0, 2].includes(row.status)" @click="goEdit(row)">编辑</el-button>
              <el-button
                v-if="row.status === 3"
                link
                type="danger"
                @click="toggleStatus(row)"
              >下架</el-button>
              <el-button
                v-else-if="row.status === 4 || row.status === 2"
                link
                type="success"
                @click="toggleStatus(row)"
              >重新提交</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { getMyActivities, applyActivity, offlineActivity } from '@/api/operator'
import { useUserStore } from '@/stores/user'
import { Tickets, Clock, CircleCheck, User, Plus, Picture } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const stats = reactive({
  totalActivities: 0,
  pendingAudit: 0,
  onlineActivities: 0,
  totalSoldTickets: 0,
  checkedTickets: 0,
  revenue: 0,
  avgSoldPerActivity: 0
})

const activities = ref([])
const recentActivities = ref([])
const salesChartRef = ref(null)
const statusChartRef = ref(null)
let salesChart = null
let statusChart = null

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

const tipText = computed(() => {
  if (stats.pendingAudit > 0) return `有 ${stats.pendingAudit} 场活动等待平台审核，可去活动列表催办。`
  if (stats.totalActivities === 0) return '发布你的第一场活动，开启票务运营吧！'
  if (stats.onlineActivities > 0) return '已上架活动记得定期关注售票与退票数据哦。'
  return '完善活动信息并提交审核后即可上架开售。'
})

const statusMap = {
  0: '待审核', 1: '已通过', 2: '已驳回', 3: '已上架', 4: '已下架'
}
const statusTagTypeMap = {
  0: 'warning', 1: '', 2: 'danger', 3: 'success', 4: 'info'
}
const statusLabel = (status) => statusMap[status] || '未知'
const statusTagType = (status) => statusTagTypeMap[status] || 'info'

const formatMoney = (num) => Number(num || 0).toLocaleString('zh-CN')

const progressPercent = (row) => {
  if (!row.totalTickets) return 0
  return Math.min(100, Math.round(((row.soldTickets || 0) / row.totalTickets) * 100))
}
const progressColor = (row) => {
  const p = progressPercent(row)
  if (p >= 90) return '#ff4d6d'
  if (p >= 60) return '#ff7849'
  return '#409eff'
}

const loadAll = async () => {
  let page = 1
  const size = 50
  const all = []
  while (true) {
    const res = await getMyActivities({ page, size })
    const data = res.data || res
    const records = data.records || data.list || []
    all.push(...records)
    const total = data.total || all.length
    if (all.length >= total || records.length === 0) break
    page += 1
  }
  return all
}

const loadData = async () => {
  try {
    const all = await loadAll()
    activities.value = all
    const list = [...all].sort((a, b) => (b.soldTickets || 0) - (a.soldTickets || 0))
    recentActivities.value = list.slice(0, 6)

    stats.totalActivities = all.length
    stats.pendingAudit = all.filter((a) => a.status === 0).length
    stats.onlineActivities = all.filter((a) => a.status === 3).length
    stats.totalSoldTickets = all.reduce((s, a) => s + (a.soldTickets || 0), 0)
    stats.checkedTickets = all.reduce((s, a) => s + (a.checkedTickets || 0), 0)
    stats.revenue = all.reduce((s, a) => s + (a.soldTickets || 0) * (Number(a.price) || 0), 0)
    stats.avgSoldPerActivity = all.length ? Math.round(stats.totalSoldTickets / all.length) : 0

    await nextTick()
    renderCharts(all)
  } catch (e) {
    console.error(e)
  }
}

const renderCharts = (list) => {
  const sales = [...list].sort((a, b) => (b.soldTickets || 0) - (a.soldTickets || 0)).slice(0, 5)
  const names = sales.map((a) => (a.title || '').slice(0, 10))
  const values = sales.map((a) => a.soldTickets || 0)

  if (!salesChart && salesChartRef.value) {
    salesChart = echarts.init(salesChartRef.value)
  }
  if (salesChart) {
    salesChart.setOption({
      grid: { left: 10, right: 30, top: 20, bottom: 10, containLabel: true },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { color: '#909399', interval: 0 },
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#909399' },
        splitLine: { lineStyle: { color: '#f0f2f5' } }
      },
      series: [
        {
          type: 'bar',
          data: values,
          barWidth: 26,
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#5a8cff' },
              { offset: 1, color: '#409eff' }
            ])
          },
          label: { show: true, position: 'top', color: '#606266', fontSize: 12 }
        }
      ]
    })
  }

  const statusCounts = [0, 1, 2, 3, 4].map((s) => ({
    name: statusMap[s],
    value: list.filter((a) => a.status === s).length
  })).filter((i) => i.value > 0)

  if (!statusChart && statusChartRef.value) {
    statusChart = echarts.init(statusChartRef.value)
  }
  if (statusChart) {
    if (statusCounts.length) {
      statusChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 场 ({d}%)' },
        legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { color: '#606266' } },
        color: ['#ff9f43', '#909399', '#ff4d6d', '#67c23a', '#409eff'],
        series: [
          {
            type: 'pie',
            radius: ['48%', '72%'],
            center: ['50%', '44%'],
            itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
            label: { show: false },
            emphasis: { label: { show: true, fontSize: 14, fontWeight: 600 } },
            data: statusCounts
          }
        ],
        graphic: []
      })
    } else {
      statusChart.setOption({ series: [] })
      statusChart.setOption({
        graphic: [
          {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: { text: '暂无活动数据', fill: '#c0c4cc', fontSize: 14 }
          }
        ]
      })
    }
  }
}

const goDetail = (row) => {
  if (row.status === 3) {
    router.push(`/activity/${row.id}`)
  } else {
    router.push(`/operator/activity/edit/${row.id}`)
  }
}

const goEdit = (row) => {
  router.push(`/operator/activity/edit/${row.id}`)
}

const toggleStatus = async (row) => {
  try {
    if (row.status === 3) {
      await ElMessageBox.confirm(`确定将「${row.title}」下架吗？下架后用户将无法购票。`, '下架提醒', {
        type: 'warning'
      })
      await offlineActivity(row.id)
      ElMessage.success('已下架')
    } else if (row.status === 4 || row.status === 2) {
      await ElMessageBox.confirm(`确定将「${row.title}」重新提交平台审核吗？`, '提交审核', { type: 'info' })
      await applyActivity(row.id)
      ElMessage.success('已提交审核，等待平台审核通过后即可上架')
    } else if (row.status === 1) {
      ElMessage.info('活动已通过审核，请联系平台尽快安排上架')
      return
    } else {
      ElMessage.info('当前状态暂不支持该操作')
      return
    }
    loadData()
  } catch {
    // cancelled
  }
}

const handleResize = () => {
  salesChart && salesChart.resize()
  statusChart && statusChart.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesChart && salesChart.dispose()
  statusChart && statusChart.dispose()
  salesChart = null
  statusChart = null
})
</script>

<style scoped>
.dashboard {
  min-height: calc(100vh - 110px);
}

/* 欢迎横幅 */
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(120deg, #1b2a4a 0%, #2b4a8f 60%, #2b6cb8 100%);
  border-radius: 14px;
  padding: 22px 28px;
  color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(27, 42, 74, 0.2);
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.welcome-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.publish-btn {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border: none;
  box-shadow: 0 6px 16px rgba(255, 77, 109, 0.4);
}

/* 指标卡 */
.stat-row {
  margin-bottom: 4px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 10px rgba(31, 92, 170, 0.06);
  margin-bottom: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(31, 92, 170, 0.12);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.12);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #1b2a4a;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* 营收横幅 */
.revenue-banner {
  background: #fff;
  border-radius: 14px;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 28px;
  margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(31, 92, 170, 0.06);
}

.revenue-item {
  flex-shrink: 0;
}

.revenue-item.tip {
  flex: 1;
  margin-left: 10px;
  padding: 8px 14px;
  background: #f0f7ff;
  border-radius: 10px;
}

.revenue-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.revenue-value {
  font-size: 24px;
  font-weight: 800;
  color: #ff4d6d;
}

.revenue-tip {
  font-size: 13px;
  color: #2b6cb8;
  line-height: 1.6;
}

.revenue-divider {
  width: 1px;
  height: 40px;
  background: #ebeef5;
}

/* 图表 */
.chart-card,
.table-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 2px 10px rgba(31, 92, 170, 0.06);
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title {
  font-weight: 700;
  color: #1b2a4a;
}

.card-sub {
  font-size: 12px;
  color: #c0c4cc;
}

.chart-box {
  height: 320px;
}

/* 表格内活动列 */
.act-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.act-cover {
  width: 56px;
  height: 40px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f0f2f5;
}

.act-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.act-info {
  min-width: 0;
}

.act-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 3px;
}

.act-sub {
  font-size: 12px;
  color: #909399;
}

.price {
  color: #ff4d6d;
  font-weight: 700;
}

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-wrap :deep(.el-progress) {
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .dashboard {
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

<template>
  <div class="stats-page">
    <el-card class="filter-card">
      <div class="filter-inner">
        <div class="filter-left">
          <el-icon :size="18" style="margin-right: 6px"><DataAnalysis /></el-icon>
          <span class="filter-title">数据统计</span>
          <span class="filter-sub">选择具体活动可查看该活动详情</span>
        </div>
        <div class="filter-right">
          <el-select
            v-model="statusFilter"
            placeholder="全部状态"
            clearable
            @change="renderCharts"
            style="width: 130px"
          >
            <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="Number(key)" />
          </el-select>
          <el-select
            v-model="selectedActivity"
            placeholder="请选择活动"
            clearable
            filterable
            @change="handleSelect"
            style="width: 300px"
          >
            <el-option
              v-for="item in activities"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
          <el-button :icon="Refresh" circle title="刷新数据" @click="refresh" />
        </div>
      </div>
    </el-card>

    <!-- 无活动时的空状态 -->
    <el-card v-if="!loading && activities.length === 0" shadow="never">
      <el-empty description="还没有发布活动，发布后可在此查看销售数据">
        <el-button type="primary" :icon="Plus" @click="$router.push('/operator/activity/add')">去发布活动</el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <!-- 汇总视角（未选择具体活动时展示） -->
      <div v-show="!selectedActivity">
        <div class="overview-cards">
          <el-card v-for="c in overviewCards" :key="c.label" shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" :style="{ backgroundColor: c.color }">
                <el-icon :size="24"><component :is="c.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ c.raw }}</div>
                <div class="stat-label">{{ c.label }}</div>
              </div>
            </div>
          </el-card>
        </div>

        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="14">
            <el-card shadow="hover">
              <template #header><span>各活动售票进度</span></template>
              <div ref="overviewBarRef" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="hover">
              <template #header><span>各活动购票占比</span></template>
              <div ref="overviewPieRef" class="chart"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="14">
            <el-card shadow="hover">
              <template #header><span>各活动浏览预约情况</span></template>
              <div ref="browseBarRef" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :span="10">
            <el-card shadow="hover">
              <template #header>
                <div class="card-head">
                  <span>整体转化漏斗</span>
                  <span class="card-head-tip">浏览 → 预约 → 购票</span>
                </div>
              </template>
              <div ref="funnelRef" class="chart"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="hover" style="margin-top: 16px">
          <template #header><span>活动数据明细</span></template>
          <div class="table-scroll">
            <el-table :data="filteredActivities" v-loading="loading" style="width: 100%">
              <el-table-column label="活动名称" min-width="180">
                <template #default="{ row }">
                  <div class="cell-title">{{ row.title }}</div>
                  <div class="cell-time">开售 {{ formatTime(row.grabStartTime) }}</div>
                </template>
              </el-table-column>
              <el-table-column label="分类" width="110">
                <template #default="{ row }">{{ row.categoryName || '-' }}</template>
              </el-table-column>
              <el-table-column label="票量" width="220">
                <template #default="{ row }">
                  <div class="cell-tickets">
                    <span class="sell-rate">{{ rate(row) }}%</span>
                    <el-progress :percentage="rate(row)" :show-text="false" :stroke-width="8" />
                    <span class="cell-ticket-num">{{ row.soldTickets || 0 }}/{{ row.totalTickets || 0 }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="预约量" width="90" align="center" prop="reservation" sortable :sort-method="(a, b) => (reservationMap[a.id] || 0) - (reservationMap[b.id] || 0)">
                <template #default="{ row }">{{ reservationMap[row.id] || 0 }}</template>
              </el-table-column>
              <el-table-column label="浏览量" width="90" align="center" prop="viewCount" sortable>
                <template #default="{ row }">{{ row.viewCount || 0 }}</template>
              </el-table-column>
              <el-table-column label="预计营收" width="120" align="center" prop="revenue" sortable :sort-method="(a, b) => revenue(a) - revenue(b)">
                <template #default="{ row }">¥{{ revenue(row).toLocaleString() }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="goEdit(row)">编辑</el-button>
                  <el-button link type="primary" size="small" @click="goDetail(row)">前台页</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无数据" :image-size="80" />
              </template>
            </el-table>
          </div>
        </el-card>
      </div>

      <!-- 具体活动详情 -->
      <div v-show="selectedActivity">
        <!-- 活动基础信息 -->
        <el-card shadow="hover" class="act-info-card">
          <div class="act-info">
            <el-image v-if="coverUrl" :src="coverUrl" class="act-cover" fit="cover" lazy>
              <template #error>
                <div class="act-cover act-cover-empty"><el-icon :size="24"><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="act-cover act-cover-empty"><el-icon :size="24"><Picture /></el-icon></div>
            <div class="act-info-main">
              <div class="act-info-title">
                <span class="act-title-text">{{ currentActivity.title || '-' }}</span>
                <el-tag :type="statusTagType(currentActivity.status)" effect="light" size="small">
                  {{ statusLabel(currentActivity.status) }}
                </el-tag>
              </div>
              <div class="act-info-meta">
                <span><el-icon><Location /></el-icon>{{ currentActivity.venue || '场地待定' }}</span>
                <span><el-icon><Calendar /></el-icon>{{ formatTime(currentActivity.startTime) }} 开演</span>
                <span><el-icon><Ticket /></el-icon>票价 ¥{{ currentActivity.price || 0 }}</span>
                <span><el-icon><Folder /></el-icon>{{ currentActivity.categoryName || '未分类' }}</span>
              </div>
            </div>
            <div class="act-info-actions">
              <el-button size="small" :icon="View" @click="goDetail(currentActivity)">前台预览</el-button>
              <el-button size="small" type="primary" :icon="EditPen" @click="goEdit(currentActivity)">编辑活动</el-button>
            </div>
          </div>
        </el-card>

        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="4" v-for="item in statCards" :key="item.label">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-icon" :style="{ backgroundColor: item.color }">
                  <el-icon :size="22"><component :is="item.icon" /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ item.value }}</div>
                  <div class="stat-label">{{ item.label }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="8">
            <el-card shadow="hover">
              <template #header><span>预约 / 购票 / 浏览</span></template>
              <div ref="detailBarRef" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <template #header><span>售票占比</span></template>
              <div ref="detailPieRef" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <template #header><span>订单构成</span></template>
              <div ref="orderPieRef" class="chart"></div>
            </el-card>
          </el-col>
        </el-row>

        <el-card shadow="hover" style="margin-top: 16px">
          <template #header>
            <div class="card-head">
              <span>转化分析</span>
              <span class="card-head-tip">帮助判断活动从曝光到成交的每一环流失情况</span>
            </div>
          </template>
          <div class="convert-list">
            <div class="convert-item" v-for="c in convertList" :key="c.label">
              <div class="convert-top">
                <span class="convert-label">{{ c.label }}</span>
                <span class="convert-value">{{ c.text }}</span>
              </div>
              <el-progress :percentage="c.percent" :stroke-width="10" :color="c.color" />
              <div class="convert-desc">{{ c.desc }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getMyActivities, getActivityStats, getActivityHotStats } from '@/api/operator'
import {
  User, ShoppingCart, Document, CircleClose, Clock, Plus, DataAnalysis,
  Refresh, Picture, Location, Calendar, Ticket, Folder, View, EditPen, Money
} from '@element-plus/icons-vue'

const router = useRouter()

const selectedActivity = ref(null)
const statusFilter = ref(null)
const activities = ref([])
const stats = ref({})
const hotStats = ref({})
const loading = ref(false)
const reservationMap = ref({})

const overviewBarRef = ref()
const overviewPieRef = ref()
const browseBarRef = ref()
const funnelRef = ref()
const detailBarRef = ref()
const detailPieRef = ref()
const orderPieRef = ref()

let charts = []

const statusMap = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
  3: '已上架',
  4: '已下架'
}

const statusTagTypeMap = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'success',
  4: 'info'
}

const statusLabel = (status) => statusMap[status] || '未知'
const statusTagType = (status) => statusTagTypeMap[status] || 'info'

const currentActivity = computed(() =>
  activities.value.find(a => a.id === selectedActivity.value) || {}
)

const coverUrl = computed(() => currentActivity.value.coverImage || currentActivity.value.cover || '')

const filteredActivities = computed(() =>
  statusFilter.value === null || statusFilter.value === undefined
    ? activities.value
    : activities.value.filter(a => a.status === statusFilter.value)
)

const selectedTitle = computed(() => currentActivity.value.title || '')

// 预计营收：优先用后端按票档汇总的值（多票档口径正确）；列表未带 revenue 时退回"单一挂牌价×已售"估算
const revenue = (row) => {
  if (row.revenue != null) return Number(row.revenue) || 0
  return (row.soldTickets || 0) * (row.price || 0)
}

const statCards = computed(() => [
  { label: '预约人数', value: stats.value.reservationCount || 0, color: '#409EFF', icon: User },
  { label: '购票总量', value: stats.value.ticketCount || 0, color: '#67C23A', icon: ShoppingCart },
  { label: '成交订单数', value: stats.value.successOrderCount || 0, color: '#E6A23C', icon: Document },
  { label: '取消订单数', value: stats.value.cancelOrderCount || 0, color: '#F56C6C', icon: CircleClose },
  { label: '过期订单数', value: stats.value.expiredOrderCount || 0, color: '#909399', icon: Clock },
  { label: '退款订单数', value: stats.value.refundedOrderCount || 0, color: '#9b59b6', icon: Money }
])

const totalReservation = computed(() => filteredActivities.value.reduce((s, a) => s + (reservationMap.value[a.id] || 0), 0))
const totalTickets = computed(() => filteredActivities.value.reduce((s, a) => s + (a.totalTickets || 0), 0))
const totalSold = computed(() => filteredActivities.value.reduce((s, a) => s + (a.soldTickets || 0), 0))
const totalView = computed(() => filteredActivities.value.reduce((s, a) => s + (a.viewCount || 0), 0))
const totalRevenue = computed(() => filteredActivities.value.reduce((s, a) => s + revenue(a), 0))

const overviewCards = computed(() => [
  { label: '活动总数', raw: filteredActivities.value.length, color: '#409EFF', icon: Document },
  { label: '预约总人数', raw: totalReservation.value, color: '#67C23A', icon: User },
  { label: '购票总量', raw: totalSold.value, color: '#E6A23C', icon: ShoppingCart },
  { label: '总浏览量', raw: totalView.value, color: '#F56C6C', icon: DataAnalysis },
  { label: '预计营收', raw: '¥' + totalRevenue.value.toLocaleString(), color: '#909399', icon: Money }
])

// 转化分析：浏览 -> 预约 -> 购票 -> 售罄
const detailView = computed(() => hotStats.value.viewCount || currentActivity.value.viewCount || 0)
const detailReservation = computed(() => hotStats.value.reservationCount || 0)
const detailSold = computed(() => hotStats.value.soldTickets || currentActivity.value.soldTickets || 0)
const detailTotal = computed(() => currentActivity.value.totalTickets || 0)

const percentOf = (child, parent) => (parent > 0 ? Math.min(100, Math.round((child / parent) * 100)) : 0)

const convertList = computed(() => {
  const view = detailView.value
  const reservation = detailReservation.value
  const sold = detailSold.value
  const total = detailTotal.value
  const totalOrders = (stats.value.successOrderCount || 0) + (stats.value.cancelOrderCount || 0) +
    (stats.value.expiredOrderCount || 0) + (stats.value.refundedOrderCount || 0)
  return [
    {
      label: '浏览 → 预约',
      percent: percentOf(reservation, view),
      color: '#409EFF',
      text: `${percentOf(reservation, view)}%`,
      desc: `浏览量 ${view} 次，产生预约 ${reservation} 人`
    },
    {
      label: '预约 → 购票',
      percent: percentOf(sold, reservation),
      color: '#67C23A',
      text: `${percentOf(sold, reservation)}%`,
      desc: `预约 ${reservation} 人，实际购票 ${sold} 张`
    },
    {
      label: '售罄进度',
      percent: percentOf(sold, total),
      color: '#E6A23C',
      text: `${percentOf(sold, total)}%`,
      desc: `总票量 ${total} 张，已售 ${sold} 张，剩余 ${Math.max(0, total - sold)} 张`
    },
    {
      label: '订单成交率',
      percent: percentOf(stats.value.successOrderCount || 0, totalOrders),
      color: '#F56C6C',
      text: `${percentOf(stats.value.successOrderCount || 0, totalOrders)}%`,
      desc: `共 ${totalOrders} 笔订单，其中成交 ${stats.value.successOrderCount || 0} 笔`
    }
  ]
})

const loadActivities = async () => {
  loading.value = true
  try {
    const res = await getMyActivities({ page: 1, size: 100 })
    const data = res.data || res
    activities.value = data.records || data.list || []
    await loadReservationMap()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadReservationMap = async () => {
  if (!activities.value.length) return
  try {
    const entries = await Promise.all(
      activities.value.map(a =>
        getActivityHotStats(a.id)
          .then(r => ({ id: a.id, data: r.data || r || {} }))
          .catch(() => ({ id: a.id, data: {} }))
      )
    )
    const map = {}
    entries.forEach(e => { map[e.id] = e.data.reservationCount || 0 })
    reservationMap.value = map
  } catch (e) {
    console.error(e)
  }
}

const loadStats = async () => {
  if (!selectedActivity.value) {
    stats.value = {}
    hotStats.value = {}
    return
  }
  try {
    const [statsRes, hotRes] = await Promise.all([
      getActivityStats(selectedActivity.value),
      getActivityHotStats(selectedActivity.value)
    ])
    stats.value = statsRes.data || statsRes || {}
    hotStats.value = hotRes.data || hotRes || {}
  } catch (e) {
    console.error(e)
  }
}

const handleSelect = () => {
  if (!selectedActivity.value) {
    stats.value = {}
    hotStats.value = {}
    renderCharts()
    return
  }
  loading.value = true
  loadStats().then(() => {
    loading.value = false
    renderCharts()
  })
}

const refresh = async () => {
  await loadActivities()
  if (selectedActivity.value) await loadStats()
  renderCharts()
}

const goEdit = (row) => {
  router.push(`/operator/activity/edit/${row.id}`)
}

const goDetail = (row) => {
  const route = router.resolve(`/activity/${row.id}`)
  window.open(route.href, '_blank')
}

const rate = (row) => {
  const total = row.totalTickets || 0
  if (!total) return 0
  return Math.min(100, Math.round(((row.soldTickets || 0) / total) * 100))
}

const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

const shortTitle = (t) => {
  if (!t) return '-'
  return t.length > 8 ? t.slice(0, 8) + '…' : t
}

const palette = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9b59b6', '#1abc9c', '#e67e22']

const renderOverview = () => {
  const acts = filteredActivities.value
  if (!acts.length) return
  const names = acts.map(a => shortTitle(a.title))
  const sold = acts.map(a => a.soldTickets || 0)
  const remain = acts.map(a => (a.totalTickets || 0) - (a.soldTickets || 0))
  const reservation = acts.map(a => reservationMap.value[a.id] || 0)
  const view = acts.map(a => a.viewCount || 0)

  if (charts[0]) {
    charts[0].setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['已售票', '未售票'] },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: 30 } },
      yAxis: { type: 'value', name: '张数' },
      series: [
        { name: '已售票', type: 'bar', stack: 't', barWidth: 26, itemStyle: { color: '#409EFF', borderRadius: [0, 0, 0, 0] }, data: sold },
        { name: '未售票', type: 'bar', stack: 't', barWidth: 26, itemStyle: { color: '#dcdfe6' }, data: remain }
      ]
    })
  }

  if (charts[1]) {
    charts[1].setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 张 ({d}%)' },
      legend: { orient: 'vertical', left: 'left', type: 'scroll' },
      series: [{
        name: '购票占比',
        type: 'pie',
        radius: ['40%', '68%'],
        center: ['52%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: acts.map((a, i) => ({ name: shortTitle(a.title), value: a.soldTickets || 0, itemStyle: { color: palette[i % palette.length] } }))
      }]
    })
  }

  if (charts[2]) {
    charts[2].setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['预约人数', '浏览量'] },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: 30 } },
      yAxis: { type: 'value' },
      series: [
        { name: '预约人数', type: 'bar', barWidth: 16, itemStyle: { color: '#67C23A' }, data: reservation },
        { name: '浏览量', type: 'bar', barWidth: 16, itemStyle: { color: '#E6A23C' }, data: view }
      ]
    })
  }

  if (charts[5]) {
    const steps = [
      { name: '浏览量', value: totalView.value, itemStyle: { color: '#F56C6C' } },
      { name: '预约人数', value: totalReservation.value, itemStyle: { color: '#67C23A' } },
      { name: '购票张数', value: totalSold.value, itemStyle: { color: '#409EFF' } }
    ]
    charts[5].setOption({
      tooltip: { trigger: 'item', formatter: '{b}：{c}' },
      legend: { bottom: 0, data: steps.map(s => s.name) },
      series: [{
        name: '整体转化',
        type: 'funnel',
        left: '10%',
        right: '10%',
        top: 30,
        bottom: 40,
        minSize: '28%',
        sort: 'none',
        gap: 4,
        label: { formatter: '{b} {c}' },
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        data: steps
      }]
    })
  }
}

const renderDetail = () => {
  const act = activities.value.find(a => a.id === selectedActivity.value)
  if (!act) return
  const reservation = hotStats.value.reservationCount || 0
  const sold = hotStats.value.soldTickets || act.soldTickets || 0
  const view = hotStats.value.viewCount || act.viewCount || 0
  const total = act.totalTickets || 0

  if (charts[3]) {
    charts[3].setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: ['预约人数', '购票张数', '浏览量'] },
      yAxis: { type: 'value' },
      series: [{
        name: '数量',
        type: 'bar',
        barWidth: 40,
        itemStyle: { color: '#409EFF', borderRadius: [6, 6, 0, 0] },
        data: [reservation, sold, view]
      }]
    })
  }

  if (charts[4]) {
    charts[4].setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 张 ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        name: '售票占比',
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '45%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        data: [
          { name: '已售票', value: sold, itemStyle: { color: '#409EFF' } },
          { name: '未售票', value: Math.max(0, total - sold), itemStyle: { color: '#dcdfe6' } }
        ]
      }]
    })
  }

  if (charts[6]) {
    const orderData = [
      { name: '已成交', value: stats.value.successOrderCount || 0, itemStyle: { color: '#67C23A' } },
      { name: '已取消', value: stats.value.cancelOrderCount || 0, itemStyle: { color: '#F56C6C' } },
      { name: '已过期', value: stats.value.expiredOrderCount || 0, itemStyle: { color: '#909399' } },
      { name: '已退款', value: stats.value.refundedOrderCount || 0, itemStyle: { color: '#E6A23C' } }
    ].filter(i => i.value > 0)
    charts[6].setOption({
      tooltip: { trigger: 'item', formatter: '{b}：{c} 笔 ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        name: '订单构成',
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '45%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        data: orderData.length ? orderData : [{ name: '暂无订单', value: 0, itemStyle: { color: '#dcdfe6' } }]
      }]
    })
  }
}

const disposeCharts = () => {
  charts.forEach(c => c && c.dispose())
  charts = []
}

const renderCharts = () => {
  nextTick(() => {
    disposeCharts()
    // 只初始化当前可见区域的图表，避免隐藏容器尺寸为 0 的告警
    if (selectedActivity.value) {
      if (detailBarRef.value) charts[3] = echarts.init(detailBarRef.value)
      if (detailPieRef.value) charts[4] = echarts.init(detailPieRef.value)
      if (orderPieRef.value) charts[6] = echarts.init(orderPieRef.value)
      renderDetail()
    } else {
      if (overviewBarRef.value) charts[0] = echarts.init(overviewBarRef.value)
      if (overviewPieRef.value) charts[1] = echarts.init(overviewPieRef.value)
      if (browseBarRef.value) charts[2] = echarts.init(browseBarRef.value)
      if (funnelRef.value) charts[5] = echarts.init(funnelRef.value)
      renderOverview()
    }
  })
}

const resizeCharts = () => {
  charts.forEach(c => c && c.resize())
}

onMounted(async () => {
  await loadActivities()
  await nextTick()
  // 等待布局完成后再初始化图表，避免容器尺寸为 0 的告警
  setTimeout(() => {
    renderCharts()
  }, 60)
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach(c => c && c.dispose())
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 10px;
}

.filter-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.filter-left {
  display: flex;
  align-items: center;
  color: #303133;
}

.filter-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.filter-sub {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.overview-cards {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.overview-cards .stat-card {
  flex: 1;
  margin-bottom: 0;
}

.stat-card {
  margin-bottom: 10px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
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
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.chart {
  width: 100%;
  height: 320px;
}

.cell-title {
  color: #303133;
  font-weight: 500;
}

.cell-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.cell-tickets {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sell-rate {
  width: 36px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.cell-ticket-num {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.card-head-tip {
  font-size: 12px;
  color: #909399;
}

.act-info-card {
  margin-top: 16px;
}

.act-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.act-cover {
  width: 96px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
  overflow: hidden;
}

.act-cover-empty {
  background: #f0f2f5;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.act-info-main {
  flex: 1;
  min-width: 0;
}

.act-info-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.act-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.act-info-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12px;
  color: #909399;
}

.act-info-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.act-info-actions {
  flex-shrink: 0;
}

.convert-list {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.convert-item {
  flex: 1;
  min-width: 220px;
}

.convert-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 6px;
}

.convert-label {
  font-size: 13px;
  color: #606266;
}

.convert-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.convert-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #a8abb2;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
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

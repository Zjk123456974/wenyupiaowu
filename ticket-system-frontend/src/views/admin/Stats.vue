<template>
  <div class="admin-stats">
    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalUsers || 0 }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon :size="28"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalActivities || 0 }}</div>
              <div class="stat-label">总活动数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C">
              <el-icon :size="28"><List /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
              <div class="stat-label">总订单量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #F56C6C">
              <el-icon :size="28"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ overview.totalAmount || 0 }}</div>
              <div class="stat-label">总交易额</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度订单趋势 + 用户增长趋势 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header><span>月度订单趋势</span></template>
          <div ref="orderChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>用户增长趋势</span></template>
          <div ref="userChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门活动排行 + 活动分类占比 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card>
          <template #header><span>热门活动排行 Top10</span></template>
          <div ref="hotChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><span>活动分类占比</span></template>
          <div ref="categoryChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 票务资源利用率 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover" class="util-card">
          <el-statistic title="售出率" :value="utilization.sellRate || 0" suffix="%">
            <template #prefix>
              <el-icon style="color: #67C23A"><CircleCheck /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="util-card">
          <el-statistic title="过期释放率" :value="utilization.expiredRate || 0" suffix="%">
            <template #prefix>
              <el-icon style="color: #E6A23C"><Clock /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="util-card">
          <el-statistic title="退票率" :value="utilization.refundRate || 0" suffix="%">
            <template #prefix>
              <el-icon style="color: #F56C6C"><CircleClose /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近7天订单统计 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header><span>最近7天订单统计</span></template>
          <div ref="dailyChartRef" style="height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据汇总分析 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header><span>数据汇总分析</span></template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="总用户数">{{ summary.totalUsers || 0 }}</el-descriptions-item>
            <el-descriptions-item label="本月新增用户">{{ summary.monthlyNewUsers || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总活动数">{{ summary.totalActivities || 0 }}</el-descriptions-item>
            <el-descriptions-item label="上线活动数">{{ summary.onlineActivities || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总订单数">{{ summary.totalOrders || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已支付订单">{{ summary.paidOrders || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已取消订单">{{ summary.cancelledOrders || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已过期订单">{{ summary.expiredOrders || 0 }}</el-descriptions-item>
            <el-descriptions-item label="总交易额">¥{{ summary.totalAmount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="支付率">{{ summary.paidRate || 0 }}%</el-descriptions-item>
            <el-descriptions-item label="总票数">{{ summary.totalTickets || 0 }}</el-descriptions-item>
            <el-descriptions-item label="已售票数">{{ summary.soldTickets || 0 }}</el-descriptions-item>
            <el-descriptions-item label="整体售出率">{{ summary.overallSellRate || 0 }}%</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getOverview, getHotActivitiesStats, getMonthlyOrders, getUserGrowth,
  getCategoryDistribution, getTicketUtilization, getDailyOrders, getWeeklyOrders, getSummary
} from '@/api/admin'
import { User, Tickets, List, Money, CircleCheck, Clock, CircleClose } from '@element-plus/icons-vue'

const overview = ref({})
const utilization = ref({})
const summary = ref({})

const orderChartRef = ref(null)
const userChartRef = ref(null)
const hotChartRef = ref(null)
const categoryChartRef = ref(null)
const dailyChartRef = ref(null)

let orderChart = null
let userChart = null
let hotChart = null
let categoryChart = null
let dailyChart = null

const handleResize = () => {
  orderChart?.resize()
  userChart?.resize()
  hotChart?.resize()
  categoryChart?.resize()
  dailyChart?.resize()
}

const initOrderChart = (data) => {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  const months = data.map(item => item.month || item.date || '')
  const counts = data.map(item => item.count || item.orderCount || 0)
  orderChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{
      name: '订单量',
      type: 'line',
      data: counts,
      smooth: true,
      areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
      lineStyle: { color: '#409EFF' },
      itemStyle: { color: '#409EFF' }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  })
}

const initUserChart = (data) => {
  if (!userChartRef.value) return
  userChart = echarts.init(userChartRef.value)
  const months = data.map(item => item.month || item.date || '')
  const counts = data.map(item => item.count || item.userCount || 0)
  userChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: months, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{
      name: '新增用户',
      type: 'line',
      data: counts,
      smooth: true,
      areaStyle: { color: 'rgba(103, 194, 58, 0.2)' },
      lineStyle: { color: '#67C23A' },
      itemStyle: { color: '#67C23A' }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  })
}

const initHotChart = (data) => {
  if (!hotChartRef.value) return
  hotChart = echarts.init(hotChartRef.value)
  const names = data.map(item => item.title || item.name || '').reverse()
  const purchaseCounts = data.map(item => item.soldTickets || item.purchaseCount || 0).reverse()
  hotChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names },
    series: [{
      name: '购票量',
      type: 'bar',
      data: purchaseCounts,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#67C23A' }
        ])
      }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  })
}

const initCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  categoryChart = echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      name: '活动分类',
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      emphasis: {
        label: { show: true, fontSize: 16, fontWeight: 'bold' }
      },
      data: Array.isArray(data) ? data : []
    }]
  })
}

const initDailyChart = (data) => {
  if (!dailyChartRef.value) return
  dailyChart = echarts.init(dailyChartRef.value)
  const dates = data.map(item => item.date || '')
  const totalOrders = data.map(item => item.totalOrders || 0)
  const paidOrders = data.map(item => item.paidOrders || 0)
  const cancelledOrders = data.map(item => item.cancelledOrders || 0)
  const expiredOrders = data.map(item => item.expiredOrders || 0)
  dailyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总订单', '已支付', '已取消', '已过期'], top: 0 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      {
        name: '总订单',
        type: 'line',
        data: totalOrders,
        smooth: true,
        lineStyle: { color: '#409EFF' },
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '已支付',
        type: 'line',
        data: paidOrders,
        smooth: true,
        lineStyle: { color: '#67C23A' },
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '已取消',
        type: 'line',
        data: cancelledOrders,
        smooth: true,
        lineStyle: { color: '#E6A23C' },
        itemStyle: { color: '#E6A23C' }
      },
      {
        name: '已过期',
        type: 'line',
        data: expiredOrders,
        smooth: true,
        lineStyle: { color: '#F56C6C' },
        itemStyle: { color: '#F56C6C' }
      }
    ],
    grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true }
  })
}

const loadData = async () => {
  try {
    const [
      overviewRes, ordersRes, growthRes, hotRes,
      categoryRes, utilizationRes, dailyRes, summaryRes
    ] = await Promise.all([
      getOverview(),
      getMonthlyOrders(),
      getUserGrowth(),
      getHotActivitiesStats(),
      getCategoryDistribution(),
      getTicketUtilization(),
      getDailyOrders(),
      getSummary()
    ])

    overview.value = overviewRes.data || overviewRes || {}
    utilization.value = utilizationRes.data || utilizationRes || {}
    summary.value = summaryRes.data || summaryRes || {}

    const ordersData = ordersRes.data || ordersRes || []
    const growthData = growthRes.data || growthRes || []
    const hotData = hotRes.data || hotRes || []
    const categoryData = categoryRes.data || categoryRes || []
    const dailyData = dailyRes.data || dailyRes || []

    await nextTick()
    initOrderChart(Array.isArray(ordersData) ? ordersData : [])
    initUserChart(Array.isArray(growthData) ? growthData : [])
    initHotChart(Array.isArray(hotData) ? hotData : [])
    initCategoryChart(Array.isArray(categoryData) ? categoryData : [])
    initDailyChart(Array.isArray(dailyData) ? dailyData : [])
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  orderChart?.dispose()
  userChart?.dispose()
  hotChart?.dispose()
  categoryChart?.dispose()
  dailyChart?.dispose()
})
</script>

<style scoped>
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
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.util-card {
  text-align: center;
}

.util-card :deep(.el-statistic__head) {
  font-size: 15px;
  color: #909399;
  margin-bottom: 8px;
}

.util-card :deep(.el-statistic__content) {
  font-size: 32px;
  font-weight: bold;
}
</style>

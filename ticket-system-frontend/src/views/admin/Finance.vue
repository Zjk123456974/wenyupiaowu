<template>
  <div class="finance-page">
    <!-- 财务汇总卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon :size="28"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ summary.totalIncome || 0 }}</div>
              <div class="stat-label">总收入</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #F56C6C">
              <el-icon :size="28"><Wallet /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ summary.totalRefund || 0 }}</div>
              <div class="stat-label">总退款</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ summary.netRevenue || 0 }}</div>
              <div class="stat-label">净营收</div>
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
              <div class="stat-value">{{ summary.totalFlowCount || 0 }}</div>
              <div class="stat-label">流水总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据导出 -->
    <el-card style="margin-top: 20px">
      <template #header><span>数据导出</span></template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-button type="primary" plain :icon="Download" @click="handleExport('orders')" :loading="exportLoading === 'orders'">
            导出订单数据
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" plain :icon="Download" @click="handleExport('users')" :loading="exportLoading === 'users'">
            导出用户数据
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="warning" plain :icon="Download" @click="handleExport('activities')" :loading="exportLoading === 'activities'">
            导出活动数据
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="danger" plain :icon="Download" @click="handleExport('finance')" :loading="exportLoading === 'finance'">
            导出财务流水
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 交易流水明细 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>交易流水明细</span>
          <div class="filter-area">
            <el-select v-model="filter.flowType" placeholder="流水类型" clearable style="width: 120px">
              <el-option label="收入" :value="1" />
              <el-option label="退款" :value="2" />
              <el-option label="手续费" :value="3" />
            </el-select>
            <el-date-picker v-model="filter.timeRange" type="datetimerange" range-separator="至"
              start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 360px" />
            <el-button type="primary" @click="fetchFlows">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </div>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="flows" v-loading="flowLoading" stripe>
          <el-table-column label="流水编号" prop="flowNo" width="200" />
          <el-table-column label="订单号" prop="orderNo" width="200" />
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="活动名称" prop="activityTitle" show-overflow-tooltip min-width="180" />
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.flowType === 1 ? '#67C23A' : '#F56C6C' }">
                {{ row.flowType === 1 ? '+' : '-' }}¥{{ row.amount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="flowTypeTag(row.flowType)" size="small">{{ flowTypeName(row.flowType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="交易时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.payTime) }}
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" show-overflow-tooltip min-width="120" />
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
          @size-change="fetchFlows"
          @current-change="fetchFlows"
        />
      </div>
    </el-card>

    <!-- 每日对账 -->
    <el-card style="margin-top: 20px">
      <template #header><span>每日对账（近30天）</span></template>
      <div class="table-scroll">
        <el-table :data="dailyList" v-loading="dailyLoading" stripe max-height="400">
          <el-table-column label="日期" prop="date" width="150" />
          <el-table-column label="收入" width="140" align="right">
            <template #default="{ row }">
              <span style="color: #67C23A">¥{{ row.income }}</span>
            </template>
          </el-table-column>
          <el-table-column label="退款" width="140" align="right">
            <template #default="{ row }">
              <span style="color: #F56C6C">¥{{ row.refund }}</span>
            </template>
          </el-table-column>
          <el-table-column label="净收入" width="140" align="right">
            <template #default="{ row }">
              ¥{{ (Number(row.income) - Number(row.refund)).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="订单数" prop="orderCount" width="100" align="center" />
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, Wallet, TrendCharts, List, Download } from '@element-plus/icons-vue'
import {
  getFinanceSummary, getDailyReconciliation, getFinanceFlows,
  exportOrders, exportUsers, exportActivities, exportFinance
} from '@/api/finance'

const summary = ref({})
const flows = ref([])
const dailyList = ref([])
const total = ref(0)
const flowLoading = ref(false)
const dailyLoading = ref(false)
const exportLoading = ref('')

const filter = ref({
  page: 1,
  size: 10,
  flowType: null,
  timeRange: null
})

const fetchSummary = async () => {
  try {
    const res = await getFinanceSummary()
    summary.value = res?.data || res || {}
  } catch {
    // 静默处理
  }
}

const fetchFlows = async () => {
  flowLoading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      flowType: filter.value.flowType || undefined
    }
    if (filter.value.timeRange && filter.value.timeRange.length === 2) {
      params.startTime = filter.value.timeRange[0]
      params.endTime = filter.value.timeRange[1]
    }
    const res = await getFinanceFlows(params)
    const data = res?.data || res
    flows.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 静默处理
  } finally {
    flowLoading.value = false
  }
}

const fetchDaily = async () => {
  dailyLoading.value = true
  try {
    const res = await getDailyReconciliation()
    const data = res?.data || res || {}
    dailyList.value = data.dailyList || []
  } catch {
    // 静默处理
  } finally {
    dailyLoading.value = false
  }
}

const resetFilter = () => {
  filter.value.flowType = null
  filter.value.timeRange = null
  filter.value.page = 1
  fetchFlows()
}

const handleExport = async (type) => {
  exportLoading.value = type
  try {
    let response
    const params = {}
    if (filter.value.timeRange && filter.value.timeRange.length === 2) {
      params.startTime = filter.value.timeRange[0]
      params.endTime = filter.value.timeRange[1]
    }

    if (type === 'orders') {
      response = await exportOrders(params)
    } else if (type === 'users') {
      response = await exportUsers()
    } else if (type === 'activities') {
      response = await exportActivities()
    } else if (type === 'finance') {
      response = await exportFinance(params)
    }

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const names = { orders: '订单数据', users: '用户数据', activities: '活动数据', finance: '财务流水' }
    link.download = `${names[type]}_${new Date().toLocaleDateString()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = ''
  }
}

const flowTypeName = (type) => {
  const map = { 1: '收入', 2: '退款', 3: '手续费' }
  return map[type] || '未知'
}

const flowTypeTag = (type) => {
  const map = { 1: 'success', 2: 'danger', 3: 'warning' }
  return map[type] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

onMounted(() => {
  fetchSummary()
  fetchFlows()
  fetchDaily()
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-area {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .finance-page {
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

<template>
  <div class="consumption-page">
    <div class="page-header">
      <h2>消费记录</h2>
      <p class="page-desc">查看您的消费统计与交易流水</p>
    </div>

    <!-- 消费统计卡片 -->
    <div class="stats-row" v-loading="statsLoading">
      <el-card shadow="hover" class="stat-card primary">
        <div class="stat-icon-wrapper">
          <el-icon class="stat-icon"><Wallet /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">累计消费</div>
          <div class="stat-value">¥{{ formatMoney(stats.totalSpent) }}</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card success">
        <div class="stat-icon-wrapper">
          <el-icon class="stat-icon"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">已支付订单</div>
          <div class="stat-value">{{ stats.paidCount || 0 }} 笔</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card warning">
        <div class="stat-icon-wrapper">
          <el-icon class="stat-icon"><RefreshLeft /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">已退款金额</div>
          <div class="stat-value">¥{{ formatMoney(stats.totalRefunded) }}</div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card info">
        <div class="stat-icon-wrapper">
          <el-icon class="stat-icon"><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">净消费</div>
          <div class="stat-value">¥{{ formatMoney(stats.netSpent) }}</div>
        </div>
      </el-card>
    </div>

    <!-- 订单状态分布 -->
    <div class="status-distribution">
      <el-card shadow="never">
        <template #header>
          <span class="card-title">订单状态分布</span>
        </template>
        <div class="status-list">
          <div class="status-item">
            <span class="status-dot paid"></span>
            <span class="status-name">已支付</span>
            <span class="status-count">{{ stats.paidCount || 0 }} 笔</span>
          </div>
          <div class="status-item">
            <span class="status-dot cancelled"></span>
            <span class="status-name">已取消</span>
            <span class="status-count">{{ stats.cancelledCount || 0 }} 笔</span>
          </div>
          <div class="status-item">
            <span class="status-dot expired"></span>
            <span class="status-name">已过期</span>
            <span class="status-count">{{ stats.expiredCount || 0 }} 笔</span>
          </div>
          <div class="status-item">
            <span class="status-dot refunded"></span>
            <span class="status-name">已退款</span>
            <span class="status-count">{{ stats.refundedCount || 0 }} 笔</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 交易流水 -->
    <div class="flows-section">
      <div class="section-header">
        <span class="section-title">交易流水明细</span>
        <el-select v-model="flowTypeFilter" placeholder="全部类型" clearable size="small" style="width: 140px" @change="loadFlows">
          <el-option label="全部类型" value="" />
          <el-option label="支付(收入)" :value="1" />
          <el-option label="退款" :value="2" />
          <el-option label="手续费" :value="3" />
        </el-select>
      </div>

      <div class="table-scroll">
        <el-table :data="flows" stripe v-loading="flowsLoading" style="width: 100%">
          <el-table-column prop="flowNo" label="流水号" min-width="180" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getFlowTypeTag(row.flowType)" size="small">
                {{ getFlowTypeText(row.flowType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span :class="['amount', row.flowType === 1 ? 'income' : 'expense']">
                {{ row.flowType === 1 ? '+' : '-' }}¥{{ formatMoney(row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column label="交易时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.payTime) }}
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>

      <el-empty v-if="flows.length === 0 && !flowsLoading" description="暂无交易记录" />

      <div class="pagination-wrap" v-if="flowsTotal > 0">
        <el-pagination
          v-model:current-page="flowsPage"
          v-model:page-size="flowsSize"
          :total="flowsTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          @size-change="loadFlows"
          @current-change="loadFlows"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getConsumptionStats, getConsumptionFlows } from '@/api/user'

const statsLoading = ref(false)
const flowsLoading = ref(false)
const stats = ref({})
const flows = ref([])
const flowsPage = ref(1)
const flowsSize = ref(10)
const flowsTotal = ref(0)
const flowTypeFilter = ref('')

const formatMoney = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getFlowTypeText = (type) => {
  const map = { 1: '支付', 2: '退款', 3: '手续费' }
  return map[type] ?? '未知'
}

const getFlowTypeTag = (type) => {
  const map = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[type] ?? 'info'
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getConsumptionStats()
    stats.value = res.data || {}
  } catch {
    // ignore
  } finally {
    statsLoading.value = false
  }
}

const loadFlows = async () => {
  flowsLoading.value = true
  try {
    const params = {
      page: flowsPage.value,
      size: flowsSize.value
    }
    if (flowTypeFilter.value !== '' && flowTypeFilter.value !== null) {
      params.flowType = flowTypeFilter.value
    }
    const res = await getConsumptionFlows(params)
    const data = res.data || res
    flows.value = data.records || data.list || []
    flowsTotal.value = data.total || 0
  } catch {
    // ignore
  } finally {
    flowsLoading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadFlows()
})
</script>

<style scoped>
.consumption-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 22px;
  color: #303133;
  margin-bottom: 6px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  width: 100%;
}

.stat-icon-wrapper {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.primary .stat-icon-wrapper {
  background: #ecf5ff;
}

.stat-card.primary .stat-icon {
  color: #409eff;
}

.stat-card.success .stat-icon-wrapper {
  background: #f0f9eb;
}

.stat-card.success .stat-icon {
  color: #67c23a;
}

.stat-card.warning .stat-icon-wrapper {
  background: #fdf6ec;
}

.stat-card.warning .stat-icon {
  color: #e6a23c;
}

.stat-card.info .stat-icon-wrapper {
  background: #f4f4f5;
}

.stat-card.info .stat-icon {
  color: #909399;
}

.stat-icon {
  font-size: 24px;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

/* 订单状态分布 */
.status-distribution {
  margin-bottom: 24px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

.status-list {
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.paid {
  background: #67c23a;
}

.status-dot.cancelled {
  background: #909399;
}

.status-dot.expired {
  background: #e6a23c;
}

.status-dot.refunded {
  background: #f56c6c;
}

.status-name {
  font-size: 14px;
  color: #606266;
}

.status-count {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

/* 交易流水 */
.flows-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.amount {
  font-weight: 600;
}

.amount.income {
  color: #67c23a;
}

.amount.expense {
  color: #f56c6c;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 20px 0 0;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .consumption-page {
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

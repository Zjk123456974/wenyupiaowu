<template>
  <el-card shadow="never" class="refund-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">退票记录</span>
        <el-button type="primary" :icon="Refresh" @click="fetchRefunds">刷新</el-button>
      </div>
    </template>

    <el-alert
      title="退票说明"
      type="info"
      :closable="false"
      show-icon
      class="refund-tip"
    >
      <template #default>
        如需退票，请在订单详情中发起退票申请。退票申请提交后，客服将在 1-3 个工作日内审核，
        审核通过后退款将原路退回至您的支付账户。
      </template>
    </el-alert>

    <div class="table-scroll">
      <el-table
        :data="refunds"
        v-loading="loading"
        stripe
        border
        class="refund-table"
        empty-text="暂无退票记录"
      >
        <el-table-column label="工单号" prop="refundNo" min-width="180" align="center">
          <template #default="{ row }">
            <span class="refund-no">{{ row.refundNo || row.refundCode || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="订单号" prop="orderNo" min-width="180" align="center">
          <template #default="{ row }">
            <span>{{ row.orderNo || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="活动标题" prop="activityTitle" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.activityTitle || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="退款金额" prop="refundAmount" width="110" align="center">
          <template #default="{ row }">
            <span class="amount-text">¥ {{ formatAmount(row.refundAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="手续费" prop="feeAmount" width="100" align="center">
          <template #default="{ row }">
            <span class="fee-text">¥ {{ formatAmount(row.feeAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="实退金额" prop="actualAmount" width="110" align="center">
          <template #default="{ row }">
            <span class="actual-amount">¥ {{ formatAmount(row.actualAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="退票原因" prop="reason" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.reason || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="160" align="center">
          <template #default="{ row }">
            <span>{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              size="small"
              @click="handleViewDetail(row)"
            >详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>
      </el-table>
    </div>

    <div v-if="total > pageSize" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchRefunds"
      />
    </div>

    <!-- 退票详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="退票详情"
      width="560px"
      destroy-on-close
    >
      <div v-if="detailData">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="工单号">{{ detailData.refundNo || detailData.refundCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detailData.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="活动标题">{{ detailData.activityTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款金额">¥ {{ formatAmount(detailData.refundAmount) }}</el-descriptions-item>
          <el-descriptions-item label="手续费">¥ {{ formatAmount(detailData.feeAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实退金额">
            <span class="actual-amount">¥ {{ formatAmount(detailData.actualAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="退票原因">{{ detailData.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)" effect="light">
              {{ statusText(detailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间" v-if="detailData.auditTime">
            {{ formatTime(detailData.auditTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="退款时间" v-if="detailData.refundTime">
            {{ formatTime(detailData.refundTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核备注" v-if="detailData.auditRemark">
            {{ detailData.auditRemark }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getMyRefunds } from '@/api/refund'

const loading = ref(false)
const refunds = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const detailData = ref(null)

const statusMap = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
  3: '已退款'
}

const statusTagTypeMap = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'info'
}

const statusText = (status) => statusMap[status] ?? '未知'
const statusTagType = (status) => statusTagTypeMap[status] ?? 'info'

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const formatAmount = (val) => {
  const num = Number(val || 0)
  return num.toFixed(2)
}

const fetchRefunds = async () => {
  loading.value = true
  try {
    const res = await getMyRefunds()
    let list = res.data?.records || res.data?.list || res.data || []
    if (!Array.isArray(list)) list = []
    total.value = list.length
    // 简单分页
    const start = (page.value - 1) * pageSize.value
    refunds.value = list.slice(start, start + pageSize.value)
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (row) => {
  detailData.value = row
  detailDialogVisible.value = true
}

onMounted(() => {
  fetchRefunds()
})
</script>

<style scoped>
.refund-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.refund-tip {
  margin-bottom: 16px;
}

.refund-table {
  width: 100%;
}

.refund-no {
  font-family: monospace;
  color: #303133;
  font-weight: 500;
}

.amount-text {
  color: #606266;
}

.fee-text {
  color: #e6a23c;
}

.actual-amount {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .refund-card {
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

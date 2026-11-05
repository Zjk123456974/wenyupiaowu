<template>
  <div class="order-manage">
    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="5">
          <el-input v-model="keyword" placeholder="活动关键词" clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="4">
          <el-select v-model="statusFilter" placeholder="订单状态" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已过期" :value="3" />
            <el-option label="已退款" :value="5" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="3">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 首屏骨架屏：列数与真实表格匹配，避免加载完成后的布局跳动 -->
    <div v-if="loading && orders.length === 0" class="table-skeleton">
      <el-skeleton :count="8" animated>
        <template #template>
          <div class="skeleton-row">
            <el-skeleton-item variant="text" style="width: 70%" />
            <el-skeleton-item variant="text" style="width: 45%" />
            <el-skeleton-item variant="text" style="width: 55%" />
            <el-skeleton-item variant="text" style="width: 35%" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else class="table-scroll" style="margin-top: 16px">
      <el-table :data="orders" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="activityTitle" label="活动" min-width="160" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="totalAmount" label="金额" width="90">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="orderStatusTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="80" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="showDetail(row)">查看详情</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="warning"
              text
              size="small"
              @click="handleAbnormal(row)"
            >处理异常</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>
      </el-table>
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
        <el-descriptions-item label="活动">{{ currentOrder.activityTitle }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentOrder.quantity }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ orderStatusLabel(currentOrder.status) }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ sourceLabel(currentOrder.source) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间" :span="2">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 异常处理记录 -->
      <div v-if="handleRecords.length" style="margin-top: 16px">
        <div style="font-weight: 600; margin-bottom: 8px;">异常处理记录</div>
        <el-table :data="handleRecords" size="small" border>
          <el-table-column label="处理方式" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="handleTypeTag(row.handleType)">{{ handleTypeName(row.handleType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理备注" prop="handleRemark" show-overflow-tooltip min-width="140">
            <template #default="{ row }">{{ row.handleRemark || '-' }}</template>
          </el-table-column>
          <el-table-column label="处理人" prop="operatorName" width="100">
            <template #default="{ row }">{{ row.operatorName || '-' }}</template>
          </el-table-column>
          <el-table-column label="处理时间" width="160">
            <template #default="{ row }">{{ formatHandleTime(row.createTime) }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>
    </el-dialog>

    <!-- 异常处理对话框 -->
    <el-dialog v-model="abnormalVisible" title="订单异常处理" width="480px">
      <el-descriptions :column="1" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="orderStatusTagType(currentOrder.status)">{{ orderStatusLabel(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
      </el-descriptions>

      <div class="abnormal-form">
        <div class="abnormal-label">处理方式</div>
        <el-radio-group v-model="abnormalForm.handleType">
          <el-radio :value="1">恢复订单</el-radio>
          <el-radio :value="2">强制取消订单</el-radio>
          <el-radio :value="3">标记已解决</el-radio>
        </el-radio-group>
        <div class="abnormal-tip" v-if="abnormalForm.handleType === 2">
          取消后将释放库存并作废电子票，该操作不可撤销
        </div>
        <div class="abnormal-tip" v-else-if="abnormalForm.handleType === 1">
          保持订单原状态不变，仅标记为正常订单
        </div>
        <div class="abnormal-tip" v-else>
          不变更订单状态，仅记录异常已人工核实解决
        </div>
        <div class="abnormal-label" style="margin-top: 12px;">处理备注</div>
        <el-input
          v-model="abnormalForm.remark"
          type="textarea"
          :rows="3"
          placeholder="选填，例如：核实为用户误操作，已与用户沟通"
        />
      </div>

      <template #footer>
        <el-button @click="abnormalVisible = false">取消</el-button>
        <el-button type="primary" :loading="abnormalLoading" @click="submitAbnormal">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminOrders, handleAbnormalOrder, getHandleRecords } from '@/api/admin'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const statusFilter = ref('')
const dateRange = ref(null)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
// 首屏加载态：用于骨架屏展示，翻页时仍用 v-loading 遮罩
const loading = ref(false)

const orderStatusMap = {
  0: '待支付',
  1: '已支付',
  2: '已取消',
  3: '已过期',
  5: '已退款'
}
const orderStatusTagMap = {
  0: 'warning',
  1: 'success',
  2: 'info',
  3: 'danger',
  5: ''
}
const orderStatusLabel = (status) => orderStatusMap[status] || '未知'
const orderStatusTagType = (status) => orderStatusTagMap[status] || ''

const sourceMap = { 0: '抢票', 1: '预约出票', 2: '补票' }
const sourceLabel = (source) => sourceMap[source] || '未知'

// 详情
const detailVisible = ref(false)
const currentOrder = ref(null)
const handleRecords = ref([])

// 异常处理对话框
const abnormalVisible = ref(false)
const abnormalLoading = ref(false)
const abnormalForm = ref({ handleType: 2, remark: '' })

const showDetail = async (row) => {
  currentOrder.value = row
  detailVisible.value = true
  // 加载该订单的异常处理记录
  handleRecords.value = []
  try {
    const res = await getHandleRecords(row.id)
    handleRecords.value = res.data || res || []
  } catch (e) {
    console.error(e)
  }
}

const handleAbnormal = (row) => {
  currentOrder.value = row
  abnormalForm.value = { handleType: 2, remark: '' }
  abnormalVisible.value = true
}

const submitAbnormal = async () => {
  abnormalLoading.value = true
  try {
    await handleAbnormalOrder(currentOrder.value.id, {
      handleType: abnormalForm.value.handleType,
      remark: abnormalForm.value.remark || undefined
    })
    ElMessage.success('处理完成')
    abnormalVisible.value = false
    loadData()
    // 刷新当前订单的处理记录
    if (detailVisible.value) {
      try {
        const res = await getHandleRecords(currentOrder.value.id)
        handleRecords.value = res.data || res || []
      } catch (e) {
        console.error(e)
      }
    }
  } catch (e) {
    ElMessage.error('处理失败')
  } finally {
    abnormalLoading.value = false
  }
}

const handleTypeName = (type) => {
  const map = { 1: '恢复订单', 2: '强制取消', 3: '标记已解决' }
  return map[type] || '未知'
}

const handleTypeTag = (type) => {
  const map = { 1: 'success', 2: 'danger', 3: 'warning' }
  return map[type] || 'info'
}

const formatHandleTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== '' && statusFilter.value !== null && statusFilter.value !== undefined) params.status = statusFilter.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0] + ' 00:00:00'
      params.endTime = dateRange.value[1] + ' 23:59:59'
    }
    const res = await getAdminOrders(params)
    const data = res.data || res
    orders.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.abnormal-form {
  margin-top: 16px;
}

.abnormal-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-primary);
  margin-bottom: 8px;
}

.abnormal-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-secondary);
}

:deep(.el-table) {
  width: 100%;
  table-layout: fixed;
}

:deep(.el-table th) {
  text-align: center;
}

:deep(.el-table td) {
  text-align: center;
}

:deep(.el-table--border .el-table__cell) {
  border-right: 1px solid #ebeef5;
}

:deep(.el-table--border .el-table__cell:last-child) {
  border-right: none;
}

/* 骨架屏：行高与表格行高接近，加载完成时不产生明显跳动 */
.table-skeleton {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 48px;
  border-bottom: 1px solid #f0f2f5;
}

.skeleton-row:last-child {
  border-bottom: none;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .order-manage {
    max-width: 100%;
    box-sizing: border-box;
  }

  /* 筛选表单：栅格改单列，输入框撑满 */
  .filter-card :deep(.el-row) {
    row-gap: 10px;
  }

  .filter-card :deep(.el-col) {
    flex: 0 0 100%;
    max-width: 100%;
  }

  .filter-card :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }

  /* 表格容器横向滚动，列宽保持不变 */
  .table-scroll {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .table-scroll :deep(.el-table) {
    min-width: 760px;
  }

  .pagination-wrap {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>

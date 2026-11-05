<template>
  <div class="refund-manage-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>退票工单</span>
          <div class="filter-area">
            <el-input
              v-model="filter.keyword"
              placeholder="搜索工单号/订单号/用户名"
              clearable
              style="width: 240px"
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </div>
        </div>
      </template>

      <!-- 状态筛选Tab -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待审核" name="pending" />
        <el-tab-pane label="已通过" name="approved" />
        <el-tab-pane label="已驳回" name="rejected" />
        <el-tab-pane label="已退款" name="refunded" />
      </el-tabs>

      <!-- 首屏骨架屏：列数与真实表格匹配，避免加载完成后的布局跳动 -->
      <div v-if="loading && refunds.length === 0" class="table-skeleton">
        <el-skeleton :count="8" animated>
          <template #template>
            <div class="skeleton-row">
              <el-skeleton-item variant="text" style="width: 65%" />
              <el-skeleton-item variant="text" style="width: 50%" />
              <el-skeleton-item variant="text" style="width: 40%" />
              <el-skeleton-item variant="text" style="width: 30%" />
            </div>
          </template>
        </el-skeleton>
      </div>

      <div v-else class="table-scroll">
        <el-table :data="refunds" v-loading="loading" stripe>
          <el-table-column label="工单号" prop="refundNo" width="180" />
          <el-table-column label="订单号" prop="orderNo" width="180" />
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="活动标题" prop="activityTitle" show-overflow-tooltip min-width="180" />
          <el-table-column label="退款金额" width="110" align="right">
            <template #default="{ row }">
              <span style="color: #F56C6C">¥{{ row.refundAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="手续费" width="100" align="right">
            <template #default="{ row }">
              ¥{{ row.feeAmount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="实退金额" width="110" align="right">
            <template #default="{ row }">
              <span style="color: #67C23A">¥{{ row.actualAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="退票原因" prop="reason" show-overflow-tooltip min-width="160" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" size="small">{{ statusName(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="审核信息" width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <template v-if="row.status === 1 || row.status === 2 || row.status === 3">
                <div>{{ row.auditorName || '-' }} ｜ {{ formatTime(row.auditTime) }}</div>
                <div v-if="row.auditRemark" style="font-size: 12px; color: #909399; margin-top: 2px">
                  {{ row.auditRemark }}
                </div>
              </template>
              <span v-else style="color: #c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'pending' || row.status === 0"
                type="success"
                text
                size="small"
                @click="handleApprove(row)"
              >通过</el-button>
              <el-button
                v-if="row.status === 'pending' || row.status === 0"
                type="danger"
                text
                size="small"
                @click="handleReject(row)"
              >驳回</el-button>
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
          @size-change="fetchRefunds"
          @current-change="fetchRefunds"
        />
      </div>
    </el-card>

    <!-- 通过确认对话框 -->
    <el-dialog v-model="approveDialogVisible" title="通过退票" width="450px">
      <el-form ref="approveFormRef" :model="approveForm" label-width="90px">
        <el-form-item label="工单号">
          <span>{{ currentRefund.refundNo }}</span>
        </el-form-item>
        <el-form-item label="实退金额">
          <span style="color: #67C23A">¥{{ currentRefund.actualAmount }}</span>
        </el-form-item>
        <el-form-item label="审核备注" prop="approveRemark">
          <el-input
            v-model="approveForm.approveRemark"
            type="textarea"
            :rows="3"
            placeholder="选填，例如：核实无误，同意退款"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="approveLoading" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回退票" width="450px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="工单号">
          <span>{{ currentRefund.refundNo }}</span>
        </el-form-item>
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectLoading" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRefundPage, approveRefund, rejectRefund } from '@/api/refund'

const refunds = ref([])
const total = ref(0)
const loading = ref(false)
const activeTab = ref('all')

const filter = ref({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined
})

const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref(null)
const approveDialogVisible = ref(false)
const approveLoading = ref(false)
const approveFormRef = ref(null)
const currentRefund = ref({})
const rejectForm = ref({ rejectReason: '' })
const approveForm = ref({ approveRemark: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }]
}

const statusMap = {
  all: undefined,
  pending: 0,
  approved: 1,
  rejected: 2,
  refunded: 3
}

const fetchRefunds = async () => {
  loading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      keyword: filter.value.keyword || undefined,
      status: filter.value.status || undefined
    }
    const res = await getRefundPage(params)
    const data = res?.data || res
    refunds.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  filter.value.status = statusMap[tab]
  filter.value.page = 1
  fetchRefunds()
}

const handleSearch = () => {
  filter.value.page = 1
  fetchRefunds()
}

const resetFilter = () => {
  filter.value.keyword = ''
  filter.value.page = 1
  fetchRefunds()
}

const handleApprove = (row) => {
  currentRefund.value = row
  approveForm.value = { approveRemark: '' }
  approveDialogVisible.value = true
}

const submitApprove = async () => {
  approveLoading.value = true
  try {
    await approveRefund(currentRefund.value.id, { remark: approveForm.value.approveRemark || undefined })
    ElMessage.success('审核通过，退款已执行')
    approveDialogVisible.value = false
    fetchRefunds()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    approveLoading.value = false
  }
}

const handleReject = (row) => {
  currentRefund.value = row
  rejectForm.value = { rejectReason: '' }
  rejectDialogVisible.value = true
}

const submitReject = async () => {
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return
  rejectLoading.value = true
  try {
    await rejectRefund(currentRefund.value.id, { rejectReason: rejectForm.value.rejectReason })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    fetchRefunds()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    rejectLoading.value = false
  }
}

const statusName = (status) => {
  const map = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    refunded: '已退款',
    0: '待审核',
    1: '已通过',
    2: '已驳回',
    3: '已退款'
  }
  return map[status] || '未知'
}

const statusTag = (status) => {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    refunded: 'info',
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: 'info'
  }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

onMounted(() => {
  fetchRefunds()
})
</script>

<style scoped>
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

/* 骨架屏：行高与表格行高接近，加载完成时不产生明显跳动 */
.table-skeleton {
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
  .refund-manage-page {
    max-width: 100%;
    box-sizing: border-box;
  }

  /* 卡片头部：筛选区换行堆叠 */
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-area {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .filter-area :deep(.el-input) {
    width: 100% !important;
  }

  .filter-area :deep(.el-button) {
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
    min-width: 900px;
  }

  .pagination-wrapper {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>

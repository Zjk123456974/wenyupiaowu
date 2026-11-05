<template>
  <div class="review-manage-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评价管理</span>
          <div class="filter-area">
            <el-input
              v-model="filter.keyword"
              placeholder="搜索活动/用户名"
              clearable
              style="width: 220px"
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
        <el-tab-pane label="已隐藏" name="hidden" />
        <el-tab-pane label="已驳回" name="rejected" />
      </el-tabs>

      <div class="table-scroll">
        <el-table :data="reviews" v-loading="loading" stripe>
          <el-table-column label="活动标题" prop="activityTitle" show-overflow-tooltip min-width="180" />
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="评分" width="150" align="center">
            <template #default="{ row }">
              <el-rate v-model="row.rating" disabled />
            </template>
          </el-table-column>
          <el-table-column label="评价内容" prop="content" show-overflow-tooltip min-width="220" />
          <el-table-column label="图片" width="120" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.images && row.images.length"
                :src="row.images[0]"
                :preview-src-list="row.images"
                fit="cover"
                style="width: 50px; height: 50px; border-radius: 4px"
                preview-teleported
                lazy
                >
                <template #error>
                  <img :src="PLACEHOLDER_IMAGE" class="img-fallback" alt="图片加载失败" />
                </template>
              </el-image>
              <span v-else style="color: #c0c4cc">无</span>
            </template>
          </el-table-column>
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
          <el-table-column label="操作" width="280" fixed="right">
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
              <el-button
                v-if="row.status !== 2"
                type="warning"
                text
                size="small"
                @click="handleHide(row)"
              >隐藏</el-button>
              <el-button
                v-if="row.status === 2"
                type="success"
                text
                size="small"
                @click="handleRestore(row)"
              >恢复展示</el-button>
              <el-button type="primary" text size="small" @click="handleReply(row)">回复</el-button>
              <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
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
          @size-change="fetchReviews"
          @current-change="fetchReviews"
        />
      </div>
    </el-card>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyDialogVisible" title="回复评价" width="500px">
      <el-form ref="replyFormRef" :model="replyForm" :rules="replyRules" label-width="80px">
        <el-form-item label="评价内容">
          <div style="color: #606266; line-height: 1.6">{{ currentReview.content }}</div>
        </el-form-item>
        <el-form-item label="回复内容" prop="replyContent">
          <el-input
            v-model="replyForm.replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replyLoading" @click="submitReply">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getReviewPage, approveReview, rejectReview, hideReview, deleteReview, replyReview
} from '@/api/review'
import { PLACEHOLDER_IMAGE } from '@/utils/placeholder'

const reviews = ref([])
const total = ref(0)
const loading = ref(false)
const activeTab = ref('all')

const filter = ref({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined
})

const replyDialogVisible = ref(false)
const replyLoading = ref(false)
const replyFormRef = ref(null)
const currentReview = ref({})
const replyForm = ref({ replyContent: '' })
const replyRules = {
  replyContent: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

const statusMap = {
  all: undefined,
  pending: 0,
  approved: 1,
  hidden: 2,
  rejected: 3
}

const fetchReviews = async () => {
  loading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      keyword: filter.value.keyword || undefined,
      status: filter.value.status || undefined
    }
    const res = await getReviewPage(params)
    const data = res?.data || res
    reviews.value = data?.records || []
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
  fetchReviews()
}

const handleSearch = () => {
  filter.value.page = 1
  fetchReviews()
}

const resetFilter = () => {
  filter.value.keyword = ''
  filter.value.page = 1
  fetchReviews()
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认通过该评价？`, '审核确认', { type: 'warning' })
    await approveReview(row.id)
    ElMessage.success('已通过')
    fetchReviews()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleReject = async (row) => {
  try {
    await ElMessageBox.confirm(`确认驳回该评价？`, '审核确认', { type: 'warning' })
    await rejectReview(row.id)
    ElMessage.success('已驳回')
    fetchReviews()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleHide = async (row) => {
  try {
    await ElMessageBox.confirm(`确认隐藏该评价？隐藏后用户端不再展示，可随时恢复`, '提示', { type: 'warning' })
    await hideReview(row.id)
    ElMessage.success('已隐藏')
    fetchReviews()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

// 恢复展示：把已隐藏的评价重新上架（复用审核通过接口）
const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(`确认恢复展示该评价？`, '提示', { type: 'info' })
    await approveReview(row.id)
    ElMessage.success('已恢复展示')
    fetchReviews()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除该评价？删除后不可恢复`, '危险操作', { type: 'error' })
    await deleteReview(row.id)
    ElMessage.success('已删除')
    fetchReviews()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const handleReply = (row) => {
  currentReview.value = row
  replyForm.value = { replyContent: row.reply || '' }
  replyDialogVisible.value = true
}

const submitReply = async () => {
  const valid = await replyFormRef.value.validate().catch(() => false)
  if (!valid) return
  replyLoading.value = true
  try {
    await replyReview(currentReview.value.id, { reply: replyForm.value.replyContent })
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    fetchReviews()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    replyLoading.value = false
  }
}

const statusName = (status) => {
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已隐藏',
    3: '已驳回'
  }
  return map[status] || '未知'
}

const statusTag = (status) => {
  const map = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

onMounted(() => {
  fetchReviews()
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

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .review-manage-page {
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

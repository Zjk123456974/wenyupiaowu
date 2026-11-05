<template>
  <div class="my-reviews-page">
    <div class="page-header">
      <div class="header-left">
        <h2>我的评价</h2>
        <p class="page-desc">查看和管理您发布过的所有评价记录</p>
      </div>
      <el-button type="primary" @click="showAddDialog">写评价</el-button>
    </div>

    <!-- 评价列表 -->
    <div class="review-list" v-loading="loading">
      <div
        v-for="item in reviews"
        :key="item.id"
        class="review-card"
      >
        <div class="review-card-header">
          <div class="activity-info" @click="goActivity(item.activityId)">
            <el-icon><Ticket /></el-icon>
            <span class="activity-title">{{ item.activityTitle || '未知活动' }}</span>
            <el-icon class="link-icon"><ArrowRight /></el-icon>
          </div>
          <el-tag :type="getStatusType(item.status)" size="small">
            {{ getStatusText(item.status) }}
          </el-tag>
        </div>

        <div class="review-card-body">
          <div class="rating-row">
            <span class="rating-label">我的评分：</span>
            <el-rate
              :model-value="Number(item.rating || item.score || 0)"
              disabled
              size="small"
            />
            <span class="rating-value">{{ Number(item.rating || item.score || 0).toFixed(1) }} 分</span>
          </div>

          <div class="review-content">{{ item.content || item.comment || '未填写评价内容' }}</div>

          <!-- 评价图片 -->
          <div class="review-images" v-if="parseImages(item.images).length">
            <el-image
              v-for="(img, idx) in parseImages(item.images)"
              :key="idx"
              :src="img"
              :preview-src-list="parseImages(item.images)"
              :initial-index="idx"
              class="review-image"
              fit="cover"
              preview-teleported
              lazy
            >
              <template #error>
                <div class="review-image review-image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>

          <!-- 商家回复 -->
          <div class="review-reply" v-if="item.reply || item.replyContent">
            <div class="reply-tag">商家回复：</div>
            <div class="reply-content">{{ item.reply || item.replyContent }}</div>
          </div>

          <div class="review-footer">
            <span class="review-time">{{ formatTime(item.createTime) }}</span>
            <el-button
              type="danger"
              text
              size="small"
              @click="handleDelete(item)"
            >删除评价</el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="reviews.length === 0 && !loading" description="暂无评价记录">
        <el-button type="primary" @click="showAddDialog">写第一条评价</el-button>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 写评价对话框 -->
    <el-dialog
      v-model="addDialogVisible"
      title="发布评价"
      width="560px"
      destroy-on-close
    >
      <el-form :model="addForm" label-width="80px" v-loading="pendingLoading">
        <el-form-item label="选择活动" required>
          <el-select
            v-model="addForm.activityId"
            placeholder="请选择要评价的活动"
            style="width: 100%"
            :disabled="!pendingList.length"
          >
            <el-option
              v-for="item in pendingList"
              :key="item.activityId"
              :label="item.activityTitle"
              :value="item.activityId"
            />
          </el-select>
          <div v-if="!pendingList.length && !pendingLoading" class="no-pending-tip">
            暂无可评价的活动（需先购票才能评价）
          </div>
        </el-form-item>

        <el-form-item label="评分" required>
          <el-rate v-model="addForm.score" :texts="['很差', '一般', '还行', '不错', '很好']" />
        </el-form-item>

        <el-form-item label="评价内容" required>
          <el-input
            v-model="addForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请分享您的观演体验..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">发布评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyReviews, deleteMyReview, getPendingReviews, addReview } from '@/api/review'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const reviews = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const addDialogVisible = ref(false)
const pendingLoading = ref(false)
const pendingList = ref([])
const submitLoading = ref(false)
const addForm = ref({
  activityId: null,
  score: 5,
  content: ''
})

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] ?? 'info'
}

const getStatusText = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已隐藏', 3: '已驳回' }
  return map[status] ?? '未知'
}

const parseImages = (images) => {
  if (!images) return []
  try {
    if (typeof images === 'string') {
      const parsed = JSON.parse(images)
      return Array.isArray(parsed) ? parsed : [images]
    }
    return Array.isArray(images) ? images : []
  } catch {
    return images.split(',').filter(Boolean)
  }
}

const goActivity = (activityId) => {
  if (activityId) {
    router.push(`/activity/${activityId}`)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyReviews({ page: page.value, size: size.value })
    const data = res.data || res
    reviews.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定删除对"${item.activityTitle || '该活动'}"的评价吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteMyReview(item.id)
    ElMessage.success('评价已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const showAddDialog = async () => {
  addForm.value = { activityId: null, score: 5, content: '' }
  addDialogVisible.value = true
  await fetchPending()
}

const fetchPending = async () => {
  pendingLoading.value = true
  try {
    const res = await getPendingReviews()
    pendingList.value = res.data || []
  } catch {
    pendingList.value = []
  } finally {
    pendingLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!addForm.value.activityId) {
    ElMessage.warning('请选择要评价的活动')
    return
  }
  if (!addForm.value.score || addForm.value.score < 1) {
    ElMessage.warning('请选择评分')
    return
  }
  if (!addForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  submitLoading.value = true
  try {
    await addReview({
      activityId: addForm.value.activityId,
      rating: addForm.value.score,
      content: addForm.value.content.trim()
    })
    ElMessage.success('评价发布成功，审核通过后将公开展示')
    addDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('评价发布失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-reviews-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 22px;
  color: #303133;
  margin-bottom: 6px;
}

.page-desc {
  font-size: 14px;
  color: #909399;
}

/* 评价卡片 */
.review-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s ease;
}

.review-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.review-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.activity-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #409eff;
  font-size: 15px;
  font-weight: 600;
}

.activity-info:hover {
  opacity: 0.8;
}

.link-icon {
  font-size: 12px;
}

.review-card-body {
  padding-top: 4px;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.rating-label {
  font-size: 14px;
  color: #606266;
}

.rating-value {
  font-size: 14px;
  color: #f56c6c;
  font-weight: 600;
}

.review-content {
  font-size: 15px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #f9fafc;
  border-radius: 8px;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.review-image {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  cursor: pointer;
}

/* 评价图片加载失败的兜底 */
.review-image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 26px;
}

.review-reply {
  background: #fffbe6;
  border: 1px solid #f5dab1;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
}

.reply-tag {
  font-size: 13px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 4px;
}

.reply-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-time {
  font-size: 13px;
  color: #909399;
}

.no-pending-tip {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>

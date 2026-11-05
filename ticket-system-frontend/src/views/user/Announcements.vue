<template>
  <div class="announcements-page">
    <div class="page-header">
      <h2>系统公告</h2>
      <p class="page-desc">查看平台最新公告与通知</p>
    </div>

    <!-- 置顶公告 -->
    <div v-if="topAnnouncements.length > 0" class="top-section">
      <div
        v-for="item in topAnnouncements"
        :key="item.id"
        class="top-card"
        @click="showDetail(item)"
      >
        <div class="top-badge">
          <el-icon><Top /></el-icon>
          置顶
        </div>
        <h3 class="top-title">{{ item.title }}</h3>
        <p class="top-content">{{ item.content }}</p>
        <div class="top-time">{{ formatTime(item.createTime) }}</div>
      </div>
    </div>

    <!-- 公告列表 -->
    <div class="list-section" v-loading="loading">
      <div
        v-for="item in announcements"
        :key="item.id"
        class="announcement-card"
        @click="showDetail(item)"
      >
        <div class="card-left">
          <el-icon class="card-icon"><Bell /></el-icon>
        </div>
        <div class="card-body">
          <div class="card-title-row">
            <span class="card-title">{{ item.title }}</span>
            <el-tag v-if="item.isTop === 1" type="danger" size="small">置顶</el-tag>
            <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small">
              {{ item.status === 1 ? '已发布' : '已下架' }}
            </el-tag>
          </div>
          <p class="card-content">{{ item.content }}</p>
          <div class="card-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
      <el-empty v-if="announcements.length === 0 && !loading" description="暂无公告" />
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

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentDetail?.title || '公告详情'" width="600px">
      <div class="detail-content" v-if="currentDetail">
        <div class="detail-meta">
          <el-tag v-if="currentDetail.isTop === 1" type="danger" size="small">置顶</el-tag>
          <span class="detail-time">发布时间：{{ formatTime(currentDetail.createTime) }}</span>
        </div>
        <div class="detail-text">{{ currentDetail.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAnnouncements } from '@/api/announcement'

const loading = ref(false)
const announcements = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentDetail = ref(null)

const topAnnouncements = computed(() => {
  return announcements.value.filter(item => item.isTop === 1)
})

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const showDetail = (item) => {
  currentDetail.value = item
  detailVisible.value = true
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAnnouncements({ page: page.value, size: size.value })
    const data = res.data || res
    announcements.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.announcements-page {
  max-width: 900px;
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

/* 置顶公告 */
.top-section {
  margin-bottom: 24px;
}

.top-card {
  background: linear-gradient(135deg, #fff9f0 0%, #fff3e6 100%);
  border: 1px solid #f5dab1;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.top-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(230, 162, 60, 0.15);
}

.top-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.top-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.top-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-time {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}

/* 公告列表 */
.list-section {
  min-height: 200px;
}

.announcement-card {
  display: flex;
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.announcement-card:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.card-left {
  flex-shrink: 0;
  margin-right: 16px;
}

.card-icon {
  font-size: 24px;
  color: #409eff;
  margin-top: 2px;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-time {
  font-size: 13px;
  color: #909399;
}

/* 详情弹窗 */
.detail-content {
  padding: 0 4px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-time {
  font-size: 13px;
  color: #909399;
}

.detail-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>

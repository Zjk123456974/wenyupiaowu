<template>
  <div class="activity-manage">
    <!-- 头部标题 + 新增 -->
    <div class="page-head">
      <div class="page-head-left">
        <h3 class="page-title">活动管理</h3>
        <span class="page-desc">发布、编辑并跟进您的每一场演出</span>
      </div>
      <router-link to="/operator/activity/add">
        <el-button type="primary" :icon="Plus" class="add-btn">发布新活动</el-button>
      </router-link>
    </div>

    <!-- 状态总览小卡 -->
    <div class="stat-strip">
      <div class="stat-chip" :class="{ active: activeStatus === null }" @click="changeStatus(null)">
        <span class="chip-value">{{ countByStatus(null) }}</span>
        <span class="chip-label">全部活动</span>
      </div>
      <div class="stat-chip" :class="{ active: activeStatus === -1 }" @click="changeStatus(-1)">
        <span class="chip-value">{{ countByStatus(-1) }}</span>
        <span class="chip-label">草稿</span>
      </div>
      <div class="stat-chip" :class="{ active: activeStatus === 0 }" @click="changeStatus(0)">
        <span class="chip-value">{{ countByStatus(0) }}</span>
        <span class="chip-label">待审核</span>
      </div>
      <div class="stat-chip" :class="{ active: activeStatus === 1 }" @click="changeStatus(1)">
        <span class="chip-value">{{ countByStatus(1) }}</span>
        <span class="chip-label">已通过</span>
      </div>
      <div class="stat-chip" :class="{ active: activeStatus === 3 }" @click="changeStatus(3)">
        <span class="chip-value">{{ countByStatus(3) }}</span>
        <span class="chip-label">已上架</span>
      </div>
      <div class="stat-chip" :class="{ active: activeStatus === 2 }" @click="changeStatus(2)">
        <span class="chip-value">{{ countByStatus(2) }}</span>
        <span class="chip-label">已驳回</span>
      </div>
      <div class="stat-chip" :class="{ active: activeStatus === 4 }" @click="changeStatus(4)">
        <span class="chip-value">{{ countByStatus(4) }}</span>
        <span class="chip-label">已下架</span>
      </div>
    </div>

    <!-- 筛选工具栏 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="按活动名称快速检索（当前页内）"
        clearable
        class="keyword-input"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
        @clear="handleReset"
      />
      <el-button type="primary" plain @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 列表 -->
    <div class="table-card">
      <!-- 首屏骨架屏：列数与真实表格匹配，避免加载完成后的布局跳动 -->
      <div v-if="loading && tableData.length === 0" class="table-skeleton">
        <el-skeleton :count="8" animated>
          <template #template>
            <div class="skeleton-row">
              <el-skeleton-item variant="text" style="width: 65%" />
              <el-skeleton-item variant="text" style="width: 35%" />
              <el-skeleton-item variant="text" style="width: 40%" />
              <el-skeleton-item variant="text" style="width: 30%" />
            </div>
          </template>
        </el-skeleton>
      </div>

      <div v-else class="table-scroll">
        <el-table :data="tableData" style="width: 100%" v-loading="loading">
          <el-table-column label="活动" min-width="280">
            <template #default="{ row }">
              <div class="act-cell" @click="goPreview(row)">
                <el-image
                  v-if="row.coverImage"
                  :src="row.coverImage"
                  fit="cover"
                  class="act-cover"
                  :preview-src-list="[row.coverImage]"
                  preview-teleported
                  lazy
                  >
                  <template #error>
                    <div class="act-cover act-cover-empty">
                      <el-icon :size="20"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div v-else class="act-cover act-cover-empty">
                  <el-icon :size="20"><Picture /></el-icon>
                </div>
                <div class="act-info">
                  <div class="act-title ellipsis">{{ row.title }}</div>
                  <div class="act-meta">ID: {{ row.id }} · {{ row.categoryName || '未分类' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="venue" label="场地" min-width="140" show-overflow-tooltip />
          <el-table-column label="票价" width="110">
            <template #default="{ row }">
              <span class="price-text">¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="售票进度" width="190">
            <template #default="{ row }">
              <div class="progress-wrap">
                <el-progress
                  :percentage="salePercent(row)"
                  :stroke-width="7"
                  :color="progressColor(row)"
                  :format="() => `${row.soldTickets}/${row.totalTickets}`"
                  />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="抢票时间" min-width="155">
            <template #default="{ row }">
              <div class="time-cell">
                <span>{{ formatDate(row.grabStartTime) }}</span>
                <span class="time-to">至</span>
                <span>{{ formatDate(row.grabEndTime) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light" round>
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === -1 || row.status === 0 || row.status === 2"
                type="primary"
                text
                size="small"
                @click="router.push(`/operator/activity/edit/${row.id}`)"
              >编辑</el-button>
              <el-button
                v-if="row.status === -1"
                type="success"
                text
                size="small"
                @click="handleApply(row)"
              >提交审核</el-button>
              <el-button
                v-if="row.status === 3"
                link
                type="success"
                size="small"
                @click="goPreview(row)"
              >查看前台</el-button>
              <el-button
                v-if="row.status === 2 || row.status === 4"
                type="warning"
                text
                size="small"
                @click="handleApply(row)"
              >{{ row.status === 4 ? '重新上架' : '再次提交' }}</el-button>
              <el-button
                v-if="row.status === 3"
                type="danger"
                text
                size="small"
                @click="handleOffline(row)"
              >下架</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无相关活动，快去发布一场吧" />
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePagination"
        @current-change="handlePagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyActivities, applyActivity, offlineActivity } from '@/api/operator'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Picture } from '@element-plus/icons-vue'

const router = useRouter()

const activities = ref([])
const allStats = ref({})
const page = ref(1)
const size = ref(10)
const total = ref(0)
const activeStatus = ref(null)
const keyword = ref('')
const loading = ref(false)

const statusMap = {
  '-1': '草稿',
  0: '待审核',
  1: '已通过',
  2: '已驳回',
  3: '已上架',
  4: '已下架'
}
const statusTagTypeMap = {
  '-1': 'info',
  0: 'warning',
  1: '',
  2: 'danger',
  3: 'success',
  4: 'info'
}

const statusLabel = (status) => statusMap[status] || '未知'
const statusTagType = (status) => statusTagTypeMap[status] || 'info'

const tableData = computed(() => {
  if (!keyword.value.trim()) return activities.value
  const kw = keyword.value.trim().toLowerCase()
  return activities.value.filter((a) => (a.title || '').toLowerCase().includes(kw))
})

const salePercent = (row) => {
  if (!row.totalTickets) return 0
  return Math.min(100, Math.round(((row.soldTickets || 0) / row.totalTickets) * 100))
}
const progressColor = (row) => {
  const p = salePercent(row)
  if (p >= 90) return '#ff4d6d'
  if (p >= 60) return '#ff7849'
  return '#409eff'
}

const formatDate = (t) => (t ? String(t).slice(0, 16) : '-')

const countByStatus = (status) => {
  if (status === null) return allStats.value.total || 0
  return allStats.value[status] || 0
}

const loadStats = async () => {
  try {
    const res = await getMyActivities({ page: 1, size: 1 })
    const data = res.data || res
    const records = data.records || data.list || []
    allStats.value = {
      total: data.total || records.length
    }
    // 逐状态统计（仅用于头部小卡）
    const keys = [-1, 0, 1, 2, 3, 4]
    const pages = await Promise.all(
      keys.map((s) => getMyActivities({ page: 1, size: 1, status: s }).catch(() => null))
    )
    pages.forEach((res2, i) => {
      const d = res2 && (res2.data || res2)
      allStats.value[keys[i]] = d ? d.total || 0 : 0
    })
  } catch {
    // ignore
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (activeStatus.value !== null) params.status = activeStatus.value
    const res = await getMyActivities(params)
    const data = res.data || res
    activities.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const changeStatus = (status) => {
  activeStatus.value = status
  page.value = 1
  loadData()
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  keyword.value = ''
  activeStatus.value = null
  page.value = 1
  loadData()
}

const handlePagination = () => {
  loadData()
}

const handleApply = async (row) => {
  try {
    const tip = row.status === 4
      ? '该活动已下架，重新上架需再次提交管理员审核，确定提交？'
      : '确定重新提交审核该活动？'
    await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
    await applyActivity(row.id)
    ElMessage.success('已提交审核')
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm('下架后用户将无法继续购票，确定下架该活动？', '下架提醒', { type: 'warning' })
    await offlineActivity(row.id)
    ElMessage.success('已下架')
    loadData()
    loadStats()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const goPreview = (row) => {
  if (row.status === 3) {
    window.open(`/activity/${row.id}`, '_blank')
  } else {
    router.push(`/operator/activity/edit/${row.id}`)
  }
}

onMounted(() => {
  loadData()
  loadStats()
})
</script>

<style scoped>
.activity-manage {
  min-height: calc(100vh - 110px);
}

/* 页头 */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  color: #1b2a4a;
  margin-bottom: 4px;
}

.page-desc {
  font-size: 12px;
  color: #909399;
}

.add-btn {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border: none;
  box-shadow: 0 6px 14px rgba(255, 77, 109, 0.3);
}

/* 状态小卡 */
.stat-strip {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-chip {
  flex: 1;
  min-width: 130px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #f0f2f5;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(31, 92, 170, 0.04);
}

.stat-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(31, 92, 170, 0.1);
}

.stat-chip.active {
  border-color: #409eff;
  background: linear-gradient(135deg, #ecf5ff, #f4f9ff);
}

.chip-value {
  font-size: 24px;
  font-weight: 800;
  color: #1b2a4a;
}

.chip-label {
  font-size: 13px;
  color: #909399;
}

.stat-chip.active .chip-value {
  color: #409eff;
}

/* 工具栏 */
.toolbar {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(31, 92, 170, 0.04);
}

.keyword-input {
  width: 320px;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 6px 16px;
  box-shadow: 0 2px 8px rgba(31, 92, 170, 0.04);
}

/* 表格单元格 */
.act-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.act-cover {
  width: 72px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f0f2f5;
}

.act-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.act-info {
  min-width: 0;
}

.act-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.act-meta {
  font-size: 12px;
  color: #c0c4cc;
}

.price-text {
  color: #ff4d6d;
  font-weight: 700;
  font-size: 15px;
}

.time-cell {
  display: flex;
  flex-direction: column;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.time-to {
  color: #c0c4cc;
}

.progress-wrap :deep(.el-progress__text) {
  font-size: 12px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 骨架屏：行高与表格行高接近，加载完成时不产生明显跳动 */
.table-skeleton {
  padding: 12px 4px;
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
  .activity-manage {
    min-height: 0;
    max-width: 100%;
    box-sizing: border-box;
  }

  /* 页头：标题与按钮换行 */
  .page-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .page-head :deep(.el-button) {
    width: 100%;
  }

  /* 状态小卡：两列排列 */
  .stat-chip {
    min-width: 0;
    flex: 0 0 calc(50% - 6px);
    padding: 10px 12px;
  }

  .chip-value {
    font-size: 20px;
  }

  /* 工具栏：纵向堆叠 */
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .keyword-input {
    width: 100%;
  }

  .toolbar :deep(.el-button) {
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
    min-width: 880px;
  }

  .pagination-wrap {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>

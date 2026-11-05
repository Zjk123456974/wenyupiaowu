<template>
  <div class="activity-list-page">
    <div class="category-bar">
      <el-tabs v-model="queryParams.categoryId" @tab-change="handleCategoryChange">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane
          v-for="cat in categories"
          :key="cat.id"
          :label="cat.name"
          :name="String(cat.id)"
        />
      </el-tabs>
    </div>

    <!-- 抢票状态快捷筛选（对标"我想买可以抢的活动"） -->
    <div class="status-bar">
      <span class="status-label">抢票状态</span>
      <span
        v-for="opt in statusOptions"
        :key="opt.value"
        class="status-pill"
        :class="{ active: queryParams.grabStatus === opt.value }"
        @click="handleStatusChange(opt.value)"
      >{{ opt.label }}</span>
    </div>

    <!-- 在线选座快捷筛选（选座场次专区） -->
    <div class="status-bar">
      <span class="status-label">选座</span>
      <span
        class="status-pill"
        :class="{ active: queryParams.onlySeat }"
        @click="toggleSeatFilter"
      >🎫 仅看支持选座</span>
    </div>

    <!-- 搜索筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <span class="filter-group-label">筛选</span>
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索活动名称"
          clearable
          class="filter-keyword"
          @keyup.enter="fetchList"
        />
        <el-date-picker
          v-model="queryParams.date"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          clearable
          class="filter-date"
        />
        <div class="filter-price">
          <el-input v-model="queryParams.priceMin" placeholder="最低价" class="price-input" />
          <span class="price-sep">-</span>
          <el-input v-model="queryParams.priceMax" placeholder="最高价" class="price-input" />
        </div>
        <el-button type="primary" :icon="Search" @click="fetchList">搜索</el-button>
      </div>
      <div class="filter-divider"></div>
      <div class="filter-group">
        <span class="filter-group-label">排序</span>
        <div class="sort-pills">
          <span
            v-for="opt in sortOptions"
            :key="opt.value"
            class="sort-pill"
            :class="{ active: queryParams.sort === opt.value }"
            @click="handleSortChange(opt.value)"
          >{{ opt.label }}</span>
        </div>
      </div>
    </div>

    <!-- 标签筛选栏 -->
    <div class="tag-bar" v-if="tags.length > 0">
      <span class="tag-label">标签筛选：</span>
      <div class="tag-list">
        <el-tag
          v-for="tag in tags"
          :key="tag.id"
          :type="selectedTagId === tag.id ? '' : 'info'"
          :effect="selectedTagId === tag.id ? 'dark' : 'plain'"
          class="filter-tag"
          @click="handleTagClick(tag.id)"
        >
          {{ tag.name }}
        </el-tag>
        <el-tag
          v-if="selectedTagId"
          type="danger"
          effect="plain"
          class="filter-tag clear-tag"
          @click="handleTagClick(null)"
        >
          清除筛选
        </el-tag>
      </div>
    </div>

    <!-- 活动卡片网格 -->
    <div class="activity-grid">
      <!-- 首屏骨架屏：卡片数量与一页的实际条数匹配，避免加载完成后的布局跳动 -->
      <el-row v-if="loading && activityList.length === 0" :gutter="20">
        <el-col v-for="i in 8" :key="'sk-' + i" :xs="24" :sm="12" :md="8" :lg="6">
          <div class="activity-card skeleton-card">
            <el-skeleton :rows="4" animated />
          </div>
        </el-col>
      </el-row>

      <el-row v-else :gutter="20" v-loading="loading">
        <el-col
          v-for="item in activityList"
          :key="item.id"
          :xs="24" :sm="12" :md="8" :lg="6"
        >
          <div class="activity-card" @click="goDetail(item.id)">
            <div class="card-image-wrapper">
              <el-image class="card-cover" :src="item.coverImage" fit="cover" lazy>
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="card-category" v-if="item.categoryName">{{ item.categoryName }}</div>
              <span v-if="hasSeat(item)" class="card-seat-badge">🎫 在线选座</span>
              <!-- 抢票状态醒目角标（对标大麦“抢票中”） -->
              <span
                v-if="getItemStatus(item) === 2 || getItemStatus(item) === 1"
                class="card-status-badge"
                :class="getItemStatus(item) === 2 ? 'st-selling' : 'st-upcoming'"
              >{{ getItemStatus(item) === 2 ? '抢票中' : '即将开售' }}</span>
              <!-- 想看快捷按钮 -->
              <button
                class="card-wish-btn"
                :class="{ active: wishedMap[item.id] }"
                :title="wishedMap[item.id] ? '已想看' : '想看'"
                @click.stop="toggleWishOnCard(item)"
              >
                <el-icon><Star /></el-icon>
              </button>
            </div>
            <div class="card-info">
              <h3 class="card-title">{{ item.title || item.name }}</h3>
              <p class="card-location"><el-icon><Location /></el-icon>{{ item.venue || item.location }}</p>
              <p class="card-time"><el-icon><Clock /></el-icon>{{ formatTime(item.startTime) }} ~ {{ formatTime(item.endTime) }}</p>
              <p class="card-desc" v-if="item.description">{{ item.description.substring(0, 50) }}{{ item.description.length > 50 ? '...' : '' }}</p>
              <!-- 已售进度：仅开售后展示（未开售显示销量会造成"没开抢就有人买"的矛盾观感） -->
              <div class="card-progress" v-if="getSoldPercent(item) !== null && getItemStatus(item) !== 1">
                <div class="progress-track">
                  <div
                    class="progress-fill"
                    :class="{ hot: getSoldPercent(item) > 80 }"
                    :style="{ width: getSoldWidth(item) + '%' }"
                  ></div>
                </div>
                <span class="progress-text">已售 {{ item.soldTickets }} 张 · {{ getSoldPercent(item) }}%</span>
                <el-tag
                  v-if="getSoldPercent(item) > 80"
                  type="danger"
                  size="small"
                  effect="dark"
                  class="hot-selling-tag"
                >热销中</el-tag>
              </div>
              <div class="card-bottom">
                <div class="card-price-wrapper">
                  <span class="card-price-unit">¥</span>
                  <span class="card-price">{{ item.price ? parseFloat(item.price).toFixed(2) : '0.00' }}</span>
                  <span class="card-price-suffix">起</span>
                </div>
                <span class="card-remain">剩余 {{ item.remainTickets || (item.totalTickets - item.soldTickets) || 0 }} 张</span>
                <el-tag v-if="item.wishCount > 0" type="warning" size="small" class="card-reserve-tag">
                  ❤ {{ item.wishCount }}人想看
                </el-tag>
                <el-tag :type="getStatusType(item)" size="small" class="card-status">{{ getStatusText(item) }}</el-tag>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="activityList.length === 0 && !loading" description="暂无活动" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[8, 12, 16, 24]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Star } from '@element-plus/icons-vue'
import { getActivityList, toggleWish } from '@/api/activity'
import { getCategoryList } from '@/api/category'
import { getEnabledTags } from '@/api/tag'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
// 卡片“想看”状态（当前用户是否已想看，按活动 ID 记忆，初始未预取）
const wishedMap = ref({})
const wishLoadingMap = ref({})

const loading = ref(false)
const activityList = ref([])
const total = ref(0)
const categories = ref([])
const tags = ref([])
const selectedTagId = ref(null)

const queryParams = reactive({
  keyword: '',
  categoryId: '',
  date: '',
  priceMin: '',
  priceMax: '',
  sort: 'latest',
  grabStatus: 'all',
  onlySeat: false,
  page: 1,
  pageSize: 12
})

// 排序 pill 选项（值与原 el-select 保持一致，数据逻辑不变）
const sortOptions = [
  { label: '热门', value: 'hot' },
  { label: '最新', value: 'latest' },
  { label: '价格 低→高', value: 'priceAsc' },
  { label: '价格 高→低', value: 'priceDesc' },
  { label: '即将开售', value: 'upcoming' }
]

// 抢票状态快捷筛选（客户端过滤，数据集小）：解决"想买可以抢的活动却没有该分类"
const statusOptions = [
  { value: 'all', label: '全部' },
  { value: 'selling', label: '抢票中' },
  { value: 'upcoming', label: '即将开售' }
]
const statusNum = { selling: 2, upcoming: 1 }
const handleStatusChange = (value) => {
  queryParams.grabStatus = value
  queryParams.page = 1
  fetchList()
}
// 在线选座快捷筛选（选座场次专区）：仅展示支持选座的活动
const toggleSeatFilter = () => {
  queryParams.onlySeat = !queryParams.onlySeat
  queryParams.page = 1
  fetchList()
}

const handleSortChange = (value) => {
  queryParams.sort = value
  queryParams.page = 1
  fetchList()
}

// 已售进度百分比（soldTickets / totalTickets），无数据时返回 null
const getSoldPercent = (item) => {
  const total = Number(item?.totalTickets)
  const sold = Number(item?.soldTickets)
  if (!total || isNaN(total) || isNaN(sold) || sold < 0) return null
  const p = (sold / total) * 100
  // 小于 10% 保留 1 位小数，避免被四舍五入成 0%
  const rounded = p < 10 ? Math.round(p * 10) / 10 : Math.round(p)
  return Math.min(100, Math.max(0, rounded))
}

// 进度条填充宽度：非零销量至少给 2% 可见宽度，避免看起来完全没人买
const getSoldWidth = (item) => {
  const p = getSoldPercent(item)
  if (p === null) return 0
  const sold = Number(item?.soldTickets)
  if (sold > 0 && p < 2) return 2
  return p
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getStatusType = (item) => {
  const status = getItemStatus(item)
  const map = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'info' }
  return map[status] ?? 'info'
}

const getStatusText = (item) => {
  const status = getItemStatus(item)
  const map = { 0: '未开始', 1: '即将开售', 2: '抢票中', 3: '已结束' }
  return map[status] ?? '未知'
}

// 是否开通在线选座
const hasSeat = (item) => Number(item?.seatMapEnabled ?? 0) === 1

const getItemStatus = (item) => {
  if (!item) return 3
  const now = new Date().getTime()
  const grabStart = new Date(item.grabStartTime).getTime()
  const grabEnd = new Date(item.grabEndTime).getTime()
  
  if (isNaN(grabStart) || isNaN(grabEnd)) {
    return item.status || 3
  }
  
  if (now < grabStart) return 1
  if (now >= grabStart && now <= grabEnd) return 2
  return 3
}

const goDetail = (id) => {
  router.push(`/activity/${id}`)
}

// 卡片“想看”快捷切换（对标大麦“X人想看”）
const toggleWishOnCard = async (item) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (wishLoadingMap.value[item.id]) return
  wishLoadingMap.value[item.id] = true
  try {
    const res = await toggleWish(item.id)
    const data = res.data || {}
    item.wishCount = data.wishCount != null ? data.wishCount : (item.wishCount || 0)
    wishedMap.value[item.id] = !!data.wished
    ElMessage.success(data.wished ? '已标记为想看' : '已取消想看')
  } catch {
    // 错误由拦截器统一提示
  } finally {
    wishLoadingMap.value[item.id] = false
  }
}

const handleCategoryChange = () => {
  queryParams.page = 1
  fetchList()
}

const handleTagClick = (tagId) => {
  selectedTagId.value = tagId
  queryParams.page = 1
  fetchList()
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch {
    // ignore
  }
}

const fetchTags = async () => {
  try {
    const res = await getEnabledTags()
    tags.value = res.data || []
  } catch {
    // ignore
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const needFilter = queryParams.grabStatus !== 'all' || queryParams.onlySeat
    const params = {
      page: needFilter ? 1 : queryParams.page,
      size: needFilter ? 100 : queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      categoryId: queryParams.categoryId ? parseInt(queryParams.categoryId) : undefined,
      tagId: selectedTagId.value || undefined,
      startTime: queryParams.date ? queryParams.date + ' 00:00:00' : undefined,
      endTime: queryParams.date ? queryParams.date + ' 23:59:59' : undefined,
      minPrice: queryParams.priceMin ? parseFloat(queryParams.priceMin) : undefined,
      maxPrice: queryParams.priceMax ? parseFloat(queryParams.priceMax) : undefined,
      sortField: queryParams.sort === 'latest' ? 'createTime' :
                 queryParams.sort === 'hot' ? 'hot' :
                 queryParams.sort === 'upcoming' ? 'grabStartTime' :
                 queryParams.sort === 'priceAsc' ? 'price' :
                 queryParams.sort === 'priceDesc' ? 'price' : undefined,
      sortOrder: queryParams.sort === 'priceAsc' ? 'asc' : 'desc'
    }
    const res = await getActivityList(params)
    let records = res?.records || res?.data?.records || res?.data?.list || []
    if (needFilter) {
      if (queryParams.grabStatus !== 'all') {
        const want = statusNum[queryParams.grabStatus]
        records = records.filter(it => getItemStatus(it) === want)
      }
      if (queryParams.onlySeat) {
        records = records.filter(it => Number(it.seatMapEnabled ?? 0) === 1)
      }
      total.value = records.length
      const start = (queryParams.page - 1) * queryParams.pageSize
      activityList.value = records.slice(start, start + queryParams.pageSize)
    } else {
      activityList.value = records
      total.value = res?.total || res?.data?.total || 0
    }
  } catch {
    // 错误提示由 axios 拦截器统一弹出，这里只清空列表，避免"加载失败"被看成"没有符合条件的活动"
    activityList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 从 URL 查询参数同步筛选条件并重新加载（供初次进入和路由切换时复用）
const applyRouteQuery = () => {
  const q = route.query
  queryParams.keyword = q.keyword ? String(q.keyword) : ''
  queryParams.categoryId = q.categoryId ? String(q.categoryId) : ''
  selectedTagId.value = q.tagId ? parseInt(q.tagId) : null
  queryParams.page = 1
  fetchList()
}

onMounted(() => {
  fetchCategories()
  fetchTags()
  applyRouteQuery()
})

// 头部下拉切换分类 / 搜索跳转时，URL 参数变化后重新加载列表
watch(
  () => route.query,
  () => {
    applyRouteQuery()
  }
)
</script>

<style scoped>
.activity-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* 分类Tab */
.category-bar {
  background: #fff;
  border-radius: 12px;
  padding: 0 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.category-bar :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.category-bar :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.category-bar :deep(.el-tabs__item) {
  font-size: 15px;
  padding: 0 20px;
}

/* 搜索筛选栏 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 视觉分组：分组间浅灰分隔线 */
.filter-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.filter-group-label {
  font-size: 13px;
  color: #909399;
  font-weight: 600;
  flex-shrink: 0;
  margin-right: 2px;
}

.filter-divider {
  width: 1px;
  align-self: stretch;
  background: #f0f2f5;
}

/* 排序 pill 按钮组 */
.sort-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sort-pill {
  padding: 6px 16px;
  border-radius: 16px;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  user-select: none;
}

.sort-pill:hover {
  color: #409EFF;
  background: #ecf5ff;
}

.sort-pill.active {
  background: #409EFF;
  color: #fff;
  font-weight: 600;
}

.filter-keyword {
  width: 220px;
}

.filter-date {
  width: 170px;
}

.filter-price {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-input {
  width: 90px;
}

.price-sep {
  color: #c0c4cc;
}

/* 标签筛选栏 */
.tag-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

/* 抢票状态快捷筛选 */
.status-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}
.status-label {
  font-size: 14px;
  color: #606266;
  font-weight: 600;
  margin-right: 4px;
}
.status-pill {
  padding: 6px 18px;
  font-size: 13px;
  color: #606266;
  border-radius: 999px;
  cursor: pointer;
  background: #f4f5f7;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  user-select: none;
}
.status-pill:hover {
  color: #ff2e63;
  border-color: #ffd0dc;
}
.status-pill.active {
  color: #fff;
  background: linear-gradient(90deg, #ff4d6d, #ff7849);
  font-weight: 600;
}

/* 在线选座标识（封面右上角） */
.card-seat-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  border-radius: 999px;
  background: linear-gradient(90deg, #7b5cff, #4d8bff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
  z-index: 2;
}

/* 抢票状态醒目角标（封面左下角，对标大麦“抢票中”） */
.card-status-badge {
  position: absolute;
  left: 12px;
  bottom: 12px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  border-radius: 999px;
  z-index: 2;
  letter-spacing: 1px;
}
.card-status-badge.st-selling {
  background: linear-gradient(90deg, #ff4d6d, #ff7849);
  box-shadow: 0 2px 10px rgba(255, 77, 109, 0.45);
  animation: status-pulse 1.4s ease-in-out infinite;
}
.card-status-badge.st-upcoming {
  background: linear-gradient(90deg, #4d8bff, #7b5cff);
  box-shadow: 0 2px 10px rgba(77, 139, 255, 0.4);
}
@keyframes status-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.06); }
}

/* 想看快捷按钮（封面右下角） */
.card-wish-btn {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 3;
  transition: all 0.2s ease;
  backdrop-filter: blur(2px);
}
.card-wish-btn:hover {
  background: rgba(0, 0, 0, 0.6);
  transform: scale(1.1);
}
.card-wish-btn.active {
  color: #ff4d6d;
  background: rgba(255, 77, 109, 0.18);
}
.card-wish-btn .el-icon {
  font-size: 18px;
}

.tag-label {
  font-size: 14px;
  color: #606266;
  font-weight: 600;
  flex-shrink: 0;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tag:hover {
  transform: translateY(-2px);
}

.clear-tag {
  margin-left: 8px;
}

/* 活动卡片网格 */
.activity-grid {
  margin-bottom: 20px;
}

.activity-grid :deep(.el-row) {
  margin-left: -12px;
  margin-right: -12px;
}

.activity-grid :deep(.el-col) {
  padding: 0 12px;
}

/* 活动卡片 */
.activity-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  margin-bottom: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #f0f2f5;
  box-shadow: 0 2px 10px rgba(31, 92, 170, 0.06);
}

/* hover 效果与首页卡片统一 */
.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 14px 30px rgba(27, 42, 74, 0.14);
}

.card-image-wrapper {
  position: relative;
  flex-shrink: 0;
  overflow: hidden;
}

.card-cover {
  width: 100%;
  height: 200px;
  display: block;
  transition: transform 0.45s ease;
}

.activity-card:hover .card-cover {
  transform: scale(1.06);
}

.card-category {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
}

.image-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  color: #c0c4cc;
  font-size: 48px;
}

.card-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1b2a4a;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.activity-card:hover .card-title {
  color: #ff4d6d;
}

.card-location,
.card-time {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-location :deep(.el-icon),
.card-time :deep(.el-icon) {
  font-size: 14px;
}

.card-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 已售进度条 */
.card-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.progress-track {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #f0f2f5;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, #79bbff, #409EFF);
  transition: width 0.3s ease;
}

.progress-fill.hot {
  background: linear-gradient(90deg, #ff7849, #ff4d6d);
}

.progress-text {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.hot-selling-tag {
  flex-shrink: 0;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.card-price-wrapper {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.card-price {
  color: #ff4d6d;
  font-size: 22px;
  font-weight: 800;
}

.card-price-unit {
  font-size: 14px;
  color: #ff4d6d;
}

.card-remain {
  font-size: 13px;
  color: #909399;
}

.card-reserve-tag {
  flex-shrink: 0;
}

.card-status {
  flex-shrink: 0;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

.pagination-wrap :deep(.el-pagination) {
  margin-top: 0;
}

/* 骨架屏卡片：与真实卡片同高，加载完成时不产生明显跳动 */
.skeleton-card {
  padding: 0;
  overflow: hidden;
  cursor: default;
}

.skeleton-card :deep(.el-skeleton) {
  padding: 12px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .activity-list-page {
    max-width: 100%;
    box-sizing: border-box;
    padding: 12px;
  }

  /* 分类 Tab：横向滚动，避免换行过多 */
  .category-bar {
    padding: 0 12px;
    margin-bottom: 12px;
  }

  .category-bar :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  .category-bar :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }

  .category-bar :deep(.el-tabs__item) {
    font-size: 14px;
    padding: 0 12px;
  }

  /* 筛选栏：改为纵向块布局，输入框撑满 */
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    padding: 14px;
    gap: 12px;
    margin-bottom: 14px;
  }

  .filter-group {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .filter-group-label {
    font-weight: 500;
  }

  /* 分隔线在纵向布局下改为横向 */
  .filter-divider {
    width: auto;
    height: 1px;
    align-self: stretch;
  }

  .filter-keyword,
  .filter-date {
    width: 100%;
  }

  .filter-price {
    width: 100%;
  }

  .price-input {
    flex: 1;
    width: auto;
    min-width: 0;
  }

  .filter-group :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }

  /* 排序 pill 横向滚动 */
  .sort-pills {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .sort-pill {
    flex-shrink: 0;
    padding: 5px 12px;
    font-size: 12px;
  }

  /* 标签筛选栏 */
  .tag-bar {
    padding: 12px 14px;
    margin-bottom: 14px;
  }

  /* 卡片网格：两列 */
  .activity-grid :deep(.el-row) {
    margin-left: -6px;
    margin-right: -6px;
  }

  .activity-grid :deep(.el-col) {
    padding: 0 6px;
  }

  .activity-card {
    margin-bottom: 12px;
  }

  .card-cover {
    height: 140px;
  }

  .image-placeholder {
    height: 140px;
    font-size: 34px;
  }

  .card-info {
    padding: 12px 10px;
  }

  .card-title {
    font-size: 14px;
    margin-bottom: 6px;
  }

  .card-location,
  .card-time {
    font-size: 12px;
  }

  .card-price {
    font-size: 18px;
  }

  .card-bottom {
    flex-wrap: wrap;
    gap: 4px;
  }

  .card-remain {
    font-size: 12px;
  }

  .pagination-wrap {
    padding: 16px 0;
    overflow-x: auto;
  }
}
</style>

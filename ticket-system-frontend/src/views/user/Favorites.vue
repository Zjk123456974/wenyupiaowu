<template>
  <el-card shadow="never" class="favorites-card">
    <template #header>
      <span class="card-title">我的收藏</span>
    </template>

    <div v-loading="loading" class="favorite-body">
    <div v-if="favorites.length" class="favorite-grid">
      <el-row :gutter="16">
        <el-col
          v-for="item in favorites"
          :key="item.id"
          :span="8"
        >
          <el-card
            shadow="hover"
            class="favorite-item"
            @click="goToDetail(item.activityId)"
          >
            <el-image
              class="favorite-cover"
              :src="item.coverImage || defaultCover"
              fit="cover"
              lazy
            >
              <template #error>
                <div class="favorite-cover favorite-cover-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div class="favorite-info">
              <div class="favorite-title-row">
                <span class="favorite-title">{{ item.activityTitle }}</span>
                <span class="status-badge" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
              </div>
              <div class="favorite-meta">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(item.activityStartTime) }}</span>
              </div>
              <div class="favorite-meta">
                <el-icon><Location /></el-icon>
                <span>{{ item.venue || '地点待定' }}</span>
              </div>
              <div class="favorite-meta wish-meta">
                <el-icon><Star /></el-icon>
                <span>{{ item.wishCount || 0 }} 人想看</span>
              </div>
              <div class="sold-wrap">
                <div class="sold-bar">
                  <div
                    class="sold-fill"
                    :class="{ hot: Number(soldPercent(item)) > 80 }"
                    :style="{ width: soldWidth(item) }"
                  ></div>
                </div>
                <span class="sold-text">已售 {{ soldPercent(item) }}% · 剩 {{ remain(item) }} 张</span>
              </div>
              <div class="favorite-bottom">
                <span class="favorite-price">¥ {{ item.price }}</span>
                <el-button
                  type="danger"
                  text
                  size="small"
                  @click.stop="handleRemove(item.activityId)"
                >取消收藏</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 首次加载中不显示空状态，避免"暂无收藏"闪一下又变成列表 -->
    <el-empty v-else-if="!loading" description="暂无收藏" />

    <div v-if="total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchFavorites"
      />
    </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyFavorites, removeFavorite } from '@/api/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { PLACEHOLDER_IMAGE } from '@/utils/placeholder'

const router = useRouter()

// 无封面时的占位图（内联 SVG，全站共享常量）
const defaultCover = PLACEHOLDER_IMAGE

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

// 已售百分比（文本展示）：<10% 保留 1 位小数，否则取整
const soldPercent = (item) => {
  const total = Number(item?.totalTickets) || 0
  const sold = Number(item?.soldTickets) || 0
  if (!total) return 0
  const pct = (sold / total) * 100
  if (pct > 0 && pct < 10) return pct.toFixed(1)
  return Math.min(100, Math.round(pct))
}

// 剩余票数
const remain = (item) => {
  const total = Number(item?.totalTickets) || 0
  const sold = Number(item?.soldTickets) || 0
  return Math.max(0, total - sold)
}

// 进度条宽度：非零销量至少 2% 可见，避免“看不见已售”
const soldWidth = (item) => {
  const total = Number(item?.totalTickets) || 0
  const sold = Number(item?.soldTickets) || 0
  if (!total || sold <= 0) return '0%'
  const raw = (sold / total) * 100
  return Math.max(raw, 2) + '%'
}

// 状态文案与样式
const STATUS_TEXT = {
  '-1': '草稿',
  '0': '即将开售',
  '2': '未通过',
  '3': '抢票中',
  '4': '已下架'
}
const statusText = (s) => STATUS_TEXT[String(s)] || '在售'
const statusClass = (s) => {
  if (s === 3) return 'on-sale'
  if (s === 0) return 'coming'
  return 'off-shelf'
}

const favorites = ref([])
const page = ref(1)
const pageSize = ref(9)
const total = ref(0)
const loading = ref(false)

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await getMyFavorites({
      page: page.value,
      size: pageSize.value
    })
    const records = res.data.records || res.data.list || []
    favorites.value = records
    total.value = res.data.total || 0
    // 当前页被取消收藏删空后自动回退一页，避免停留在空白页
    if (!records.length && total.value > 0 && page.value > 1) {
      page.value -= 1
      return fetchFavorites()
    }
  } catch {
    // 错误提示由 axios 拦截器统一弹出，这里只重置数据状态，避免把"加载失败"显示成"暂无收藏"
    favorites.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleRemove = async (activityId) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户点了取消
  }
  try {
    await removeFavorite(activityId)
    ElMessage.success('已取消收藏')
    fetchFavorites()
  } catch {
    // 失败提示由拦截器统一处理
  }
}

const goToDetail = (activityId) => {
  router.push({ name: 'ActivityDetail', params: { id: activityId } })
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-card {
  min-height: 400px;
}

/* 加载遮罩需要一定高度才有意义 */
.favorite-body {
  min-height: 220px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.favorite-grid {
  margin-bottom: 16px;
}

.favorite-item {
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 16px;
  transition: transform 0.2s;
}

.favorite-item:hover {
  transform: translateY(-2px);
}

.favorite-item :deep(.el-card__body) {
  padding: 0;
}

.favorite-cover {
  width: 100%;
  height: 160px;
  border-radius: 8px 8px 0 0;
}

.favorite-info {
  padding: 12px;
}

.favorite-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favorite-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.favorite-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.favorite-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.favorite-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
  font-weight: 600;
}
.status-badge.on-sale {
  background: #fef0f0;
  color: #f56c6c;
}
.status-badge.coming {
  background: #fdf6ec;
  color: #e6a23c;
}
.status-badge.off-shelf {
  background: #f4f4f5;
  color: #909399;
}

.wish-meta {
  color: #f56c6c;
}

.sold-wrap {
  margin-top: 6px;
}
.sold-bar {
  height: 6px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
}
.sold-fill {
  height: 100%;
  background: #f56c6c;
  border-radius: 3px;
  transition: width 0.3s;
}
.sold-fill.hot {
  background: #e4393c;
}
.sold-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  display: block;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 收藏封面加载失败的兜底 */
.favorite-cover-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 26px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .favorites-card {
    min-height: 0;
  }

  .favorites-card :deep(.el-card__body) {
    padding: 12px;
  }

  /* 收藏栅格由三列改为两列 */
  .favorite-grid :deep(.el-col) {
    flex: 0 0 50%;
    max-width: 50%;
  }

  .favorite-item {
    margin-bottom: 12px;
  }

  .favorite-cover {
    height: 120px;
  }

  .favorite-info {
    padding: 10px;
  }

  .favorite-title {
    font-size: 13px;
  }

  .favorite-title-row {
    flex-wrap: wrap;
    gap: 4px;
  }

  .status-badge {
    font-size: 10px;
    padding: 1px 5px;
  }

  .sold-text {
    font-size: 11px;
  }

  .favorite-meta {
    font-size: 12px;
  }

  .favorite-price {
    font-size: 15px;
  }

  .pagination-wrapper {
    overflow-x: auto;
  }
}
</style>

<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalUsers || 0 }}</div>
              <div class="stat-label">总用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon :size="28"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalActivities || 0 }}</div>
              <div class="stat-label">总活动数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C">
              <el-icon :size="28"><List /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
              <div class="stat-label">总订单量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #F56C6C">
              <el-icon :size="28"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">¥{{ overview.totalAmount || 0 }}</div>
            <div class="stat-label">总交易额</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card>
          <template #header><span>热门活动排行 Top10</span></template>
          <div class="table-scroll">
            <el-table :data="hotActivities" stripe style="width: 100%">
              <el-table-column type="index" label="序号" width="70" />
              <el-table-column prop="title" label="活动名称" min-width="200" />
              <el-table-column prop="soldTickets" label="购票量" width="100" />
              <el-table-column prop="viewCount" label="浏览量" width="100" />
              <template #empty>
                <el-empty description="暂无数据" :image-size="80" />
              </template>
            </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><span>最近注册用户</span></template>
          <div class="table-scroll">
            <el-table :data="recentUsers" stripe style="width: 100%">
              <el-table-column prop="username" label="账号" min-width="120" />
              <el-table-column prop="nickname" label="昵称" min-width="120" />
              <el-table-column prop="createTime" label="注册时间" min-width="160" />
              <template #empty>
                <el-empty description="暂无数据" :image-size="80" />
              </template>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getOverview, getHotActivitiesStats } from '@/api/admin'
import { User, Tickets, List, Money } from '@element-plus/icons-vue'

const overview = ref({})
const hotActivities = ref([])
const recentUsers = ref([])

const loadData = async () => {
  try {
    const [overviewRes, hotRes] = await Promise.all([
      getOverview(),
      getHotActivitiesStats()
    ])
    overview.value = overviewRes.data || overviewRes || {}
    const hotData = hotRes.data || hotRes || []
    hotActivities.value = Array.isArray(hotData) ? hotData : []
    recentUsers.value = overview.value.recentUsers || []
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 10px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .admin-dashboard {
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

<template>
  <div class="log-manage-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统日志</span>
          <el-button type="success" plain :icon="Download" :loading="exportLoading" @click="handleExport">
            导出Excel
          </el-button>
        </div>
      </template>

      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="操作日志" name="operation" />
        <el-tab-pane label="登录日志" name="login" />
      </el-tabs>

      <!-- 搜索筛选 -->
      <div class="filter-area" style="margin-bottom: 16px">
        <el-input
          v-model="filter.username"
          placeholder="搜索用户名"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <!-- 操作日志表格 / 登录日志表格 -->
      <div class="table-scroll">
        <el-table v-if="activeTab === 'operation'" :data="logs" v-loading="loading" stripe>
          <el-table-column label="操作人" prop="username" width="120" />
          <el-table-column label="模块" prop="module" width="120" />
          <el-table-column label="操作描述" prop="operation" show-overflow-tooltip min-width="200" />
          <el-table-column label="请求URL" prop="requestUrl" show-overflow-tooltip min-width="200" />
          <el-table-column label="IP" prop="ip" width="140" />
          <el-table-column label="设备" prop="device" show-overflow-tooltip min-width="160" />
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">
                {{ row.result === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时(ms)" prop="costTime" width="100" align="right" />
          <el-table-column label="时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>

        <el-table v-else :data="logs" v-loading="loading" stripe>
          <el-table-column label="用户名" prop="username" width="120" />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.loginType === 1 ? 'success' : 'warning'" size="small">
                {{ loginTypeName(row.loginType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="IP" prop="ip" width="140" />
          <el-table-column label="设备" prop="device" show-overflow-tooltip min-width="160" />
          <el-table-column label="浏览器" prop="browser" show-overflow-tooltip min-width="160" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="消息" prop="message" show-overflow-tooltip min-width="180" />
          <el-table-column label="时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
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
          @size-change="fetchLogs"
          @current-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import {
  getOperationLogs, getLoginLogs, exportOperationLogs, exportLoginLogs
} from '@/api/log'

const activeTab = ref('operation')
const logs = ref([])
const total = ref(0)
const loading = ref(false)
const exportLoading = ref(false)

const filter = ref({
  page: 1,
  size: 10,
  username: ''
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      username: filter.value.username || undefined
    }
    const res = activeTab.value === 'operation'
      ? await getOperationLogs(params)
      : await getLoginLogs(params)
    const data = res?.data || res
    logs.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  filter.value.page = 1
  fetchLogs()
}

const handleSearch = () => {
  filter.value.page = 1
  fetchLogs()
}

const resetFilter = () => {
  filter.value.username = ''
  filter.value.page = 1
  fetchLogs()
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      username: filter.value.username || undefined
    }
    const response = activeTab.value === 'operation'
      ? await exportOperationLogs(params)
      : await exportLoginLogs(params)

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const name = activeTab.value === 'operation' ? '操作日志' : '登录日志'
    link.download = `${name}_${new Date().toLocaleDateString()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

const loginTypeName = (type) => {
  const map = { login: '登录', logout: '退出', 1: '登录', 2: '退出' }
  return map[type] || '未知'
}

const formatTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

onMounted(() => {
  fetchLogs()
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
  .log-manage-page {
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

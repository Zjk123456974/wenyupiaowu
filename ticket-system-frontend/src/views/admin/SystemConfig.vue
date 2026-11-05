<template>
  <div class="config-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统基础配置</span>
          <el-button type="primary" plain @click="fetchData">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeGroup" @tab-change="fetchData">
        <el-tab-pane label="系统参数" name="system" />
        <el-tab-pane label="文件上传" name="upload" />
        <el-tab-pane label="定时任务" name="task" />
      </el-tabs>

      <div class="table-scroll">
        <el-table :data="configs" v-loading="loading" stripe>
          <el-table-column label="配置名称" prop="configName" width="200" />
          <el-table-column label="配置键" prop="configKey" width="240">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.configKey }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="配置值" min-width="280">
            <template #default="{ row }">
              <el-input v-if="editingId === row.id" v-model="editValue" size="small"
                :type="row.configKey === 'system.logo' ? 'textarea' : 'text'"
                :rows="2" />
              <span v-else class="config-value">{{ row.configValue || '(空)' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="editingId === row.id">
                <el-button size="small" type="primary" @click="handleSave(row)">保存</el-button>
                <el-button size="small" @click="cancelEdit">取消</el-button>
              </template>
              <el-button v-else size="small" @click="startEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>
    </el-card>

    <!-- 配置说明 -->
    <el-card style="margin-top: 20px">
      <template #header><span>配置说明</span></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="系统参数">
          管理平台基本信息，包括系统名称、网站LOGO、备案信息、客服电话等全局参数，以及订单超时时间、库存预警阈值、抢票限流间隔等业务参数。
        </el-descriptions-item>
        <el-descriptions-item label="文件上传">
          配置图片上传相关参数，包括上传大小限制、允许的文件格式、文件存储路径，统一规范平台素材资源管理。
        </el-descriptions-item>
        <el-descriptions-item label="定时任务">
          配置系统自动化任务的执行间隔，包括订单超时检查、活动状态自动更新等定时任务的调度频率。
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, updateConfig } from '@/api/config'

const configs = ref([])
const loading = ref(false)
const activeGroup = ref('system')
const editingId = ref(null)
const editValue = ref('')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getConfigList({ group: activeGroup.value })
    configs.value = res?.data || res || []
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const startEdit = (row) => {
  editingId.value = row.id
  editValue.value = row.configValue || ''
}

const cancelEdit = () => {
  editingId.value = null
  editValue.value = ''
}

const handleSave = async (row) => {
  try {
    await updateConfig(row.id, { configValue: editValue.value })
    ElMessage.success('配置更新成功')
    editingId.value = null
    fetchData()
  } catch {
    // 错误已由拦截器处理
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-value {
  word-break: break-all;
  color: #303133;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .config-page {
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

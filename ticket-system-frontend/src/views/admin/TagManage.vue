<template>
  <div class="tag-manage-page">
    <!-- 标签统计 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF">
              <el-icon :size="28"><PriceTag /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ tagTotal }}</div>
              <div class="stat-label">标签总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A">
              <el-icon :size="28"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ enabledCount }}</div>
              <div class="stat-label">启用中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C">
              <el-icon :size="28"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ relatedActivityTotal }}</div>
              <div class="stat-label">关联活动总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>标签列表</span>
          <el-button type="primary" @click="showAdd">新增标签</el-button>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="tags" v-loading="loading" stripe>
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="标签名称" min-width="160">
            <template #default="{ row }">
              <el-tag :color="row.color" effect="dark" size="small" style="border: none">
                {{ row.name }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="颜色" width="120" align="center">
            <template #default="{ row }">
              <div class="color-preview">
                <span class="color-block" :style="{ backgroundColor: row.color }"></span>
                <span>{{ row.color }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="排序" prop="sortOrder" width="100" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 || row.status === 'enabled' ? 'success' : 'info'" size="small">
                {{ row.status === 1 || row.status === 'enabled' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联活动数" prop="activityCount" width="120" align="center" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click="showEdit(row)">编辑</el-button>
              <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无数据" :image-size="80" />
          </template>
        </el-table>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑标签' : '新增标签'" width="450px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="form.color" />
          <span style="margin-left: 12px; color: #909399">{{ form.color }}</span>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PriceTag, CircleCheck, Connection } from '@element-plus/icons-vue'
import {
  getTagList, addTag, updateTag, deleteTag, getTagStatistics
} from '@/api/tag'

const tags = ref([])
const statistics = ref([])
const loading = ref(false)
const submitLoading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({
  id: null,
  name: '',
  color: '#409EFF',
  sortOrder: 0,
  status: 1
})

// /tag/statistics 返回数组 [{id,name,color,status,activityCount}]
const tagTotal = computed(() => statistics.value.length)
const enabledCount = computed(() => statistics.value.filter(s => s.status === 1).length)
const relatedActivityTotal = computed(() =>
  statistics.value.reduce((sum, s) => sum + (s.activityCount || 0), 0)
)

const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  color: [{ required: true, message: '请选择颜色', trigger: 'change' }]
}

const fetchTags = async () => {
  loading.value = true
  try {
    const res = await getTagList()
    const data = res?.data || res
    tags.value = data?.records || data || []
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const res = await getTagStatistics()
    const data = res?.data || res || []
    statistics.value = Array.isArray(data) ? data : []
  } catch {
    // 静默处理
  }
}

// 将统计接口的 activityCount 按 id 合并进表格数据
const mergeActivityCounts = () => {
  const countMap = {}
  statistics.value.forEach(s => {
    countMap[s.id] = s.activityCount || 0
  })
  tags.value.forEach(t => {
    t.activityCount = countMap[t.id] != null ? countMap[t.id] : (t.activityCount || 0)
  })
}

const refresh = async () => {
  await Promise.all([fetchTags(), fetchStatistics()])
  mergeActivityCounts()
}

const showAdd = () => {
  isEdit.value = false
  form.value = { id: null, name: '', color: '#409EFF', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const showEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    name: row.name,
    color: row.color || '#409EFF',
    sortOrder: row.sortOrder != null ? row.sortOrder : 0,
    status: row.status === 1 || row.status === 'enabled' ? 1 : 0
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = {
      name: form.value.name,
      color: form.value.color,
      sortOrder: form.value.sortOrder,
      status: form.value.status
    }
    if (isEdit.value) {
      await updateTag(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await addTag(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除标签「${row.name}」？`, '警告', { type: 'warning' })
    await deleteTag(row.id)
    ElMessage.success('已删除')
    refresh()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(() => {
  refresh()
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
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.color-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.color-block {
  display: inline-block;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .tag-manage-page {
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

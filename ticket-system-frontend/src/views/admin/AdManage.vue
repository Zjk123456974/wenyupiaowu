<template>
  <div class="ad-manage-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>广告位管理</span>
          <el-button type="primary" @click="showAdd">新增广告</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-area" style="margin-bottom: 16px">
        <el-select v-model="filter.position" placeholder="广告位位置" clearable style="width: 160px" @change="fetchAds">
          <el-option label="弹窗" value="popup" />
          <el-option label="侧边栏" value="sidebar" />
          <el-option label="底部" value="footer" />
        </el-select>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <div class="table-scroll">
        <el-table :data="ads" v-loading="loading" stripe>
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="标题" prop="title" show-overflow-tooltip min-width="160" />
          <el-table-column label="图片预览" width="120" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                fit="cover"
                style="width: 80px; height: 50px; border-radius: 4px"
                :preview-src-list="[row.imageUrl]"
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
          <el-table-column label="广告位位置" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="positionTag(row.position)" size="small">{{ positionName(row.position) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="跳转链接" prop="linkUrl" show-overflow-tooltip min-width="180" />
          <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生效时间" width="320">
            <template #default="{ row }">
              <div style="font-size: 12px; line-height: 1.6">
                <div>开始：{{ formatTime(row.startTime) }}</div>
                <div>结束：{{ formatTime(row.endTime) }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
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

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="filter.page"
          v-model:page-size="filter.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchAds"
          @current-change="fetchAds"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑广告' : '新增广告'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入广告标题" />
        </el-form-item>
        <el-form-item label="广告图片" prop="imageUrl">
          <el-upload
            action="/api/upload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :show-file-list="false"
            accept="image/*"
            name="file"
          >
            <el-image v-if="form.imageUrl" :src="form.imageUrl" style="width: 100%; height: 120px; object-fit: cover;" fit="cover" lazy>
              <template #error>
                <div style="width: 100%; height: 120px; display: flex; align-items: center; justify-content: center; background: #f0f2f5;">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <div v-else style="width: 100%; height: 120px; display: flex; align-items: center; justify-content: center; background: #f0f2f5; border: 1px dashed #d9d9d9;">
              <el-icon style="font-size: 32px; color: #c0c4cc;"><Plus /></el-icon>
            </div>
          </el-upload>
          <div style="font-size:12px;color:#909399;margin-top:4px;">点击图片上传，或直接输入图片URL</div>
          <el-input v-model="form.imageUrl" placeholder="或直接输入图片URL" style="margin-top: 8px" />
        </el-form-item>
        <el-form-item label="广告位位置" prop="position">
          <el-select v-model="form.position" placeholder="请选择位置" style="width: 100%">
            <el-option label="弹窗" value="popup" />
            <el-option label="侧边栏" value="sidebar" />
            <el-option label="底部" value="footer" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="请输入跳转链接" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
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
        <el-form-item label="生效时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture } from '@element-plus/icons-vue'
import { getAdPage, addAd, updateAd, deleteAd } from '@/api/ad'
import { PLACEHOLDER_IMAGE } from '@/utils/placeholder'

const ads = ref([])
const total = ref(0)
const loading = ref(false)
const submitLoading = ref(false)

const filter = ref({
  page: 1,
  size: 10,
  position: ''
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({
  id: null,
  title: '',
  imageUrl: '',
  position: 'popup',
  linkUrl: '',
  sort: 0,
  status: 1,
  timeRange: null
})

const rules = {
  title: [{ required: true, message: '请输入广告标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请输入图片地址', trigger: 'blur' }],
  position: [{ required: true, message: '请选择广告位位置', trigger: 'change' }]
}

const fetchAds = async () => {
  loading.value = true
  try {
    const params = {
      page: filter.value.page,
      size: filter.value.size,
      position: filter.value.position || undefined
    }
    const res = await getAdPage(params)
    const data = res?.data || res
    ads.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    // 静默处理
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filter.value.position = ''
  filter.value.page = 1
  fetchAds()
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    form.value.imageUrl = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '图片上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('图片上传失败')
}

const showAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    title: '',
    imageUrl: '',
    position: 'popup',
    linkUrl: '',
    sort: 0,
    status: 1,
    timeRange: null
  }
  dialogVisible.value = true
}

const showEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    title: row.title,
    imageUrl: row.imageUrl,
    position: row.position,
    linkUrl: row.linkUrl || '',
    sort: row.sortOrder || 0,
    status: row.status === 1 ? 1 : 0,
    timeRange: (row.startTime && row.endTime)
      ? [String(row.startTime).replace('T', ' ').substring(0, 19), String(row.endTime).replace('T', ' ').substring(0, 19)]
      : null
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload = {
      title: form.value.title,
      imageUrl: form.value.imageUrl,
      position: form.value.position,
      linkUrl: form.value.linkUrl,
      sort: form.value.sort,
      status: form.value.status
    }
    if (form.value.timeRange && form.value.timeRange.length === 2) {
      payload.startTime = form.value.timeRange[0]
      payload.endTime = form.value.timeRange[1]
    }
    if (isEdit.value) {
      await updateAd(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await addAd(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchAds()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除广告「${row.title}」？`, '警告', { type: 'warning' })
    await deleteAd(row.id)
    ElMessage.success('已删除')
    fetchAds()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('操作失败')
  }
}

const positionName = (position) => {
  const map = { popup: '弹窗', sidebar: '侧边栏', footer: '底部' }
  return map[position] || '未知'
}

const positionTag = (position) => {
  const map = { popup: 'danger', sidebar: '', footer: 'success' }
  return map[position] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return typeof time === 'string' ? time.replace('T', ' ').substring(0, 19) : time
}

onMounted(() => {
  fetchAds()
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
  .ad-manage-page {
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

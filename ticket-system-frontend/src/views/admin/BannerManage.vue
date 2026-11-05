<template>
  <div class="banner-manage">
    <el-card class="filter-card">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 16px; font-weight: bold;">轮播图管理</span>
        <el-button type="primary" @click="showAddBanner">
          <el-icon><Plus /></el-icon> 新增轮播图
        </el-button>
      </div>
    </el-card>

    <div class="table-scroll">
      <el-table :data="banners" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" style="width: 80px; height: 40px; object-fit: cover;" fit="cover" lazy>
              <template #error>
                <div style="width: 80px; height: 40px; display: flex; align-items: center; justify-content: center; background: #f0f2f5;">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="关联活动" min-width="160">
          <template #default="{ row }">
            <span>{{ getActivityName(row.activityId) || '无' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
              />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="editBanner(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" :image-size="80" />
        </template>
      </el-table>
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑轮播图' : '新增轮播图'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入轮播图标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入轮播图描述" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            class="upload-demo"
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
        </el-form-item>
        <el-form-item label="关联活动">
          <el-select
            v-model="form.activityId"
            placeholder="选择跳转活动（可选）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option :value="0" label="不关联活动（不跳转）" />
            <el-option v-for="activity in activities" :key="activity.id" :label="activity.title" :value="activity.id" />
          </el-select>
          <div style="font-size:12px;color:#909399;margin-top:4px;">可选择任意活动，不选择则轮播图不跳转</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBanners, addBanner, updateBanner, deleteBanner } from '@/api/admin'
import request from '@/utils/request'
import { getActivityList } from '@/api/activity'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture } from '@element-plus/icons-vue'

const banners = ref([])
const activities = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const formVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  title: '',
  description: '',
  imageUrl: '',
  activityId: null
})

const activityNameMap = ref({})

const getActivityName = (activityId) => {
  return activityNameMap.value[activityId] || '无'
}

const loadData = async () => {
  try {
    const res = await getBanners({ page: page.value, size: size.value })
    const data = res.data || res
    // 后端返回分页对象 IPage<Banner>
    banners.value = data.records || data.list || data || []
    total.value = data.total || banners.value.length
  } catch (e) {
    console.error(e)
  }
}

const loadActivities = async () => {
  try {
    const res = await getActivityList({ page: 1, size: 100 })
    const list = res.data?.records || res.data?.list || []
    activities.value = list
    list.forEach(act => {
      activityNameMap.value[act.id] = act.title
    })
  } catch (e) {
    console.error(e)
  }
}

const showAddBanner = () => {
  isEdit.value = false
  form.value = {
    id: null,
    title: '',
    description: '',
    imageUrl: '',
    activityId: 0
  }
  formVisible.value = true
}

const editBanner = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    title: row.title || '',
    description: row.description || '',
    imageUrl: row.imageUrl || '',
    activityId: row.activityId || 0
  }
  formVisible.value = true
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

const handleSubmit = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.imageUrl) {
    ElMessage.warning('请上传图片')
    return
  }
  try {
    // activityId 为 0 表示不关联活动，提交时转为 null 传给后端
    const payload = { ...form.value, activityId: form.value.activityId || null }
    if (isEdit.value) {
      await updateBanner(form.value.id, payload)
      ElMessage.success('修改成功')
    } else {
      await addBanner(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
    ElMessage.error('操作失败')
  }
}

const handleStatusChange = async (row) => {
  try {
    await request.put('/banner/' + row.id + '/status', { status: row.status })
    ElMessage.success(row.status === 1 ? '已上线' : '已下线')
  } catch (e) {
    // 失败回滚开关值
    row.status = row.status === 1 ? 0 : 1
    console.error(e)
    ElMessage.error('状态修改失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该轮播图？', '警告', { type: 'warning' })
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(() => {
  loadData()
  loadActivities()
})
</script>

<style scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .banner-manage {
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
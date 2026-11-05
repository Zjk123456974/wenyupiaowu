<template>
  <div class="announcement-manage">
    <div class="top-bar">
      <el-button type="primary" @click="showAdd">新增公告</el-button>
    </div>

    <div class="table-scroll">
      <el-table :data="announcements" stripe style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否置顶" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isTop === 1 ? 'danger' : 'info'">
              {{ row.isTop === 1 ? '置顶' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" min-width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="showEdit(row)">编辑</el-button>
            <el-button
              v-if="row.isTop !== 1"
              type="warning"
              text
              size="small"
              @click="handleTop(row)"
            >置顶</el-button>
            <el-button
              v-if="row.isTop === 1"
              type="info"
              text
              size="small"
              @click="handleCancelTop(row)"
            >取消置顶</el-button>
            <el-button
              v-if="row.status === 1"
              type="warning"
              text
              size="small"
              @click="handleOffline(row)"
            >下架</el-button>
            <el-button
              v-if="row.status !== 1"
              type="success"
              text
              size="small"
              @click="handleOnline(row)"
            >上架</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新增公告'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getAdminAnnouncements,
  addAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  topAnnouncement,
  offlineAnnouncement,
  onlineAnnouncement
} from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ id: null, title: '', content: '', isTop: 0 })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const showAdd = () => {
  isEdit.value = false
  form.value = { id: null, title: '', content: '', isTop: 0 }
  dialogVisible.value = true
}

const showEdit = (row) => {
  isEdit.value = true
  form.value = { id: row.id, title: row.title, content: row.content, isTop: row.isTop || 0 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await updateAnnouncement(form.value.id, {
        title: form.value.title,
        content: form.value.content,
        isTop: form.value.isTop
      })
      ElMessage.success('更新成功')
    } else {
      await addAnnouncement({
        title: form.value.title,
        content: form.value.content,
        isTop: form.value.isTop
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleTop = async (row) => {
  try {
    await topAnnouncement(row.id)
    ElMessage.success('已置顶')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleCancelTop = async (row) => {
  try {
    await updateAnnouncement(row.id, { isTop: 0 })
    ElMessage.success('已取消置顶')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleOffline = async (row) => {
  try {
    await offlineAnnouncement(row.id)
    ElMessage.success('已下架')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleOnline = async (row) => {
  try {
    await onlineAnnouncement(row.id)
    ElMessage.success('已上架')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '警告', { type: 'warning' })
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const loadData = async () => {
  try {
    const res = await getAdminAnnouncements({ page: page.value, size: size.value })
    const data = res.data || res
    announcements.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.top-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .announcement-manage {
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

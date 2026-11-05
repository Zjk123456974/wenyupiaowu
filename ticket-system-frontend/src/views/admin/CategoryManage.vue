<template>
  <div class="category-manage">
    <div class="top-bar">
      <el-button type="primary" @click="showAdd">新增分类</el-button>
    </div>

    <div class="table-scroll">
      <el-table :data="categories" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="450px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { getCategories, addCategory, updateCategory, deleteCategory } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ id: null, name: '', sortOrder: 0 })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const showAdd = () => {
  isEdit.value = false
  form.value = { id: null, name: '', sortOrder: 0 }
  dialogVisible.value = true
}

const showEdit = (row) => {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, sortOrder: row.sortOrder || 0 }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (isEdit.value) {
      await updateCategory(form.value.id, { name: form.value.name, sortOrder: form.value.sortOrder })
      ElMessage.success('更新成功')
    } else {
      await addCategory({ name: form.value.name, sortOrder: form.value.sortOrder })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该分类？', '警告', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const loadData = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || res || []
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

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .category-manage {
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

<template>
  <div class="user-manage">
    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-input v-model="keyword" placeholder="搜索账号/昵称/手机号" clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="4">
          <el-select v-model="roleFilter" placeholder="角色" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="普通用户" :value="0" />
            <el-option label="运营" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="封禁" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-button type="success" @click="showAddOperator">新增运营账号</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 首屏骨架屏：列数与真实表格匹配，避免加载完成后的布局跳动 -->
    <div v-if="loading && users.length === 0" class="table-skeleton">
      <el-skeleton :count="8" animated>
        <template #template>
          <div class="skeleton-row">
            <el-skeleton-item variant="text" style="width: 60%" />
            <el-skeleton-item variant="text" style="width: 55%" />
            <el-skeleton-item variant="text" style="width: 40%" />
            <el-skeleton-item variant="text" style="width: 30%" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else class="table-scroll" style="margin-top: 16px">
      <el-table :data="users" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)">{{ roleLabel(row.role, row.operatorType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.role === 1 && row.status === 0" type="warning">
              待审核
            </el-tag>
            <el-tag v-else :type="row.status === 0 ? 'danger' : 'success'">
              {{ row.status === 0 ? '封禁' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="showEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              text
              size="small"
              @click="showBan(row)"
            >封禁</el-button>
            <el-button
              v-if="row.status === 0 && row.role !== 1"
              type="success"
              text
              size="small"
              @click="handleUnban(row)"
            >解封</el-button>
            <el-button
              v-if="row.role === 1 && row.status === 0"
              type="success"
              text
              size="small"
              @click="handleReviewOperator(row, true)"
            >通过</el-button>
            <el-button
              v-if="row.role === 1 && row.status === 0"
              type="danger"
              text
              size="small"
              @click="handleReviewOperator(row, false)"
            >驳回</el-button>
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="450px">
      <el-form ref="editFormRef" :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="普通用户" :value="0" />
            <el-option label="运营" :value="1" />
            <el-option label="管理员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="Number(editForm.role) === 1" label="运营类型">
          <el-select v-model="editForm.operatorType" style="width: 100%">
            <el-option label="普通运营（原功能不变）" :value="0" />
            <el-option label="活动运营（可核销本人负责的活动）" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 封禁对话框 -->
    <el-dialog v-model="banVisible" title="封禁用户" width="450px">
      <el-form :model="banForm" label-width="80px">
        <el-form-item label="封禁原因">
          <el-input v-model="banForm.banReason" type="textarea" :rows="3" placeholder="请输入封禁原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" @click="handleBan">确定封禁</el-button>
      </template>
    </el-dialog>

    <!-- 新增运营账号对话框 -->
    <el-dialog v-model="addOperatorVisible" title="新增运营账号" width="450px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="addForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="运营类型">
          <el-select v-model="addForm.operatorType" style="width: 100%">
            <el-option label="普通运营（原功能不变）" :value="0" />
            <el-option label="活动运营（可核销本人负责的活动）" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addOperatorVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddOperator">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserPage, updateUser, banUser, unbanUser, deleteUser, registerOperator, reviewOperator } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const keyword = ref('')
const roleFilter = ref('')
const statusFilter = ref('')
const users = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
// 首屏加载态：用于骨架屏展示，翻页时仍用 v-loading 遮罩
const loading = ref(false)

const roleMap = { 0: '普通用户', 1: '运营', 2: '管理员' }
const roleTagMap = { 0: '', 1: 'warning', 2: 'danger' }
// 运营细分：operatorType 0 普通运营 / 1 活动运营
const roleLabel = (role, operatorType) => {
  if (Number(role) === 1) {
    return Number(operatorType) === 1 ? '活动运营' : '普通运营'
  }
  return roleMap[role] || '未知'
}
const roleTagType = (role) => roleTagMap[role] || ''

// 编辑
const editVisible = ref(false)
const editFormRef = ref(null)
const editForm = ref({ id: null, nickname: '', phone: '', role: 0, operatorType: 0 })

const showEdit = (row) => {
  editForm.value = {
    id: row.id,
    nickname: row.nickname,
    phone: row.phone,
    role: row.role,
    operatorType: row.operatorType || 0
  }
  editVisible.value = true
}

const handleEdit = async () => {
  try {
    await updateUser(editForm.value.id, {
      nickname: editForm.value.nickname,
      phone: editForm.value.phone,
      role: editForm.value.role,
      operatorType: editForm.value.operatorType
    })
    ElMessage.success('更新成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

// 封禁
const banVisible = ref(false)
const banForm = ref({ id: null, banReason: '' })

const showBan = (row) => {
  banForm.value = { id: row.id, banReason: '' }
  banVisible.value = true
}

const handleBan = async () => {
  if (!banForm.value.banReason) {
    ElMessage.warning('请输入封禁原因')
    return
  }
  try {
    await banUser(banForm.value.id, banForm.value.banReason)
    ElMessage.success('已封禁')
    banVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleUnban = async (row) => {
  try {
    await ElMessageBox.confirm('确定解封该用户？', '提示', { type: 'warning' })
    await unbanUser(row.id)
    ElMessage.success('已解封')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 运营账号审核
const handleReviewOperator = async (row, approved) => {
  try {
    await ElMessageBox.confirm(`确定${approved ? '通过' : '驳回'}该运营账号？`, '审核确认', { type: 'warning' })
    await reviewOperator(row.id, approved)
    ElMessage.success(approved ? '已通过' : '已驳回')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该用户？此操作不可恢复！', '警告', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 新增运营
const addOperatorVisible = ref(false)
const addFormRef = ref(null)
const addForm = ref({ username: '', password: '', nickname: '', phone: '', operatorType: 0 })
const addRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const showAddOperator = () => {
  addForm.value = { username: '', password: '', nickname: '', phone: '', operatorType: 0 }
  addOperatorVisible.value = true
}

const handleAddOperator = async () => {
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await registerOperator(addForm.value)
    ElMessage.success('创建成功')
    addOperatorVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (roleFilter.value !== '' && roleFilter.value !== null && roleFilter.value !== undefined) params.role = roleFilter.value
    if (statusFilter.value !== '' && statusFilter.value !== null && statusFilter.value !== undefined) params.status = statusFilter.value
    const res = await getUserPage(params)
    const data = res.data || res
    users.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  width: 100%;
  table-layout: fixed;
}

:deep(.el-table th) {
  text-align: center;
}

:deep(.el-table td) {
  text-align: center;
}

:deep(.el-table--border .el-table__cell) {
  border-right: 1px solid #ebeef5;
}

:deep(.el-table--border .el-table__cell:last-child) {
  border-right: none;
}

/* 骨架屏：行高与表格行高接近，加载完成时不产生明显跳动 */
.table-skeleton {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 48px;
  border-bottom: 1px solid #f0f2f5;
}

.skeleton-row:last-child {
  border-bottom: none;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .user-manage {
    max-width: 100%;
    box-sizing: border-box;
  }

  /* 筛选表单：栅格改单列 */
  .filter-card :deep(.el-row) {
    row-gap: 10px;
  }

  .filter-card :deep(.el-col) {
    flex: 0 0 100%;
    max-width: 100%;
    text-align: left !important;
  }

  .filter-card :deep(.el-button) {
    width: 100%;
    margin-left: 0 !important;
  }

  /* 表格容器横向滚动，列宽保持不变 */
  .table-scroll {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .table-scroll :deep(.el-table) {
    min-width: 700px;
  }

  .pagination-wrap {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>

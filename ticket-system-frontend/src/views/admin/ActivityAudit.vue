<template>
  <div class="activity-audit">
    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" style="width: 100%">
            <el-option label="全部" value="" />
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
            <el-option label="已上架" :value="3" />
            <el-option label="已下架" :value="4" />
          </el-select>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-button type="success" @click="$router.push('/operator/activity/add')">新增活动</el-button>
          <el-button type="primary" @click="handleSearch">刷新</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 首屏骨架屏：列数与真实表格匹配，避免加载完成后的布局跳动 -->
    <div v-if="loading && activities.length === 0" class="table-skeleton">
      <el-skeleton :count="8" animated>
        <template #template>
          <div class="skeleton-row">
            <el-skeleton-item variant="text" style="width: 70%" />
            <el-skeleton-item variant="text" style="width: 40%" />
            <el-skeleton-item variant="text" style="width: 35%" />
            <el-skeleton-item variant="text" style="width: 55%" />
          </div>
        </template>
      </el-skeleton>
    </div>

    <div v-else class="table-scroll" style="margin-top: 16px">
      <el-table :data="activities" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="活动名称" min-width="160" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="operatorName" label="运营" width="100" />
        <el-table-column prop="price" label="票价" width="80">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="totalTickets" label="票数" width="80" />
        <el-table-column label="抢票时间" min-width="180">
          <template #default="{ row }">
            {{ row.grabStartTime }} ~ {{ row.grabEndTime }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              size="small"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-button
              v-if="row.status === 0"
              type="success"
              text
              size="small"
              @click="handleAuditPass(row)"
            >审核通过</el-button>
            <el-button
              v-if="row.status === 0"
              type="warning"
              text
              size="small"
              @click="showReject(row)"
            >审核驳回</el-button>
            <el-button
              v-if="row.status === 1 || row.status === 4"
              type="success"
              text
              size="small"
              @click="handlePublish(row)"
            >上架</el-button>
            <el-button
              v-if="row.status === 3"
              type="danger"
              text
              size="small"
              @click="handleForceOffline(row)"
            >强制下架</el-button>
            <el-button
              type="info"
              text
              size="small"
              @click="showStock(row)"
            >查看库存</el-button>
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

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="审核驳回" width="450px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" @click="handleReject">确定驳回</el-button>
      </template>
    </el-dialog>

    <!-- 库存对话框 -->
    <el-dialog v-model="stockVisible" title="库存信息" width="450px">
      <el-descriptions :column="1" border v-if="stockInfo">
        <el-descriptions-item label="总票数">{{ stockInfo.totalTickets }}</el-descriptions-item>
        <el-descriptions-item label="已售票数">{{ stockInfo.soldTickets }}</el-descriptions-item>
        <el-descriptions-item label="剩余票数">{{ stockInfo.remainTickets }}</el-descriptions-item>
        <el-descriptions-item label="Redis库存">{{ stockInfo.redisStock }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 编辑活动对话框 -->
    <el-dialog
      v-model="editVisible"
      title="编辑活动"
      width="780px"
      top="6vh"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="110px"
        style="padding-right: 12px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="活动名称" prop="title">
              <el-input v-model="editForm.title" placeholder="请输入活动名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="cat in categories"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="举办地点" prop="venue">
          <el-input v-model="editForm.venue" placeholder="请输入举办地点" maxlength="200" />
        </el-form-item>

        <el-form-item label="活动时间" prop="activityTime">
          <el-date-picker
            v-model="editForm.activityTime"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="封面图">
          <div class="cover-wrap">
            <el-upload
              class="cover-uploader"
              action="/api/upload"
              name="file"
              accept="image/*"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <el-image v-if="editForm.coverImage" :src="editForm.coverImage" fit="cover" class="cover-preview" lazy>
                <template #error>
                  <div class="cover-empty"><el-icon :size="24"><Picture /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="cover-empty cover-empty-lg">
                <el-icon :size="28" color="#c0c4cc"><Plus /></el-icon>
                <div class="cover-upload-text">点击上传图片</div>
              </div>
            </el-upload>
            <el-input
              v-model="editForm.coverImage"
              placeholder="或直接输入封面图URL"
              clearable
              style="margin-top: 8px"
            >
              <template #append>
                <el-button @click="previewVisible = true">预览</el-button>
              </template>
            </el-input>
            <div class="cover-tip">支持 JPG / PNG / GIF / WebP，大小不超过 10MB</div>
          </div>
        </el-form-item>

        <el-form-item label="活动介绍">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入活动介绍" />
        </el-form-item>

        <el-form-item label="票务规则">
          <el-input v-model="editForm.ticketRule" type="textarea" :rows="3" placeholder="请输入票务规则" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="限购数量" prop="purchaseLimit">
              <el-input-number v-model="editForm.purchaseLimit" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总票数(自动)" prop="totalTickets">
              <el-input :model-value="tierTotal" disabled style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="票档配置" required>
          <div class="tier-editor">
            <div class="tier-head">
              <span class="th-seat">票档 / 座位类型</span>
              <span class="th-price">票价（元）</span>
              <span class="th-stock">可售库存（张）</span>
              <span class="th-ops"></span>
            </div>
            <div v-for="(tier, idx) in editForm.tiers" :key="idx" class="tier-row">
              <el-input
                v-model="tier.seatType"
                class="td-seat"
                placeholder="如：VIP / 看台 / 学生票"
                :maxlength="30"
              />
              <el-input-number
                v-model="tier.price"
                class="td-price"
                :min="0.01"
                :precision="2"
                :controls="false"
                :max="99999"
                placeholder="票价"
              />
              <el-input-number
                v-model="tier.totalStock"
                class="td-stock"
                :min="1"
                :max="100000"
                :controls="false"
                placeholder="可售张数"
              />
              <div class="td-ops">
                <el-button v-if="editForm.tiers.length > 1" text type="danger" @click="removeTier(idx)">删除</el-button>
                <span v-else class="min-tip">至少1个票档</span>
              </div>
            </div>
            <div class="tier-actions">
              <el-button type="primary" plain size="small" @click="addTier">＋ 添加票档</el-button>
              <span class="tier-summary">
                共 <b>{{ editForm.tiers.length }}</b> 档 · 起步价 <b>¥{{ tierMinPrice || '—' }}</b>
              </span>
            </div>
            <div class="tier-tip">保存后活动的总票数与起步价将自动按各票档合计与最低价同步，用户端可选购任意有库存的票档。</div>
          </div>
        </el-form-item>

        <el-form-item label="抢票时间" prop="grabTime">
          <el-date-picker
            v-model="editForm.grabTime"
            type="datetimerange"
            range-separator="至"
            start-placeholder="抢票开始时间"
            end-placeholder="抢票结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-divider content-position="left">3D 场馆视野</el-divider>

        <el-form-item label="3D 视野预览">
          <el-switch
            v-model="editForm.view3dEnabled"
            :active-value="1"
            :inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
          />
          <div class="cfg-tip">开启后，用户在该活动选座面板可一键进入 3D 场馆：全景漫游查看场馆与座位分布，并预览“从所选座位看出去”的视野，提升购票体验（需该活动已开启座位定位 / 座位平面图）。</div>
        </el-form-item>

        <el-form-item label="3D 场馆模型" v-if="editForm.view3dEnabled === 1">
          <el-select v-model="editForm.view3dModel" placeholder="选择 3D 场馆类型" style="width: 360px">
            <el-option
              v-for="opt in venueModelOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            >
              <span style="float: left">{{ opt.label }}</span>
              <span style="float: right; color: #909399; font-size: 12px; margin-left: 14px">{{ opt.desc }}</span>
            </el-option>
          </el-select>
          <div class="cfg-tip">为不同活动匹配合适的 3D 场馆外观：演唱会 / 电竞选「碗形综合馆」，话剧歌剧芭蕾选「镜框式剧院」，足球田径等选「标准体育场」，大型户外演唱会选「露天音乐场」。</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 封面预览 -->
    <el-dialog v-model="previewVisible" title="封面图预览" width="480px" append-to-body>
      <el-image v-if="editForm.coverImage" :src="editForm.coverImage" fit="contain" style="width: 100%" lazy>
        <template #error>
          <div class="cover-empty"><el-icon :size="24"><Picture /></el-icon></div>
        </template>
      </el-image>
      <el-empty v-else description="尚未设置封面图" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { getAdminActivities, auditActivity, forceOffline, getStockInfo, updateAdminActivity, getCategories } from '@/api/admin'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture } from '@element-plus/icons-vue'

const statusFilter = ref('')
const activities = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
// 首屏加载态：用于骨架屏展示，翻页时仍用 v-loading 遮罩
const loading = ref(false)

const statusMap = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
  3: '已上架',
  4: '已下架'
}
const statusTagTypeMap = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'success',
  4: 'info'
}
const statusLabel = (status) => statusMap[status] || '未知'
const statusTagType = (status) => statusTagTypeMap[status] || 'info'

// 驳回
const rejectVisible = ref(false)
const rejectForm = ref({ id: null, reason: '' })

const showReject = (row) => {
  rejectForm.value = { id: row.id, reason: '' }
  rejectVisible.value = true
}

const handleReject = async () => {
  if (!rejectForm.value.reason) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    await auditActivity(rejectForm.value.id, { approved: false, reason: rejectForm.value.reason })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleAuditPass = async (row) => {
  try {
    await ElMessageBox.confirm('确定审核通过该活动？', '提示', { type: 'success' })
    await auditActivity(row.id, { approved: true })
    ElMessage.success('已通过')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleForceOffline = async (row) => {
  try {
    await ElMessageBox.confirm('确定强制下架该活动？', '警告', { type: 'warning' })
    await forceOffline(row.id)
    ElMessage.success('已强制下架')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handlePublish = async (row) => {
  try {
    const tip = row.status === 4
      ? '该活动当前已下架，确定重新上架？上架后将恢复用户端展示与购票'
      : '确定上架该活动？'
    await ElMessageBox.confirm(tip, '提示', { type: 'success' })
    await request.put(`/admin/activity/${row.id}/publish`)
    ElMessage.success('已上架')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 库存
const stockVisible = ref(false)
const stockInfo = ref(null)

const showStock = async (row) => {
  try {
    const res = await getStockInfo(row.id)
    stockInfo.value = res.data || res || {}
    stockVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// ===== 编辑活动 =====
const categories = ref([])
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const previewVisible = ref(false)

const emptyEditForm = () => ({
  id: null,
  title: '',
  categoryId: null,
  venue: '',
  activityTime: null,
  coverImage: '',
  description: '',
  ticketRule: '',
  purchaseLimit: 1,
  totalTickets: 0,
  price: 0,
  grabTime: null,
  view3dEnabled: 0,
  view3dModel: 'arena_bowl',
  tiers: [{ seatType: '', price: null, totalStock: 100 }]
})
const editForm = ref(emptyEditForm())

// 3D 场馆模型选项：不同活动类型对应不同场馆外观（管理员可手动选择）
const venueModelOptions = [
  { value: 'arena_bowl', label: '碗形综合馆', desc: '演唱会 / 电竞综合' },
  { value: 'theatre', label: '镜框式剧院', desc: '话剧 / 歌剧 / 芭蕾' },
  { value: 'stadium', label: '标准体育场', desc: '足球 / 田径赛事' },
  { value: 'concert', label: '露天音乐场', desc: '大型户外演唱会' }
]

const editRules = {
  title: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  venue: [{ required: true, message: '请输入举办地点', trigger: 'blur' }],
  activityTime: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
  totalTickets: [{ required: true, message: '请输入总票数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入票价', trigger: 'blur' }],
  grabTime: [{ required: true, message: '请选择抢票时间', trigger: 'change' }]
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || res || []
  } catch (e) {
    console.error(e)
  }
}

const openEdit = (row) => {
  editForm.value = {
    id: row.id,
    title: row.title || '',
    categoryId: row.categoryId || null,
    venue: row.venue || '',
    activityTime: row.startTime && row.endTime ? [row.startTime, row.endTime] : null,
    coverImage: row.coverImage || '',
    description: row.description || '',
    ticketRule: row.ticketRule || '',
    purchaseLimit: row.purchaseLimit || 1,
    totalTickets: row.totalTickets != null ? row.totalTickets : 0,
    price: row.price != null ? Number(row.price) : 0,
    grabTime: row.grabStartTime && row.grabEndTime ? [row.grabStartTime, row.grabEndTime] : null,
    view3dEnabled: row.view3dEnabled != null ? Number(row.view3dEnabled) : 0,
    view3dModel: row.view3dModel || 'arena_bowl',
    tiers: [
      {
        seatType: '',
        price: row.price != null ? Number(row.price) : 0,
        totalStock: row.totalTickets != null && row.totalTickets > 0 ? row.totalTickets : 100
      }
    ]
  }
  editVisible.value = true
  loadEditTiers(row.id)
}

// 编辑弹窗：回显活动已配置的票档（多票价）
const loadEditTiers = async (activityId) => {
  try {
    const res = await request.get(`/ticket-price/activity/${activityId}`)
    const list = Array.isArray(res.data) ? res.data : []
    if (list.length) {
      editForm.value.tiers = list.map((t) => ({
        id: t.id,
        seatType: t.seatType || '',
        price: t.price != null ? Number(t.price) : 0,
        totalStock: t.totalStock != null ? Number(t.totalStock) : 100
      }))
    }
  } catch (e) {
    /* 回显失败时保留默认单档 */
  }
}

const tierTotal = computed(() =>
  (editForm.value.tiers || []).reduce((s, t) => s + (Number(t.totalStock) || 0), 0)
)
const tierMinPrice = computed(() => {
  const prices = (editForm.value.tiers || []).map((t) => Number(t.price)).filter((p) => p > 0)
  return prices.length ? Math.min(...prices) : null
})

function addTier() {
  editForm.value.tiers.push({ seatType: '', price: null, totalStock: 100 })
}

function removeTier(index) {
  if (editForm.value.tiers.length <= 1) return
  editForm.value.tiers.splice(index, 1)
}

const beforeUpload = (file) => {
  // 放宽校验：所有 image/* 类型均允许；个别环境 file.type 为空时按扩展名兜底
  const isImage = file.type
    ? file.type.startsWith('image/')
    : /\.(jpe?g|png|gif|webp|bmp|avif|svg)$/i.test(file.name || '')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.error(`仅支持上传图片文件（当前类型：${file.type || file.name || '未知'}）`)
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    editForm.value.coverImage = response.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '封面上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('封面上传失败')
}

const submitEdit = async () => {
  // 票档校验
  const tiers = editForm.value.tiers
  if (!tiers || !tiers.length) {
    ElMessage.warning('请至少配置一个票档')
    return
  }
  for (const t of tiers) {
    if (!t.seatType || !String(t.seatType).trim()) {
      ElMessage.warning('请填写完整的票档名称（座位类型）')
      return
    }
    if (!(Number(t.price) > 0)) {
      ElMessage.warning(`请为票档【${t.seatType}】填写大于 0 的票价`)
      return
    }
    if (!(Number(t.totalStock) >= 1)) {
      ElMessage.warning(`请为票档【${t.seatType}】填写可售库存`)
      return
    }
  }

  // 总票数=各档合计、票价=最低档，与票档保持同步（后端保存时也会再校准）
  editForm.value.totalTickets = tierTotal.value
  editForm.value.price = tierMinPrice.value || 0

  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: editForm.value.title,
    categoryId: editForm.value.categoryId,
    venue: editForm.value.venue,
    startTime: editForm.value.activityTime?.[0] || '',
    endTime: editForm.value.activityTime?.[1] || '',
    coverImage: editForm.value.coverImage || '',
    description: editForm.value.description || '',
    ticketRule: editForm.value.ticketRule || '',
    purchaseLimit: editForm.value.purchaseLimit,
    totalTickets: editForm.value.totalTickets,
    price: editForm.value.price,
    grabStartTime: editForm.value.grabTime?.[0] || '',
    grabEndTime: editForm.value.grabTime?.[1] || '',
    view3dEnabled: Number(editForm.value.view3dEnabled ?? 0),
    view3dModel: editForm.value.view3dModel || 'arena_bowl'
  }

  editSubmitting.value = true
  try {
    await updateAdminActivity(editForm.value.id, payload)
    await request.put(`/operator/activity/${editForm.value.id}/tiers`, {
      tiers: tiers.map((t) => ({
        id: t.id || null,
        seatType: String(t.seatType).trim(),
        price: Number(t.price),
        totalStock: Number(t.totalStock)
      }))
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    editSubmitting.value = false
  }
}

// ===== 列表加载 =====
const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value !== '' && statusFilter.value !== null && statusFilter.value !== undefined) {
      params.status = statusFilter.value
    }
    const res = await getAdminActivities(params)
    const data = res.data || res
    activities.value = data.records || data.list || []
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
  loadCategories()
})
</script>

<style scoped>
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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
  .activity-audit {
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
    min-width: 860px;
  }

  .pagination-wrap {
    justify-content: center;
    overflow-x: auto;
  }
}

.cover-wrap {
  width: 100%;
}

.cover-empty {
  width: 240px;
  height: 135px;
  border-radius: 8px;
  background: #f5f7fa;
  border: 1px dashed #d9d9d9;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
}

.cover-empty:hover {
  border-color: #409eff;
}

.cover-empty-lg {
  flex-direction: column;
  gap: 6px;
}

.cover-preview {
  width: 240px;
  height: 135px;
  border-radius: 8px;
  display: block;
  cursor: pointer;
}

.cover-upload-text {
  font-size: 12px;
  color: #909399;
}

.cover-tip {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 4px;
}

.tier-editor {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafbfc;
}
.tier-head,
.tier-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tier-head {
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}
.th-seat { width: 300px; }
.th-price { width: 140px; }
.th-stock { width: 140px; }
.th-ops { flex: 1; }
.tier-row { margin-bottom: 10px; }
.td-seat { width: 300px; }
.td-price { width: 140px; }
.td-stock { width: 140px; }
.td-ops {
  flex: 1;
  display: flex;
  align-items: center;
}
.min-tip { font-size: 12px; color: #c0c4cc; }
.tier-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 2px;
}
.tier-summary { font-size: 13px; color: #606266; }
.tier-summary b { color: var(--el-color-primary); }
.tier-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>

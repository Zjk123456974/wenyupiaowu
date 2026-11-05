<template>
  <div class="page-container">
    <el-page-header :content="isEdit ? '编辑活动' : '发布活动'" @back="goBack" />

    <el-card class="form-card" shadow="never">
      <template #header>
        <span>{{ isEdit ? '编辑演出活动' : '发布新演出活动' }}</span>
        <span v-if="isEdit" class="form-status">
          当前状态：
          <el-tag size="small" :type="statusTag.type">{{ statusTag.text }}</el-tag>
        </span>
      </template>

      <el-form ref="formRef" :model="form" label-width="110px" :disabled="isEdit && !editable">
        <el-alert
          v-if="isEdit && !editable"
          type="warning"
          :closable="false"
          show-icon
          title="该活动已发布或进入后续流程，仅可查看，如需调整请先申请下架。"
          style="margin-bottom: 20px"
        />

        <el-form-item label="活动标题" prop="title" required>
          <el-input v-model="form.title" placeholder="请输入活动标题" :maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="活动类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择活动类型">
            <el-option v-for="t in activityTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <el-form-item label="活动分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="活动标签" prop="tagIds">
          <el-select v-model="form.tagIds" multiple placeholder="可多选活动标签">
            <el-option v-for="t in tagOptions" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="活动地点" prop="venue">
          <el-input v-model="form.venue" placeholder="请输入举办场馆/地点" :maxlength="80" />
        </el-form-item>

        <el-form-item label="演出时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择演出开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="演出时长" prop="duration">
          <el-input-number v-model="form.duration" :min="30" :step="30" />
          <span class="unit">分钟</span>
        </el-form-item>

        <el-form-item label="开抢时间" prop="grabStartTime">
          <el-date-picker
            v-model="form.grabStartTime"
            type="datetime"
            placeholder="选择开抢时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="抢票结束" prop="grabEndTime">
          <el-date-picker
            v-model="form.grabEndTime"
            type="datetime"
            placeholder="选择抢票结束时间（需晚于开抢、不晚于演出开始）"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <div class="form-tip">未设置时系统无法正确展示“抢票中”状态，且抢票不会自动截止。</div>
        </el-form-item>

        <el-form-item label="限购数量" prop="purchaseLimit">
          <el-input-number v-model="form.purchaseLimit" :min="1" :max="20" :step="1" />
          <span class="unit">张/人</span>
          <div class="form-tip">每个实名观演人最多可购张数，用于防黄牛；不设则不限购。</div>
        </el-form-item>

        <el-form-item label="票档配置" required>
          <div class="tier-editor">
            <div class="tier-head">
              <span class="th-seat">座位类型 / 票档名称</span>
              <span class="th-price">票价（元）</span>
              <span class="th-stock">可售库存（张）</span>
              <span class="th-ops"></span>
            </div>
            <div v-for="(tier, idx) in form.tiers" :key="idx" class="tier-row">
              <el-input
                v-model="tier.seatType"
                class="td-seat"
                placeholder="如：VIP座 / 看台A区 / 学生票"
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
                <el-button v-if="form.tiers.length > 1" text type="danger" @click="removeTier(idx)">删除</el-button>
                <span v-else class="min-tip">至少1个票档</span>
              </div>
            </div>
            <div class="tier-actions">
              <el-button type="primary" plain size="small" @click="addTier">＋ 添加票档</el-button>
              <span class="tier-summary">
                合计 <b>{{ tierTotal }}</b> 张 · ¥{{ tierMinPrice || '—' }} 起
              </span>
            </div>
            <div class="tier-tip">
              票档决定用户可选的价位与分档库存；保存后活动的“总票数/起步价”将自动同步为各票档合计与最低价。
              仅待审核 / 被驳回的活动可维护票档。
            </div>
          </div>
        </el-form-item>

        <el-form-item label="活动封面" prop="imageUrl">
          <div class="cover-upload" @click="triggerUpload">
            <img v-if="form.imageUrl" :src="form.imageUrl" class="cover-preview" alt="cover" />
            <template v-else>
              <span class="cover-placeholder-icon">+</span>
              <span>上传封面图</span>
            </template>
            <input ref="fileInput" type="file" accept="image/*" style="display: none" @change="handleUpload" />
          </div>
          <div class="upload-tip">建议尺寸 750 × 420，支持 jpg / png，单张不超过 5MB</div>
        </el-form-item>

        <el-form-item label="活动详情">
          <el-input v-model="form.description" type="textarea" :rows="8" placeholder="请输入活动详情介绍" :maxlength="2000" show-word-limit />
        </el-form-item>

        <el-divider content-position="left">票务规则设置</el-divider>

        <el-form-item label="退票策略">
          <el-radio-group v-model="form.refundPolicy">
            <el-radio :label="1">可退票</el-radio>
            <el-radio :label="0">不可退票（如演唱会）</el-radio>
          </el-radio-group>
          <div class="cfg-tip">不可退票的场次，用户购票后将无法申请退票，页面会给出明确提示</div>
        </el-form-item>

        <el-form-item label="实名模式">
          <el-radio-group v-model="form.realNameMode">
            <el-radio :label="0">免实名</el-radio>
            <el-radio :label="1">一单一实名（电竞，1 个实名可购多张）</el-radio>
            <el-radio :label="2">一票一实名（演唱会，每票都须实名）</el-radio>
          </el-radio-group>
          <div class="cfg-tip">一票一实名：每张票必须填一位观演人身份证；一单一实名：只需填 1 位实名购票人即可购买多张，入场凭电子票验证码核销</div>
        </el-form-item>

        <el-form-item label="场馆地图">
          <el-radio-group v-model="form.seatMapEnabled">
            <el-radio :label="0">不开启座位定位</el-radio>
            <el-radio :label="1">开启座位定位（电子票显示具体座位）</el-radio>
          </el-radio-group>
          <div class="cfg-tip">开启后，用户支付的每张电子票会自动分配具体的区/排/座，可在“我的电子票”中直接定位座位；不开启则仅显示座位区域</div>
        </el-form-item>

        <el-form-item label="3D 视野预览">
          <el-switch
            v-model="form.view3dEnabled"
            :active-value="1"
            :inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
          />
          <div class="cfg-tip">开启后，用户在选座面板可一键进入 3D 场馆：全景漫游查看场馆与座位分布，并预览“从所选座位看出去”的视野，提升购票体验（需同时开启“场馆地图 / 座位定位”）。</div>
        </el-form-item>

        <el-form-item label="3D 场馆模型" v-if="form.view3dEnabled === 1">
          <el-select v-model="form.view3dModel" placeholder="选择 3D 场馆类型" @change="modelTouched = true" style="width: 360px">
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
          <div class="cfg-tip">为不同活动匹配合适的 3D 场馆：演唱会 / 电竞选「碗形综合馆」，话剧歌剧芭蕾选「镜框式剧院」，足球田径等选「标准体育场」，大型户外演唱会选「露天音乐场」。系统已按活动类型自动推荐，可手动调整。</div>
        </el-form-item>

        <el-form-item label="座位平面图" v-if="form.seatMapEnabled === 1">
          <div class="seat-layout-editor">
            <div class="seat-section-head">
              <span class="slh-id">区号</span>
              <span class="slh-name">分区名称</span>
              <span class="slh-rows">排数</span>
              <span class="slh-cols">每排座位数</span>
              <span class="slh-price">票价(可选)</span>
              <span class="slh-ops"></span>
            </div>
            <div v-for="(sec, idx) in seatSections" :key="idx" class="seat-section-row">
              <el-input v-model="sec.id" class="sl-id" maxlength="4" placeholder="如 A" />
              <el-input v-model="sec.name" class="sl-name" maxlength="20" placeholder="如 A区" />
              <el-input-number v-model="sec.rows" class="sl-rows" :min="1" :max="100" :controls="false" />
              <el-input-number v-model="sec.cols" class="sl-cols" :min="1" :max="200" :controls="false" />
              <el-input-number v-model="sec.price" class="sl-price" :min="0" :precision="2" :controls="false" placeholder="可选" />
              <div class="sl-ops">
                <el-button v-if="seatSections.length > 1" text type="danger" @click="removeSeatSection(idx)">删除</el-button>
              </div>
            </div>
            <div class="seat-section-actions">
              <el-button type="primary" plain size="small" @click="addSeatSection">＋ 添加分区</el-button>
              <span class="seat-summary">合计 {{ seatTotalSeats }} 个座位</span>
            </div>
            <div class="seat-tip">开启座位定位后，用户购票时可在「在线选座」页面按分区/排/座挑选具体位置；座位编码为「区号-排-座」，如 A-3-5。</div>
          </div>
        </el-form-item>

        <el-form-item label="活动注意事项">
          <el-input v-model="form.ticketNotice" type="textarea" :rows="3" placeholder="补充说明退票/实名/入场等注意事项（显示在活动详情“购票须知”中）" :maxlength="1000" show-word-limit />
        </el-form-item>

        <el-divider content-position="left">演出阵容 / 嘉宾</el-divider>

        <el-form-item label="演出阵容">
          <div class="lineup-editor">
            <div class="lineup-head">
              <span class="lh-name">姓名</span>
              <span class="lh-role">角色</span>
              <span class="lh-avatar">头像URL（可空）</span>
              <span class="lh-desc">简介</span>
              <span class="lh-sort">排序</span>
              <span class="lh-ops"></span>
            </div>
            <div v-for="(g, idx) in lineup" :key="idx" class="lineup-row">
              <el-input v-model="g.name" class="lg-name" placeholder="如：薛之谦" :maxlength="50" />
              <el-input v-model="g.role" class="lg-role" placeholder="如：主演" :maxlength="30" />
              <el-input v-model="g.avatar" class="lg-avatar" placeholder="https://...（可空）" :maxlength="500" />
              <el-input v-model="g.description" class="lg-desc" placeholder="一句话简介" :maxlength="200" />
              <el-input-number v-model="g.sortOrder" class="lg-sort" :min="0" :max="999" :controls="false" />
              <div class="lg-ops">
                <el-button v-if="lineup.length > 1" text type="danger" @click="removeLineup(idx)">删除</el-button>
                <span v-else class="min-tip">至少1位</span>
              </div>
            </div>
            <div class="lineup-actions">
              <el-button type="primary" plain size="small" @click="addLineup">＋ 添加嘉宾</el-button>
              <span class="lineup-tip">演出阵容将在活动详情页「演出阵容 / 嘉宾」模块按排序展示；姓名必填，其余可空。</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '保存草稿' }}
          </el-button>
          <el-button
            v-if="isEdit && editable"
            type="success"
            :loading="submitting"
            @click="handleSubmitReview"
          >提交审核</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCategoryList as getCategories } from '@/api/category'
import { getEnabledTags as getTags } from '@/api/tag'
import { addActivity, updateActivity, applyActivity, getOperatorActivityDetail, getOperatorActivityTiers, saveActivityTiers, getActivityLineup, saveActivityLineup } from '@/api/operator'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const fileInput = ref()
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)
const form = reactive({
  id: null,
  title: '',
  type: '',
  categoryId: null,
  tagIds: [],
  venue: '',
  startTime: '',
  grabStartTime: '',
  grabEndTime: '',
  purchaseLimit: 4,
  duration: 120,
  imageUrl: '',
  description: '',
  ticketNotice: '',
  refundPolicy: 1,
  realNameMode: 0,
  seatMapEnabled: 0,
  view3dEnabled: 0,
  view3dModel: 'arena_bowl',
  totalTickets: 0,
  price: null,
  status: 0,
  tiers: [{ seatType: '', price: null, totalStock: 100 }],
})

const activityTypes = ['演唱会', '音乐会', '话剧歌剧', '舞蹈芭蕾', '曲苑杂坛', '体育赛事', '亲子儿童', '展览休闲']

// 3D 场馆模型选项：不同活动类型对应不同场馆外观
const venueModelOptions = [
  { value: 'arena_bowl', label: '碗形综合馆', desc: '演唱会 / 电竞综合' },
  { value: 'theatre', label: '镜框式剧院', desc: '话剧 / 歌剧 / 芭蕾' },
  { value: 'stadium', label: '标准体育场', desc: '足球 / 田径赛事' },
  { value: 'concert', label: '露天音乐场', desc: '大型户外演唱会' }
]
// 活动类型 → 推荐场馆模型（仅当用户未手动修改时自动套用）
const typeToModel = {
  '演唱会': 'arena_bowl',
  '音乐会': 'arena_bowl',
  '话剧歌剧': 'theatre',
  '舞蹈芭蕾': 'theatre',
  '曲苑杂坛': 'theatre',
  '体育赛事': 'stadium',
  '亲子儿童': 'arena_bowl',
  '展览休闲': 'concert'
}
// 用户是否手动改动过场馆模型（手动改过后不再被类型联动覆盖）
const modelTouched = ref(false)
const suggestModel = () => {
  if (modelTouched.value) return
  form.view3dModel = typeToModel[form.type] || 'arena_bowl'
}
// 开启 3D 时若未选模型则按当前活动类型推荐；切换活动类型时也联动推荐
watch(() => form.view3dEnabled, (v) => { if (v === 1) suggestModel() })
watch(() => form.type, () => suggestModel())

const categoryOptions = ref([])
const tagOptions = ref([])
const originalStatus = ref(null)

// 演出阵容 / 嘉宾（运营后台可配置，详情页展示）
const lineup = ref([defaultLineupRow()])
function defaultLineupRow() {
  return { name: '', role: '', avatar: '', description: '', sortOrder: null }
}
function addLineup() {
  lineup.value.push({ ...defaultLineupRow(), sortOrder: lineup.value.length + 1 })
}
function removeLineup(index) {
  if (lineup.value.length <= 1) return
  lineup.value.splice(index, 1)
}
function buildLineupPayload() {
  return lineup.value
    .filter((g) => (g.name || '').trim())
    .map((g) => ({
      name: (g.name || '').trim(),
      role: (g.role || '').trim(),
      avatar: (g.avatar || '').trim(),
      description: (g.description || '').trim(),
      sortOrder: g.sortOrder != null ? Number(g.sortOrder) : null,
    }))
}
// 阵容保存失败不应阻断活动主流程，单独容错
async function saveLineupSafe(id) {
  try {
    await saveActivityLineup(id, { lineup: buildLineupPayload() })
  } catch (e) {
    ElMessage.warning('活动已保存，但演出阵容保存失败，可稍后在编辑页重试')
  }
}
async function loadLineup(id) {
  try {
    const res = await getActivityLineup(id)
    const list = res.data || []
    lineup.value = list.length
      ? list.map((g) => ({
          name: g.name || '',
          role: g.role || '',
          avatar: g.avatar || '',
          description: g.description || '',
          sortOrder: g.sortOrder != null ? Number(g.sortOrder) : null,
        }))
      : [defaultLineupRow()]
  } catch (e) {
    lineup.value = [defaultLineupRow()]
  }
}

// 座位平面图分区编辑（仅开启场馆地图时生效；最终会编码为 seatLayout JSON）
const seatSections = ref([{ id: 'A', name: 'A区', rows: 10, cols: 20, price: null }])
function defaultSeatSections() {
  return [{ id: 'A', name: 'A区', rows: 10, cols: 20, price: null }]
}
function addSeatSection() {
  const next = String.fromCharCode(65 + seatSections.value.length) // A, B, C...
  seatSections.value.push({ id: next, name: next + '区', rows: 10, cols: 20, price: null })
}
function removeSeatSection(index) {
  if (seatSections.value.length <= 1) return
  seatSections.value.splice(index, 1)
}
const seatTotalSeats = computed(() =>
  seatSections.value.reduce((s, sec) => s + (Number(sec.rows) || 0) * (Number(sec.cols) || 0), 0)
)
function buildSeatLayout() {
  if (form.seatMapEnabled !== 1) return null
  return JSON.stringify({
    sections: seatSections.value.map((s) => ({
      id: String(s.id || '').trim(),
      name: String(s.name || s.id || '').trim(),
      rows: Number(s.rows) || 0,
      cols: Number(s.cols) || 0,
      price: s.price != null ? Number(s.price) : null,
    })),
  })
}
function validateSeatLayout() {
  if (form.seatMapEnabled !== 1) return true
  const secs = seatSections.value
  if (!secs.length) {
    ElMessage.warning('请至少配置一个座位分区')
    return false
  }
  const ids = new Set()
  for (const sec of secs) {
    if (!sec.id || !String(sec.id).trim()) {
      ElMessage.warning('请填写分区区号（如 A）')
      return false
    }
    if (ids.has(sec.id)) {
      ElMessage.warning(`分区区号「${sec.id}」重复`)
      return false
    }
    ids.add(sec.id)
    if (!(Number(sec.rows) >= 1)) {
      ElMessage.warning(`分区「${sec.id}」排数必须大于 0`)
      return false
    }
    if (!(Number(sec.cols) >= 1)) {
      ElMessage.warning(`分区「${sec.id}」每排座位数必须大于 0`)
      return false
    }
  }
  return true
}

const statusMap = {
  '-1': { text: '草稿', type: 'info' },
  0: { text: '待审核', type: 'warning' },
  1: { text: '已上架（待开售）', type: 'primary' },
  2: { text: '已驳回', type: 'danger' },
  3: { text: '已开售/进行中', type: 'success' },
  4: { text: '已下架', type: 'info' },
}

const statusTag = computed(() => statusMap[originalStatus.value] || statusMap[0])
// 草稿(-1)/待审核(0)/被驳回(2) 可编辑；已提交或上线后仅可查看
const editable = computed(() => [-1, 0, 2].includes(originalStatus.value))

const tierTotal = computed(() => form.tiers.reduce((s, t) => s + (Number(t.totalStock) || 0), 0))
const tierMinPrice = computed(() => {
  const prices = form.tiers.map((t) => Number(t.price)).filter((p) => p > 0)
  return prices.length ? Math.min(...prices) : null
})

function addTier() {
  form.tiers.push({ seatType: '', price: null, totalStock: 100 })
}

function removeTier(index) {
  if (form.tiers.length <= 1) return
  form.tiers.splice(index, 1)
}

async function loadCategoriesAndTags() {
  try {
    const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
    categoryOptions.value = catRes.data || []
    tagOptions.value = tagRes.data || []
  } catch (e) {
    /* 忽略下拉加载错误 */
  }
}

async function loadActivityDetail() {
  const id = route.params.id
  const res = await getOperatorActivityDetail(id)
  const detail = res.data
  originalStatus.value = detail.status
  Object.assign(form, {
    id: detail.id,
    title: detail.title || '',
    type: detail.type || '',
    categoryId: detail.categoryId ?? null,
    tagIds: (detail.tags || []).map((t) => t.id),
    venue: detail.venue || '',
    startTime: detail.startTime || '',
    grabStartTime: detail.grabStartTime || '',
    grabEndTime: detail.grabEndTime || '',
    purchaseLimit: detail.purchaseLimit != null ? Number(detail.purchaseLimit) : 4,
    duration: detail.duration || 120,
    imageUrl: detail.imageUrl || '',
    description: detail.description || '',
    ticketNotice: detail.ticketNotice || '',
    refundPolicy: detail.refundPolicy != null ? Number(detail.refundPolicy) : 1,
    realNameMode: detail.realNameMode != null ? Number(detail.realNameMode) : 0,
    seatMapEnabled: detail.seatMapEnabled != null ? Number(detail.seatMapEnabled) : 0,
    view3dEnabled: detail.view3dEnabled != null ? Number(detail.view3dEnabled) : 0,
    view3dModel: detail.view3dModel || 'arena_bowl',
    totalTickets: detail.totalTickets || 0,
    price: detail.price != null ? Number(detail.price) : null,
    status: detail.status,
  })
  // 编辑态下已有保存值，标记为用户已选定，避免类型联动覆盖运营原本的配置
  modelTouched.value = true
  await loadTiers(id, detail)
  // 加载演出阵容（运营可配置）
  await loadLineup(id)
  // 解析座位平面图（开启场馆地图时有值）
  if (detail.seatLayout) {
    try {
      const layout = typeof detail.seatLayout === 'string' ? JSON.parse(detail.seatLayout) : detail.seatLayout
      const secs = (layout.sections || []).map((s) => ({
        id: s.id,
        name: s.name || s.id,
        rows: s.rows,
        cols: s.cols,
        price: s.price != null ? Number(s.price) : null,
      }))
      seatSections.value = secs.length ? secs : defaultSeatSections()
    } catch {
      seatSections.value = defaultSeatSections()
    }
  } else {
    seatSections.value = defaultSeatSections()
  }
}

async function loadTiers(activityId, detail) {
  try {
    const tierRes = await getOperatorActivityTiers(activityId)
    const list = tierRes.data || []
    form.tiers =
      list.length > 0
        ? list.map((t) => ({
            id: t.id,
            seatType: t.seatType || '',
            price: t.price != null ? Number(t.price) : null,
            totalStock: t.totalStock != null ? Number(t.totalStock) : null,
          }))
        : [
            {
              seatType: '',
              price: detail.price != null ? Number(detail.price) : null,
              totalStock: detail.totalTickets > 0 ? Number(detail.totalTickets) : 100,
            },
          ]
  } catch (e) {
    form.tiers = [
      {
        seatType: '',
        price: detail.price != null ? Number(detail.price) : null,
        totalStock: detail.totalTickets > 0 ? Number(detail.totalTickets) : 100,
      },
    ]
  }
}

function validateTiers() {
  const rows = form.tiers
  if (!rows.length) {
    ElMessage.warning('请至少配置一个票档')
    return false
  }
  for (const row of rows) {
    if (!row.seatType || !String(row.seatType).trim()) {
      ElMessage.warning('请填写完整的票档名称（座位类型）')
      return false
    }
    if (!(Number(row.price) > 0)) {
      ElMessage.warning(`请为票档【${row.seatType}】填写大于 0 的票价`)
      return false
    }
    if (!(Number(row.totalStock) >= 1)) {
      ElMessage.warning(`请为票档【${row.seatType}】填写可售库存`)
      return false
    }
  }
  return true
}

function buildTierPayload() {
  return form.tiers.map((t) => ({
    id: t.id || null,
    seatType: String(t.seatType).trim(),
    price: Number(t.price),
    totalStock: Number(t.totalStock),
  }))
}

function buildPayload() {
  // 与票档保持同步：总票数=各档合计、票价=最低档（后端保存票档时也会再校准一次）
  form.totalTickets = tierTotal.value
  form.price = tierMinPrice.value

  return {
    title: form.title.trim(),
    type: form.type,
    categoryId: form.categoryId,
    tagIds: form.tagIds || [],
    venue: form.venue.trim(),
    startTime: form.startTime,
    grabStartTime: form.grabStartTime,
    grabEndTime: form.grabEndTime || '',
    purchaseLimit: Number(form.purchaseLimit) || 1,
    duration: form.duration || 120,
    imageUrl: form.imageUrl || '',
    description: form.description || '',
    ticketNotice: form.ticketNotice || '',
    refundPolicy: Number(form.refundPolicy ?? 1),
    realNameMode: Number(form.realNameMode ?? 0),
    seatMapEnabled: Number(form.seatMapEnabled ?? 0),
    view3dEnabled: Number(form.view3dEnabled ?? 0),
    view3dModel: form.view3dModel || 'arena_bowl',
    seatLayout: buildSeatLayout(),
    totalTickets: form.totalTickets,
    price: form.price,
    status: form.status,
  }
}

async function handleSubmit() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入活动标题')
    return
  }
  if (!form.type) {
    ElMessage.warning('请选择活动类型')
    return
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择活动分类')
    return
  }
  if (!form.venue.trim()) {
    ElMessage.warning('请输入活动地点')
    return
  }
  if (!form.startTime) {
    ElMessage.warning('请选择演出时间')
    return
  }
  if (!form.grabStartTime) {
    ElMessage.warning('请选择开抢时间')
    return
  }
  if (new Date(form.grabStartTime) >= new Date(form.startTime)) {
    ElMessage.warning('开抢时间必须早于演出开始时间')
    return
  }
  if (!form.grabEndTime) {
    ElMessage.warning('请选择抢票结束时间')
    return
  }
  if (new Date(form.grabEndTime) <= new Date(form.grabStartTime)) {
    ElMessage.warning('抢票结束时间必须晚于开抢时间')
    return
  }
  if (new Date(form.grabEndTime) > new Date(form.startTime)) {
    ElMessage.warning('抢票结束时间不能晚于演出开始时间')
    return
  }
  if (!(Number(form.purchaseLimit) >= 1)) {
    ElMessage.warning('请设置合理的限购数量（至少 1 张）')
    return
  }
  if (!validateTiers()) return
  if (!validateSeatLayout()) return

  const payload = buildPayload()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateActivity(form.id, payload)
      await saveActivityTiers(form.id, { tiers: buildTierPayload() })
      await saveLineupSafe(form.id)
      ElMessage.success('活动与票档已保存')
    } else {
      const res = await addActivity(payload)
      const newId = res && res.data ? res.data : null
      if (newId) {
        await saveActivityTiers(newId, { tiers: buildTierPayload() })
        await saveLineupSafe(newId)
      }
      ElMessage.success('草稿已保存，请在活动列表中提交审核')
    }
    goBack()
  } catch (e) {
    /* 错误信息由请求拦截器统一提示 */
  } finally {
    submitting.value = false
  }
}

// 提交审核：先保存最新编辑，再调用 apply 将状态流转为“待审核(0)”
async function handleSubmitReview() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入活动标题')
    return
  }
  if (!form.type) {
    ElMessage.warning('请选择活动类型')
    return
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择活动分类')
    return
  }
  if (!form.venue.trim()) {
    ElMessage.warning('请输入活动地点')
    return
  }
  if (!form.startTime) {
    ElMessage.warning('请选择演出时间')
    return
  }
  if (!form.grabStartTime) {
    ElMessage.warning('请选择开抢时间')
    return
  }
  if (new Date(form.grabStartTime) >= new Date(form.startTime)) {
    ElMessage.warning('开抢时间必须早于演出开始时间')
    return
  }
  if (!form.grabEndTime) {
    ElMessage.warning('请选择抢票结束时间')
    return
  }
  if (new Date(form.grabEndTime) <= new Date(form.grabStartTime)) {
    ElMessage.warning('抢票结束时间必须晚于开抢时间')
    return
  }
  if (new Date(form.grabEndTime) > new Date(form.startTime)) {
    ElMessage.warning('抢票结束时间不能晚于演出开始时间')
    return
  }
  if (!(Number(form.purchaseLimit) >= 1)) {
    ElMessage.warning('请设置合理的限购数量（至少 1 张）')
    return
  }
  if (!validateTiers()) return
  if (!validateSeatLayout()) return

  const payload = buildPayload()
  submitting.value = true
  try {
    await updateActivity(form.id, payload)
    await saveActivityTiers(form.id, { tiers: buildTierPayload() })
    await saveLineupSafe(form.id)
    await applyActivity(form.id)
    ElMessage.success('已提交审核，等待管理员审核')
    goBack()
  } catch (e) {
    /* 错误信息由请求拦截器统一提示 */
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleUpload(event) {
  const file = event.target.files?.[0]
  if (!file) return
  // 放宽校验：所有 image/* 类型均允许；个别环境 file.type 为空时按扩展名兜底
  const isImage = file.type
    ? file.type.startsWith('image/')
    : /\.(jpe?g|png|gif|webp|bmp|avif|svg)$/i.test(file.name || '')
  if (!isImage) {
    ElMessage.warning(`仅支持上传图片文件（当前类型：${file.type || file.name || '未知'}）`)
    event.target.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    event.target.value = ''
    return
  }
  const fd = new FormData()
  fd.append('file', file)
  try {
    const { uploadImage } = await import('@/api/upload')
    const res = await uploadImage(fd)
    // 上传接口返回的是字符串 URL（Result.data 直接是 "/uploads/xxx.jpg"），兼容对象{url}写法
    const url = typeof res.data === 'string' ? res.data : (res.data && res.data.url)
    form.imageUrl = url || ''
    ElMessage.success('封面上传成功')
  } catch (e) {
    /* 错误提示统一处理 */
  } finally {
    event.target.value = ''
  }
}

onMounted(async () => {
  await loadCategoriesAndTags()
  if (isEdit.value) {
    await loadActivityDetail()
  }
})
</script>

<style scoped>
.page-container {
  padding: 8px 4px;
}
.form-card {
  max-width: 920px;
  margin-top: 18px;
}
.form-status {
  margin-left: 12px;
  font-size: 13px;
  color: #666;
}
.unit {
  margin-left: 8px;
  color: #909399;
}

.cfg-tip {
  width: 100%;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}

.cover-upload {
  width: 200px;
  height: 120px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}
.cover-upload:hover {
  border-color: var(--el-color-primary);
}
.cover-placeholder-icon {
  font-size: 26px;
  color: #c0c4cc;
}
.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
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
.th-seat {
  width: 300px;
}
.th-price {
  width: 140px;
}
.th-stock {
  width: 140px;
}
.th-ops {
  flex: 1;
}
.tier-row {
  margin-bottom: 10px;
}
.td-seat {
  width: 300px;
}
.td-price {
  width: 140px;
}
.td-stock {
  width: 140px;
}
.td-ops {
  flex: 1;
  display: flex;
  align-items: center;
}
.min-tip {
  font-size: 12px;
  color: #c0c4cc;
}
.tier-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 2px;
}
.tier-summary {
  font-size: 13px;
  color: #606266;
}
.tier-summary b {
  color: var(--el-color-primary);
}
.tier-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

/* 座位平面图编辑器 */
.seat-layout-editor {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafbfc;
}
.seat-section-head,
.seat-section-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.seat-section-head {
  font-size: 12px;
  color: #909399;
}
.slh-id { width: 80px; }
.slh-name { width: 160px; }
.slh-rows { width: 110px; }
.slh-cols { width: 130px; }
.slh-price { width: 140px; }
.slh-ops { flex: 1; }
.sl-id { width: 80px; }
.sl-name { width: 160px; }
.sl-rows { width: 110px; }
.sl-cols { width: 130px; }
.sl-price { width: 140px; }
.sl-ops {
  display: flex;
  align-items: center;
}
.seat-section-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 4px;
}
.seat-summary {
  font-size: 13px;
  color: #606266;
}
.seat-summary b {
  color: var(--el-color-primary);
}
.seat-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

/* 演出阵容编辑器 */
.lineup-editor {
  width: 100%;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  background: #fafbfc;
}
.lineup-head,
.lineup-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.lineup-head {
  font-size: 12px;
  color: #909399;
}
.lh-name { width: 160px; }
.lh-role { width: 120px; }
.lh-avatar { width: 220px; }
.lh-desc { flex: 1; min-width: 120px; }
.lh-sort { width: 90px; }
.lh-ops { width: 70px; }
.lg-name { width: 160px; }
.lg-role { width: 120px; }
.lg-avatar { width: 220px; }
.lg-desc { flex: 1; min-width: 120px; }
.lg-sort { width: 90px; }
.lg-ops {
  display: flex;
  align-items: center;
}
.lineup-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 4px;
}
.lineup-tip {
  font-size: 12px;
  color: #909399;
}
</style>

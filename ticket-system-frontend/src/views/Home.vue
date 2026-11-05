<template>
  <div class="home-page">
    <!-- 轮播图 -->
    <div class="banner-section" v-if="bannerList.length > 0">
      <div class="banner-container">
        <div 
          class="banner-track"
          :style="{ transform: `translateX(-${currentBanner * 100}%)` }"
        >
          <div 
            v-for="(item, index) in bannerList" 
            :key="index"
            class="banner-item"
            :style="{ backgroundImage: 'url(' + item.imageUrl + ')' }"
            @click="goActivity(item.activityId)"
          >
            <div class="banner-content">
              <div class="banner-title">{{ item.title || '精彩活动' }}</div>
              <div class="banner-desc">{{ item.description || '探索更多精彩' }}</div>
              <el-button v-if="item.activityId" type="primary" class="banner-btn" @click.stop="goActivity(item.activityId)">立即购票</el-button>
            </div>
          </div>
        </div>
        <div class="banner-indicators">
          <span 
            v-for="(item, index) in bannerList" 
            :key="index"
            class="indicator"
            :class="{ active: currentBanner === index }"
            @click="currentBanner = index"
          ></span>
        </div>
        <button class="banner-prev" @click="prevBanner"><el-icon><ArrowLeft /></el-icon></button>
        <button class="banner-next" @click="nextBanner"><el-icon><ArrowRight /></el-icon></button>
      </div>
    </div>

    <!-- 热门搜索 -->
    <div class="hot-search-section section">
      <div class="hot-search-card">
        <span class="hot-search-icon">🔥</span>
        <span class="hot-search-label">热门搜索：</span>
        <span
          v-for="kw in hotKeywords"
          :key="kw"
          class="hot-keyword"
          @click="goSearch(kw)"
        >{{ kw }}</span>
        <el-button text class="hot-more" @click="$router.push('/activity/list')">全部演出 →</el-button>
      </div>
    </div>

    <!-- 分类快捷导航 -->
    <div class="category-nav section">
      <div class="category-grid">
        <div
          v-for="cat in categoryList"
          :key="cat.id"
          class="category-item"
          @click="goCategory(cat.id)"
        >
          <div class="category-icon" :style="{ background: cat.color || categoryBg(cat.id) }">{{ categoryEmoji(cat.id) }}</div>
          <span class="category-name">{{ cat.name }}</span>
        </div>
      </div>
      <div class="tag-nav" v-if="tagList.length > 0">
        <span
          v-for="tag in tagList"
          :key="tag.id"
          class="tag-pill"
          :style="{ borderColor: tag.color, color: tag.color }"
          @click="goTag(tag.id)"
        >{{ tag.name }}</span>
      </div>
    </div>

    <!-- 平台特色服务 -->
    <div class="section feature-bar">
      <div class="feature-item"><span class="feature-ico">✅</span><div class="feature-meta"><b>官方授权</b><i>正规票源 · 假一赔十</i></div></div>
      <div class="feature-item"><span class="feature-ico">🎫</span><div class="feature-meta"><b>购票快速</b><i>实时选座 · 秒速出票</i></div></div>
      <div class="feature-item"><span class="feature-ico">💳</span><div class="feature-meta"><b>安全保障</b><i>实名购票 · 电子核销</i></div></div>
      <div class="feature-item"><span class="feature-ico">🛎️</span><div class="feature-meta"><b>贴心服务</b><i>在线客服 · 全天响应</i></div></div>
    </div>

    <!-- 系统公告（跑马灯） -->
    <div class="section announcement-section" v-if="announcements.length > 0">
      <div class="announcement-bar" @click="$router.push('/announcements')">
        <div class="announcement-horn">📢</div>
        <div class="announcement-notice">公告</div>
        <div class="announcement-marquee">
          <transition name="announce-slide">
            <div :key="currentAnnouncement" class="announcement-marquee-item">
              <el-tag v-if="announcements[currentAnnouncement] && announcements[currentAnnouncement].isTop === 1" type="danger" size="small" effect="dark">置顶</el-tag>
              <span class="announcement-title">{{ announcements[currentAnnouncement] && announcements[currentAnnouncement].title }}</span>
            </div>
          </transition>
        </div>
        <span class="announcement-more">查看全部 →</span>
      </div>
    </div>

    <!-- 主体内容区：活动列表 + 侧边栏广告 -->
    <div class="main-with-sidebar">
      <div class="main-left">
        <!-- 热门推荐 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">🔥 正在热卖</h2>
            <el-button text type="primary" @click="$router.push('/activity/list')">查看更多 →</el-button>
          </div>
          <el-row :gutter="20">
            <el-col
              v-for="item in hotList"
              :key="item.id"
              :xs="24" :sm="12" :md="8" :lg="6"
            >
              <div class="activity-card" @click="goDetail(item.id)">
                <div class="card-cover-wrap">
                  <el-image
                    class="card-cover"
                    :src="item.coverImage"
                    fit="cover"
                    lazy
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <span class="card-status-badge" :class="'status-' + getItemStatus(item)">{{ getStatusText(item) }}</span>
                  <span v-if="hasSeat(item)" class="card-seat-badge">🎫 选座</span>
                </div>
                <div class="card-info">
                  <h3 class="card-title">{{ item.title || item.name }}</h3>
                  <p class="card-location"><el-icon><Location /></el-icon>{{ item.venue || item.location }}</p>
                  <p class="card-time"><el-icon><Clock /></el-icon>{{ formatTime(item.startTime) }}</p>
                  <!-- 大麦风格：已售进度 + 想看热度 -->
                  <div class="card-meta-row" v-if="soldPercentOf(item) !== null && getItemStatus(item) !== 1">
                    <div class="card-progress">
                      <div class="progress-track">
                        <div class="progress-fill" :class="{ hot: soldPercentOf(item) > 80 }" :style="{ width: soldWidthOf(item) + '%' }"></div>
                      </div>
                      <span class="progress-text">已售 {{ soldPercentOf(item) }}%</span>
                    </div>
                    <span v-if="item.wishCount > 0" class="card-wish-tag">❤ {{ item.wishCount }}人想看</span>
                  </div>
                  <div class="card-bottom">
                    <span class="card-price">¥{{ item.price }}<span class="card-price-suffix">起</span></span>
                    <span class="card-remain">剩余 {{ remainOf(item) }} 张</span>
                    <el-button size="small" class="card-buy-btn" :class="{ 'is-muted': cardAction(item).muted }" :disabled="cardAction(item).disabled" @click.stop="goDetail(item.id)">{{ cardAction(item).text }}</el-button>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-empty v-if="hotList.length === 0 && !loading" description="暂无活动" />
        </div>

        <!-- 个性化推荐：登录后「猜你喜欢」（基于想看/收藏/已购偏好），未登录「为你推荐」（热门兜底）。
             始终展示，避免此前「未登录/接口偶发失败就整块消失」的奇怪现象 -->
        <div class="section" v-if="recommendList.length > 0">
          <div class="section-header">
            <h2 class="section-title">{{ recommendTitle }}</h2>
            <span class="reco-tip">{{ recommendTip }}</span>
            <el-button text type="primary" @click="$router.push('/activity/list')">查看更多 →</el-button>
          </div>
          <el-row :gutter="20">
            <el-col
              v-for="item in recommendList"
              :key="'reco-' + item.id"
              :xs="24" :sm="12" :md="8" :lg="6"
            >
              <div class="activity-card" @click="goDetail(item.id)">
                <div class="card-cover-wrap">
                  <el-image
                    class="card-cover"
                    :src="item.coverImage"
                    fit="cover"
                    lazy
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <span class="card-status-badge" :class="'status-' + getItemStatus(item)">{{ getStatusText(item) }}</span>
                  <span v-if="hasSeat(item)" class="card-seat-badge">🎫 选座</span>
                </div>
                <div class="card-info">
                  <h3 class="card-title">{{ item.title || item.name }}</h3>
                  <p class="card-location"><el-icon><Location /></el-icon>{{ item.venue || item.location }}</p>
                  <p class="card-time"><el-icon><Clock /></el-icon>{{ formatTime(item.startTime) }}</p>
                  <!-- 大麦风格：已售进度 + 想看热度 -->
                  <div class="card-meta-row" v-if="soldPercentOf(item) !== null && getItemStatus(item) !== 1">
                    <div class="card-progress">
                      <div class="progress-track">
                        <div class="progress-fill" :class="{ hot: soldPercentOf(item) > 80 }" :style="{ width: soldWidthOf(item) + '%' }"></div>
                      </div>
                      <span class="progress-text">已售 {{ soldPercentOf(item) }}%</span>
                    </div>
                    <span v-if="item.wishCount > 0" class="card-wish-tag">❤ {{ item.wishCount }}人想看</span>
                  </div>
                  <div class="card-bottom">
                    <span class="card-price">¥{{ item.price }}<span class="card-price-suffix">起</span></span>
                    <span class="card-remain">剩余 {{ remainOf(item) }} 张</span>
                    <el-button size="small" class="card-buy-btn" :class="{ 'is-muted': cardAction(item).muted }" :disabled="cardAction(item).disabled" @click.stop="goDetail(item.id)">{{ cardAction(item).text }}</el-button>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 最新活动 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">🆕 最新活动</h2>
            <div class="latest-tabs">
              <span
                v-for="t in latestTabs"
                :key="t.value"
                class="latest-tab"
                :class="{ active: latestTab === t.value }"
                @click="latestTab = t.value"
              >{{ t.label }}</span>
              <el-button text type="primary" @click="$router.push('/activity/list')">查看更多 →</el-button>
            </div>
          </div>
          <!-- 分类快捷 chips（对标大麦：演唱会/体育/展览…）+ 选座专区 -->
          <div class="latest-chips">
            <span
              v-for="c in latestCatChips"
              :key="String(c.value)"
              class="latest-chip"
              :class="{ active: latestCat === c.value }"
              @click="latestCat = c.value"
            >{{ c.emoji ? c.emoji + ' ' : '' }}{{ c.label }}</span>
          </div>
          <el-row :gutter="20">
            <el-col
              v-for="item in latestShown"
              :key="item.id"
              :xs="24" :sm="12" :md="8" :lg="6"
            >
              <div class="activity-card" @click="goDetail(item.id)">
                <div class="card-cover-wrap">
                  <el-image
                    class="card-cover"
                    :src="item.coverImage"
                    fit="cover"
                    lazy
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <span class="card-status-badge" :class="'status-' + getItemStatus(item)">{{ getStatusText(item) }}</span>
                  <span v-if="hasSeat(item)" class="card-seat-badge">🎫 选座</span>
                </div>
                <div class="card-info">
                  <h3 class="card-title">{{ item.title || item.name }}</h3>
                  <p class="card-location"><el-icon><Location /></el-icon>{{ item.venue || item.location }}</p>
                  <p class="card-time"><el-icon><Clock /></el-icon>{{ formatTime(item.startTime) }}</p>
                  <!-- 大麦风格：已售进度 + 想看热度 -->
                  <div class="card-meta-row" v-if="soldPercentOf(item) !== null && getItemStatus(item) !== 1">
                    <div class="card-progress">
                      <div class="progress-track">
                        <div class="progress-fill" :class="{ hot: soldPercentOf(item) > 80 }" :style="{ width: soldWidthOf(item) + '%' }"></div>
                      </div>
                      <span class="progress-text">已售 {{ soldPercentOf(item) }}%</span>
                    </div>
                    <span v-if="item.wishCount > 0" class="card-wish-tag">❤ {{ item.wishCount }}人想看</span>
                  </div>
                  <div class="card-bottom">
                    <span class="card-price">¥{{ item.price }}<span class="card-price-suffix">起</span></span>
                    <span class="card-remain">剩余 {{ remainOf(item) }} 张</span>
                    <el-button size="small" class="card-buy-btn" :class="{ 'is-muted': cardAction(item).muted }" :disabled="cardAction(item).disabled" @click.stop="goDetail(item.id)">{{ cardAction(item).text }}</el-button>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-empty v-if="latestShown.length === 0 && !loading" description="暂无活动" />
        </div>
      </div>

      <!-- 侧边栏：即将开抢 + 热销榜 -->
      <div class="sidebar-widgets">
        <!-- 即将开抢 -->
        <div class="widget-card" v-if="upcomingList.length > 0">
          <div class="widget-header">
            <span class="widget-icon widget-icon-clock">⏰</span>
            <span class="widget-title">即将开抢</span>
          </div>
          <div
            v-for="item in upcomingList"
            :key="'up-' + item.id"
            class="upcoming-item"
            @click="goDetail(item.id)"
          >
            <div class="upcoming-info">
              <div class="upcoming-name">{{ item.title }}</div>
              <div class="upcoming-countdown">{{ item.countdown }}</div>
            </div>
          </div>
        </div>

        <!-- 热销榜 -->
        <div class="widget-card" v-if="rankList.length > 0">
          <div class="widget-header">
            <span class="widget-icon widget-icon-fire">🔥</span>
            <span class="widget-title">热销榜</span>
          </div>
          <div
            v-for="(item, index) in rankList"
            :key="'rank-' + item.id"
            class="rank-item"
            @click="goDetail(item.id)"
          >
            <span class="rank-no" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
            <div class="rank-info">
              <div class="rank-name">{{ item.title }}</div>
              <div class="rank-price">¥{{ item.price }}<span class="rank-heat">{{ item.viewCount }}人想看</span></div>
            </div>
          </div>
        </div>

        <!-- 合作推广（侧边栏广告位） -->
        <div class="widget-card sidebar-ad-card" v-if="sidebarAds.length > 0">
          <div class="widget-header">
            <span class="widget-icon widget-icon-ad">🛍</span>
            <span class="widget-title">合作推广</span>
          </div>
          <div
            v-for="ad in sidebarAds"
            :key="'ad-' + ad.id"
            class="sidebar-ad"
            @click="handleAdClick(ad)"
          >
            <el-image :src="ad.imageUrl" fit="cover" class="sidebar-ad-img" lazy>
              <template #error>
                <div class="ad-image-placeholder"><el-icon><Picture /></el-icon></div>
              </template>
            </el-image>
            <div class="sidebar-ad-info">
              <div class="sidebar-ad-title">{{ ad.title }}</div>
              <span class="ad-badge-mini">推广</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 全部数据为空时的兜底空状态 -->
    <div class="home-empty" v-if="allEmpty">
      <el-empty description="暂无活动内容，去列表页逛逛吧">
        <el-button type="primary" @click="$router.push('/activity/list')">浏览全部活动</el-button>
      </el-empty>
    </div>

    <!-- 弹窗广告 -->
    <el-dialog
      v-model="popupAdVisible"
      width="500px"
      :show-close="false"
      align-center
      class="popup-ad-dialog"
      :close-on-click-modal="true"
      @close="handlePopupClose"
    >
      <div class="popup-ad-close" @click.stop="closePopupAd">
        <el-icon><CloseBold /></el-icon>
      </div>
      <div v-if="popupAd" class="popup-ad-content" @click="handleAdClick(popupAd)">
        <div class="popup-ad-image-wrap">
          <el-image :src="popupAd.imageUrl" fit="cover" class="popup-ad-image" lazy>
            <template #error>
              <div class="ad-image-placeholder popup">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
          <span class="ad-badge">广告</span>
          <!-- 多张轮播指示器 -->
          <div class="popup-ad-dots" v-if="popupAdList.length > 1">
            <span
              v-for="(ad, i) in popupAdList"
              :key="ad.id"
              class="popup-ad-dot"
              :class="{ active: i === currentPopupIndex }"
            ></span>
          </div>
        </div>
        <div class="popup-ad-title">{{ popupAd.title }}</div>
        <el-button type="primary" class="popup-ad-btn">查看详情</el-button>
        <div class="popup-ad-skip" @click.stop="hidePopupToday">今日不再显示</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { CloseBold, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getHotActivities, getActivityList, getHomeBanners, getRecommendActivities } from '@/api/activity'
import { getAnnouncements } from '@/api/announcement'
import { getActiveAds } from '@/api/ad'
import { getCategoryList } from '@/api/category'
import { getEnabledTags } from '@/api/tag'

const router = useRouter()
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 个性化推荐列表：登录后展示「猜你喜欢」（基于想看/收藏/已购偏好），未登录展示「为你推荐」（热门兜底）。
// 后端 /activity/recommend 已在拦截器白名单中，未登录也可访问并自动返回热门活动，故始终请求、始终展示。
const recommendTitle = computed(() => isLoggedIn.value ? '✨ 猜你喜欢' : '🔥 为你推荐')
const recommendTip = computed(() => isLoggedIn.value ? '根据你的想看、收藏与购票偏好推荐' : '登录后推荐更精准')
const recommendList = ref([])
const fetchRecommend = async () => {
  try {
    const res = await getRecommendActivities(8)
    recommendList.value = res.data || []
  } catch {
    // 接口异常时静默：不展示该区块，不影响其余内容
  }
}
const hotList = ref([])
const latestList = ref([])
// 最新活动区块：状态切换 tabs（默认只看"抢票中"，解决"满屏预约中"）
const latestTab = ref('selling')
const latestTabs = [
  { value: 'selling', label: '抢票中' },
  { value: 'upcoming', label: '即将开售' },
  { value: 'all', label: '全部' }
]
// 分类快捷 chips（对标大麦：演唱会/体育/展览…）+ 「选座专区」
const latestCat = ref('all')
const latestCatChips = [
  { value: 'all', label: '全部', emoji: '🎪' },
  { value: 1, label: '演唱会', emoji: '🎤' },
  { value: 2, label: '话剧演出', emoji: '🎭' },
  { value: 4, label: '体育赛事', emoji: '⚽' },
  { value: 3, label: '艺术展览', emoji: '🎨' },
  { value: 5, label: '校园文娱', emoji: '🎓' },
  { value: 'seat', label: '🎫 选座专区', emoji: '' }
]
const loading = ref(false)
const bannerList = ref([])
const currentBanner = ref(0)
const announcements = ref([])
const currentAnnouncement = ref(0)
let announcementTimer = null
// 分类与标签快捷导航
const categoryList = ref([])
const tagList = ref([])
// 侧边栏：即将开抢 + 热销榜
const upcomingList = ref([])
const rankList = ref([])
let countdownTimer = null
// 广告相关（弹窗轮播 + 侧边栏推广位）
const popupAdList = ref([])
const currentPopupIndex = ref(0)
const popupAd = ref(null)
const popupAdVisible = ref(false)
const sidebarAds = ref([])
let bannerTimer = null
let popupTimer = null

// 分类图标映射（按分类ID：1演唱会 2话剧演出 3艺术展览 4体育赛事 5校园文娱）
const categoryEmoji = (id) => {
  const map = { 1: '🎤', 2: '🎭', 3: '🎨', 4: '⚽', 5: '🎓' }
  return map[id] || '🎪'
}

// 分类主题色（数据库 color 字段为空时的渐变兜底）
const categoryBg = (id) => {
  const map = {
    1: 'linear-gradient(135deg, #ff4d6d, #ff7849)',
    2: 'linear-gradient(135deg, #409EFF, #36cfc9)',
    3: 'linear-gradient(135deg, #9254de, #b37feb)',
    4: 'linear-gradient(135deg, #23c343, #95de64)',
    5: 'linear-gradient(135deg, #e6a23c, #f7c948)'
  }
  return map[id] || '#409EFF'
}

const goCategory = (id) => {
  router.push({ path: '/activity/list', query: { categoryId: id } })
}

const goTag = (id) => {
  router.push({ path: '/activity/list', query: { tagId: id } })
}

const startAnnouncementTimer = () => {
  stopAnnouncementTimer()
  announcementTimer = setInterval(() => {
    if (announcements.value.length > 0) {
      currentAnnouncement.value = (currentAnnouncement.value + 1) % announcements.value.length
    }
  }, 4000)
}

const stopAnnouncementTimer = () => {
  if (announcementTimer) {
    clearInterval(announcementTimer)
    announcementTimer = null
  }
}

const fetchCategoryNav = async () => {
  try {
    const [catRes, tagRes] = await Promise.all([getCategoryList(), getEnabledTags()])
    categoryList.value = (catRes?.data || []).filter(c => c.status === 1)
    tagList.value = (tagRes?.data || []).filter(t => t.status === 1)
  } catch {
    // ignore
  }
}

// 热门搜索词（快捷标签，点击直达列表页搜索）
const hotKeywords = ['演唱会', '音乐节', '话剧歌剧', '音乐会', '脱口秀', '体育赛事']

const goSearch = (keyword) => {
  router.push({ path: '/activity/list', query: { keyword } })
}

// 全部数据为空时的兜底空状态
const allEmpty = computed(() =>
  !loading.value &&
  hotList.value.length === 0 &&
  latestList.value.length === 0 &&
  bannerList.value.length === 0
)

const goDetail = (id) => {
  router.push(`/activity/${id}`)
}

const goActivity = (activityId) => {
  if (activityId) {
    router.push(`/activity/${activityId}`)
  }
}

const prevBanner = () => {
  currentBanner.value = (currentBanner.value - 1 + bannerList.value.length) % bannerList.value.length
}

const nextBanner = () => {
  currentBanner.value = (currentBanner.value + 1) % bannerList.value.length
}

const startBannerTimer = () => {
  stopBannerTimer()
  bannerTimer = setInterval(() => {
    nextBanner()
  }, 4000)
}

const stopBannerTimer = () => {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}

const fetchBanners = async () => {
  try {
    const res = await getHomeBanners()
    bannerList.value = res.data || []
    if (bannerList.value.length > 0) {
      startBannerTimer()
    }
  } catch {
    // ignore
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getItemStatus = (item) => {
  if (!item) return 3
  const now = new Date().getTime()
  const grabStart = item.grabStartTime ? new Date(item.grabStartTime).getTime() : NaN
  const grabEnd = item.grabEndTime ? new Date(item.grabEndTime).getTime() : NaN

  if (isNaN(grabStart) || isNaN(grabEnd)) {
    return 3
  }

  if (now < grabStart) return 1
  if (now >= grabStart && now <= grabEnd) return 2
  return 3
}

const getStatusType = (item) => {
  const status = getItemStatus(item)
  const map = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'info' }
  return map[status] ?? 'info'
}

const getStatusText = (item) => {
  const status = getItemStatus(item)
  const map = { 0: '未开启', 1: '即将开售', 2: '抢票中', 3: '已结束' }
  return map[status] ?? '未知'
}

// 是否开通在线选座
const hasSeat = (item) => Number(item?.seatMapEnabled ?? 0) === 1

// 卡片主按钮随抢票状态变化：抢票中=立即购票；即将开售=即将开售(置灰)；已结束=已结束(禁用)
const cardAction = (item) => {
  const s = getItemStatus(item)
  if (s === 2) return { text: '立即购票', muted: false, disabled: false }
  if (s === 1) return { text: '即将开售', muted: true, disabled: false }
  return { text: '已结束', muted: true, disabled: true }
}

// 已售百分比（大麦风格：卡片展示进度条）。<10% 保留 1 位小数，其余取整
const soldPercentOf = (item) => {
  const total = Number(item?.totalTickets)
  const sold = Number(item?.soldTickets)
  if (!total || isNaN(total) || isNaN(sold) || sold < 0) return null
  const p = Math.min(100, Math.max(0, (sold / total) * 100))
  return p < 10 ? Number(p.toFixed(1)) : Math.round(p)
}

// 进度条宽度：非零销量至少给 4% 可见宽度，避免“看起来是空的”
const soldWidthOf = (item) => {
  const p = soldPercentOf(item)
  if (p == null) return 0
  return p > 0 ? Math.max(p, 4) : 0
}

// 剩余票数（优先用后端计算的 remainTickets，兜底用 total-sold）
const remainOf = (item) => {
  if (item?.remainTickets != null) return item.remainTickets
  const t = Number(item?.totalTickets || 0)
  const s = Number(item?.soldTickets || 0)
  return Math.max(0, t - s)
}

// 最新活动：按状态 tab + 分类/选座 过滤后取前 12 条
const latestShown = computed(() => {
  const all = latestList.value || []
  let filtered = all
  if (latestTab.value !== 'all') {
    filtered = filtered.filter(it => (latestTab.value === 'selling' ? getItemStatus(it) === 2 : getItemStatus(it) === 1))
  }
  if (latestCat.value === 'seat') {
    filtered = filtered.filter(it => Number(it.seatMapEnabled ?? 0) === 1)
  } else if (latestCat.value !== 'all') {
    filtered = filtered.filter(it => Number(it.categoryId) === Number(latestCat.value))
  }
  return filtered.slice(0, 12)
})

const fetchHotActivities = async () => {
  try {
    const res = await getHotActivities()
    // 热门横向轮播，展示前8个热门活动（填满 2 行 × 4 列网格，避免右侧空列）
    hotList.value = (res.data || []).slice(0, 8)
  } catch {
    // ignore
  }
}

const fetchLatestActivities = async () => {
  loading.value = true
  try {
    const res = await getActivityList({ page: 1, size: 48 })
    latestList.value = res.data?.records || res.data?.list || []
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

const fetchAnnouncements = async () => {
  try {
    const res = await getAnnouncements({ page: 1, size: 5 })
    const data = res.data || res
    const list = data.records || data.list || []
    // 置顶的排前面
    announcements.value = list.sort((a, b) => (b.isTop || 0) - (a.isTop || 0))
    if (announcements.value.length > 1) {
      startAnnouncementTimer()
    }
  } catch {
    // ignore
  }
}

// 弹窗广告：进首页时轮播展示，点掉当天不再出现
const fetchAds = async () => {
  try {
    const sidebarRes = await getActiveAds('sidebar')
    sidebarAds.value = (sidebarRes?.data || []).slice(0, 3)
    const popupRes = await getActiveAds('popup')
    popupAdList.value = popupRes?.data || []
    if (popupAdList.value.length > 0) {
      const lastShown = localStorage.getItem('popupAdLastShown')
      const today = new Date().toDateString()
      if (lastShown !== today) {
        showPopupAt(0)
        startPopupRotation()
        // 记录今天已展示过；关闭后当天不再打扰
        localStorage.setItem('popupAdLastShown', today)
      }
    }
  } catch {
    // 静默处理
  }
}

const showPopupAt = (index) => {
  if (popupAdList.value.length === 0) return
  currentPopupIndex.value = index
  popupAd.value = popupAdList.value[index]
  popupAdVisible.value = true
}

const startPopupRotation = () => {
  stopPopupRotation()
  if (popupAdList.value.length < 2) return
  popupTimer = setInterval(() => {
    showPopupAt((currentPopupIndex.value + 1) % popupAdList.value.length)
  }, 4000)
}

const stopPopupRotation = () => {
  if (popupTimer) {
    clearInterval(popupTimer)
    popupTimer = null
  }
}

// 广告点击跳转
const handleAdClick = (ad) => {
  if (ad.activityId) {
    router.push(`/activity/${ad.activityId}`)
  } else if (ad.linkUrl) {
    window.open(ad.linkUrl, '_blank')
  }
}

const closePopupAd = () => {
  popupAdVisible.value = false
}

const handlePopupClose = () => {
  popupAdVisible.value = false
  stopPopupRotation()
}

// 今日不再显示：记到明天零点前都不再弹
const hidePopupToday = () => {
  popupAdVisible.value = false
  stopPopupRotation()
  localStorage.setItem('popupAdLastShown', new Date().toDateString())
}

// 侧边栏数据：即将开抢（开售时间在未来） + 热销榜（按热度）
const fetchSidebarWidgets = async () => {
  try {
    const [upcomingRes, rankRes] = await Promise.all([
      getActivityList({ page: 1, size: 20, sortField: 'grabStartTime', sortOrder: 'asc' }),
      getActivityList({ page: 1, size: 5, sortField: 'hot' })
    ])
    const now = Date.now()
    // 即将开抢：开售时间在未来的前 3 场
    upcomingList.value = ((upcomingRes?.data?.records) || [])
      .filter(item => {
        const t = item.grabStartTime ? new Date(item.grabStartTime).getTime() : NaN
        return !isNaN(t) && t > now
      })
      .slice(0, 3)
      .map(item => ({ ...item, countdown: '' }))
    startCountdown()
    // 热销榜：热度前 5
    rankList.value = ((rankRes?.data?.records) || []).slice(0, 5)
  } catch {
    // 静默处理
  }
}

// 即将开抢倒计时（每秒刷新）
const startCountdown = () => {
  stopCountdown()
  if (upcomingList.value.length === 0) return
  countdownTimer = setInterval(() => {
    const now = Date.now()
    let allExpired = true
    upcomingList.value.forEach(item => {
      const target = item.grabStartTime ? new Date(item.grabStartTime).getTime() : 0
      const diff = target - now
      if (diff > 0) {
        allExpired = false
        const d = Math.floor(diff / 86400000)
        const h = Math.floor((diff % 86400000) / 3600000)
        const m = Math.floor((diff % 3600000) / 60000)
        const s = Math.floor((diff % 60000) / 1000)
        item.countdown = d > 0
          ? `${d}天 ${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
          : `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      } else {
        item.countdown = '开抢中'
      }
    })
    if (allExpired) {
      upcomingList.value = upcomingList.value.filter(
        item => new Date(item.grabStartTime).getTime() > now
      )
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

onMounted(() => {
  fetchHotActivities()
  fetchLatestActivities()
  fetchRecommend()
  fetchBanners()
  fetchAnnouncements()
  fetchAds()
  fetchCategoryNav()
  fetchSidebarWidgets()
})

onUnmounted(() => {
  stopBannerTimer()
  stopAnnouncementTimer()
  stopPopupRotation()
  stopCountdown()
})
</script>

<style scoped>
.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 44px;
  /* 细腻的顶部氛围渐变，呼应品牌蓝 */
  background:
    radial-gradient(1000px 260px at 50% -40px, rgba(64, 158, 255, 0.08), transparent 70%),
    radial-gradient(600px 200px at 8% 0, rgba(255, 77, 109, 0.05), transparent 65%);
}

/* 轮播图 */
.banner-section {
  margin-bottom: 24px;
  padding: 0 20px;
}

.banner-container {
  position: relative;
  width: 100%;
  height: 400px;
  overflow: hidden;
  border-radius: 20px;
  box-shadow: 0 18px 48px rgba(31, 92, 170, 0.22);
}

.banner-track {
  display: flex;
  height: 100%;
  transition: transform 0.5s ease;
}

.banner-item {
  flex-shrink: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 64px;
  cursor: pointer;
  position: relative;
}

.banner-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(12, 18, 40, 0.72) 0%, rgba(12, 18, 40, 0.28) 55%, rgba(12, 18, 40, 0) 100%);
}

.banner-item::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 100% at 100% 100%, rgba(255, 77, 109, 0.12), transparent 55%);
  pointer-events: none;
}

.banner-content {
  position: relative;
  z-index: 1;
  color: #fff;
  max-width: 520px;
}

.banner-title {
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 1px;
  margin-bottom: 14px;
  text-shadow: 0 3px 12px rgba(0,0,0,0.35);
}

.banner-desc {
  font-size: 16px;
  margin-bottom: 26px;
  opacity: 0.92;
  line-height: 1.7;
}

.banner-btn {
  font-size: 15px;
  padding: 12px 32px;
  border-radius: 25px;
  border: none;
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  box-shadow: 0 10px 26px rgba(255, 77, 109, 0.45);
  letter-spacing: 1px;
}

.banner-btn:hover {
  opacity: 0.92;
  transform: translateY(-2px);
}

.banner-indicators {
  position: absolute;
  bottom: 22px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 2;
}

.indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.45);
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator.active {
  width: 26px;
  border-radius: 5px;
  background: linear-gradient(90deg, #ff7a45, #ff4d6d);
}

.banner-prev,
.banner-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(6px);
  border: none;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.banner-prev:hover,
.banner-next:hover {
  background: rgba(255, 255, 255, 0.3);
}

.banner-prev {
  left: 18px;
}

.banner-next {
  right: 18px;
}

/* 热门搜索条 */
.hot-search-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  background: #fff;
  border-radius: 14px;
  padding: 14px 20px;
  box-shadow: 0 3px 14px rgba(31, 92, 170, 0.07);
  border: 1px solid #eef1f6;
}

.hot-search-icon {
  font-size: 16px;
  line-height: 1;
  margin-right: 2px;
}

.hot-search-label {
  font-size: 13px;
  color: #909399;
  font-weight: 600;
  flex-shrink: 0;
}

.hot-keyword {
  padding: 5px 15px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f5f7fa, #eef2f7);
  border: 1px solid #e8ebf0;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.22s ease;
}

.hot-keyword:hover {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border-color: transparent;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 77, 109, 0.3);
}

.hot-more {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

/* 通用区域 */
.section {
  padding: 0 20px;
  margin-bottom: 32px;
}

/* 公告区域（跑马灯样式） */
.announcement-section {
  background: #fff;
  border-radius: 14px;
  padding: 14px 20px;
  box-shadow: 0 3px 14px rgba(31, 92, 170, 0.07);
  border: 1px solid #eef1f6;
}

.announcement-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.announcement-horn {
  font-size: 18px;
  line-height: 1;
}

.announcement-notice {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 600;
  color: #409EFF;
  padding: 2px 10px;
  background: #ecf5ff;
  border-radius: 4px;
}

.announcement-marquee {
  flex: 1;
  height: 24px;
  overflow: hidden;
  position: relative;
}

.announcement-marquee-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 24px;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.announcement-more {
  flex-shrink: 0;
  font-size: 13px;
  color: #909399;
}

.announcement-bar:hover .announcement-more {
  color: #409EFF;
}

/* 跑马灯切换动画 */
.announce-slide-enter-active,
.announce-slide-leave-active {
  transition: all 0.4s ease;
}

.announce-slide-enter-from {
  transform: translateY(24px);
  opacity: 0;
}

.announce-slide-leave-to {
  transform: translateY(-24px);
  opacity: 0;
}

/* 分类快捷导航 */
.category-nav {
  background: #fff;
  border-radius: 14px;
  padding: 24px 24px 20px;
  box-shadow: 0 3px 14px rgba(31, 92, 170, 0.07);
  border: 1px solid #eef1f6;
}

.category-grid {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex: 1;
  transition: transform 0.2s ease;
}

.category-item:hover {
  transform: translateY(-4px);
}

.category-icon {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 27px;
  color: #fff;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.14);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.category-item:hover .category-icon {
  transform: scale(1.08) rotate(-4deg);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.2);
}

.category-name {
  font-size: 13px;
  color: #303133;
  font-weight: 600;
  transition: color 0.2s ease;
}

.category-item:hover .category-name {
  color: #409eff;
}

/* 标签快捷入口 */
.tag-nav {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;
  flex-wrap: wrap;
}

.tag-pill {
  padding: 5px 16px;
  border: 1px solid;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: #fff;
  transition: all 0.22s ease;
}

.tag-pill:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.1);
}

/* 卡片封面状态角标 */
.card-cover-wrap {
  position: relative;
  overflow: hidden;
}

.card-status-badge {
  position: absolute;
  top: 12px;
  left: 0;
  padding: 4px 12px 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.5px;
  border-radius: 0 999px 999px 0;
  z-index: 2;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(4px);
}

.card-status-badge.status-2 {
  background: linear-gradient(90deg, #ff4d6d, #ff7849);
}

.card-status-badge.status-1 {
  background: linear-gradient(90deg, #e6a23c, #f7c948);
}

.card-status-badge.status-3 {
  background: linear-gradient(90deg, #909399, #b1b3b8);
}

/* 弹窗广告关闭按钮（弹窗内右上角） */
.popup-ad-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  z-index: 10;
  transition: all 0.2s ease;
}

.popup-ad-close:hover {
  background: rgba(0, 0, 0, 0.7);
  transform: rotate(90deg);
}

.popup-ad-close .el-icon {
  font-size: 16px;
}

.popup-ad-skip {
  text-align: center;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  padding-top: 10px;
}

.popup-ad-skip:hover {
  color: #409EFF;
}

.popup-ad-btn {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border: none;
  padding: 10px 36px;
  border-radius: 20px;
  letter-spacing: 1px;
}

.popup-ad-btn:hover {
  opacity: 0.92;
}

.announcement-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.section-header :deep(.el-button.text) {
  color: #909399;
  font-weight: 500;
  transition: color 0.2s ease, transform 0.2s ease;
}

.section-header :deep(.el-button.text:hover) {
  color: #ff4d6d;
  transform: translateX(2px);
}

.reco-tip {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
  margin-left: 10px;
  margin-right: auto;
}

.section-title {
  position: relative;
  font-size: 23px;
  font-weight: 700;
  color: #1b2a4a;
  padding-left: 14px;
  line-height: 1.3;
  letter-spacing: 0.5px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 22px;
  border-radius: 3px;
  background: linear-gradient(180deg, #ff7a45, #ff4d6d);
}

/* 活动卡片 */
.activity-card {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.28s var(--ease-out), box-shadow 0.28s var(--ease-out);
  margin-bottom: 20px;
  border: 1px solid #eef1f6;
  box-shadow: 0 3px 12px rgba(31, 92, 170, 0.06);
}

.activity-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 40px rgba(31, 92, 170, 0.16);
  border-color: rgba(255, 77, 109, 0.2);
}

.card-cover-wrap {
  overflow: hidden;
  background: #eef1f6;
  position: relative;
}

.card-cover-wrap::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(0, 0, 0, 0.15) 100%);
  pointer-events: none;
  z-index: 1;
}

.card-cover {
  width: 100%;
  height: 186px;
  display: block;
  transition: transform 0.5s var(--ease-out);
}

.activity-card:hover .card-cover {
  transform: scale(1.07);
}

.image-placeholder {
  width: 100%;
  height: 186px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef1f6, #e8ebf0);
  color: #c0c4cc;
  font-size: 40px;
}

.card-info {
  padding: 14px 15px 15px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1b2a4a;
  margin-bottom: 9px;
  line-height: 1.4;
  /* 大麦风格：标题最多两行，避免长标题被单行截断 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
  transition: color 0.2s ease;
}

.activity-card:hover .card-title {
  color: #ff4d6d;
}

.card-location,
.card-time {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-location .el-icon,
.card-time .el-icon {
  color: #c0c4cc;
  font-size: 13px;
}

.card-bottom {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  flex-wrap: wrap;
  padding-top: 12px;
  border-top: 1px dashed #eef1f6;
}

.card-price {
  color: #ff4d6d;
  font-size: 21px;
  font-weight: 800;
  font-style: italic;
  text-shadow: 0 2px 8px rgba(255, 77, 109, 0.1);
}

.card-price-suffix {
  font-size: 12px;
  font-weight: normal;
  margin-left: 2px;
  font-style: normal;
  color: #909399;
}

.card-remain {
  font-size: 12px;
  color: #909399;
}

/* 大麦风格：已售进度条 + 想看热度（卡片信息更丰富） */
.card-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  flex-wrap: wrap;
}

.card-progress {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.progress-track {
  flex: 1;
  min-width: 42px;
  height: 6px;
  border-radius: 999px;
  background: #eef1f6;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #ff7a45, #ff4d6d);
  transition: width 0.4s ease;
}

.progress-fill.hot {
  background: linear-gradient(90deg, #ff2e63, #ff4d6d);
}

.progress-text {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  flex-shrink: 0;
}

.card-wish-tag {
  flex-shrink: 0;
  font-size: 11px;
  color: #ff4d6d;
  background: rgba(255, 77, 109, 0.08);
  border: 1px solid rgba(255, 77, 109, 0.25);
  border-radius: 999px;
  padding: 1px 7px;
  white-space: nowrap;
}

/* 立即购票按钮：渐变胶囊 */
.card-buy-btn {
  margin-left: auto;
  padding: 6px 16px;
  border-radius: 15px;
  border: none;
  background: var(--gradient-grab);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.25s var(--ease-out);
  box-shadow: 0 5px 12px rgba(255, 77, 109, 0.26);
}

.card-buy-btn:hover,
.card-buy-btn:focus {
  color: #fff;
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 8px 20px rgba(255, 77, 109, 0.4);
  background: var(--gradient-grab-deep);
}

/* 非抢票中状态：按钮置灰（即将开售/已结束） */
.card-buy-btn.is-muted {
  background: #eef0f3;
  color: #909399;
  box-shadow: none;
}
.card-buy-btn.is-muted:hover,
.card-buy-btn.is-muted:focus {
  background: #e6e8eb;
  color: #909399;
  transform: none;
  box-shadow: none;
}

/* 在线选座标识（封面右上角） */
.card-seat-badge {
  position: absolute;
  top: 12px;
  right: 10px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  border-radius: 999px;
  background: linear-gradient(90deg, #7b5cff, #4d8bff);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.22);
  z-index: 2;
}

/* 最新活动：状态切换 tabs（对标大麦"正在热卖"） */
.latest-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
}
.latest-tab {
  padding: 4px 14px;
  font-size: 13px;
  color: #606266;
  border-radius: 999px;
  cursor: pointer;
  background: #f4f5f7;
  transition: all 0.2s var(--ease-out);
  user-select: none;
}
.latest-tab:hover {
  color: #ff2e63;
}
.latest-tab.active {
  color: #fff;
  background: var(--gradient-grab, linear-gradient(90deg, #ff4d6d, #ff7849));
  font-weight: 600;
}

/* 分类快捷 chips（演唱会/体育/展览… + 选座专区） */
.latest-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 4px 0 18px;
}
.latest-chip {
  padding: 5px 14px;
  font-size: 13px;
  color: #606266;
  border-radius: 999px;
  cursor: pointer;
  background: #f4f5f7;
  border: 1px solid transparent;
  transition: all 0.2s var(--ease-out);
  user-select: none;
}
.latest-chip:hover {
  color: #ff2e63;
  border-color: rgba(255, 46, 99, 0.4);
}
.latest-chip.active {
  color: #fff;
  background: var(--gradient-grab, linear-gradient(90deg, #ff4d6d, #ff7849));
  font-weight: 600;
}

/* 主体区与侧边栏广告布局：使用 grid 严格锁定两列宽度，避免内容相互挤压/重叠 */
.main-with-sidebar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 268px;
  column-gap: 24px;
  align-items: start;
  padding: 0 20px;
  margin-bottom: 30px;
  box-sizing: border-box;
  max-width: 100%;
}

.main-left {
  min-width: 0; /* 关键：允许内容区收缩，防止溢出压到侧边栏 */
  width: 100%;
  max-width: 100%;
  overflow: hidden;
}

.main-left .section {
  padding: 0;
  min-width: 0;
  max-width: 100%;
}

/* 侧边栏：即将开抢 + 热销榜 */
.sidebar-widgets {
  width: 268px;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.widget-card {
  background: #fff;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 3px 14px rgba(31, 92, 170, 0.07);
  border: 1px solid #eef1f6;
  min-width: 0;
  overflow: hidden;
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 9px;
  padding-bottom: 14px;
  margin-bottom: 6px;
  border-bottom: 1px solid #f0f2f5;
}

.widget-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
}

.widget-icon-clock {
  background: linear-gradient(135deg, #fff4e6, #ffe8d6);
  box-shadow: 0 3px 8px rgba(255, 159, 67, 0.18);
}

.widget-icon-fire {
  background: linear-gradient(135deg, #fff2ef, #ffe6ec);
  box-shadow: 0 3px 8px rgba(255, 77, 109, 0.18);
}

.widget-title {
  font-size: 16px;
  font-weight: 700;
  color: #1b2a4a;
  letter-spacing: 0.5px;
}

/* 即将开抢 */
.upcoming-item {
  display: flex;
  align-items: center;
  padding: 12px 6px;
  border-bottom: 1px dashed #eef1f6;
  cursor: pointer;
  border-radius: 10px;
  transition: background 0.2s ease;
}

.upcoming-item:last-child {
  border-bottom: none;
}

.upcoming-item:hover {
  background: #f8f9fc;
}

.upcoming-item:hover .upcoming-name {
  color: #409EFF;
}

.upcoming-info {
  width: 100%;
  min-width: 0;
}

.upcoming-name {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
  font-weight: 500;
  transition: color 0.2s ease;
}

.upcoming-countdown {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #ff4d6d;
  font-variant-numeric: tabular-nums;
  background: linear-gradient(135deg, #fff2ef, #ffe9ef);
  padding: 3px 10px;
  border-radius: 999px;
}

/* 热销榜 */
.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 6px;
  border-bottom: 1px dashed #eef1f6;
  cursor: pointer;
  border-radius: 10px;
  transition: background 0.2s ease;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-item:hover {
  background: #f8f9fc;
}

.rank-item:hover .rank-name {
  color: #409EFF;
}

.rank-no {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank-no.rank-1 {
  background: linear-gradient(135deg, #ff4d6d, #ff7849);
  color: #fff;
  box-shadow: 0 3px 8px rgba(255, 77, 109, 0.32);
}

.rank-no.rank-2 {
  background: linear-gradient(135deg, #ff9f43, #f7c948);
  color: #fff;
  box-shadow: 0 3px 8px rgba(255, 159, 67, 0.32);
}

.rank-no.rank-3 {
  background: linear-gradient(135deg, #409EFF, #36cfc9);
  color: #fff;
  box-shadow: 0 3px 8px rgba(64, 158, 255, 0.32);
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
  font-weight: 500;
  transition: color 0.2s ease;
}

.rank-price {
  font-size: 14px;
  font-weight: 700;
  color: #ff4d6d;
}

.rank-heat {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 8px;
}

/* 侧边栏合作推广广告位 */
.sidebar-ad {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px dashed #eef1f6;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s ease;
}
.sidebar-ad:last-child {
  border-bottom: none;
}
.sidebar-ad:hover {
  background: #f8f9fc;
}
.sidebar-ad:hover .sidebar-ad-title {
  color: #409EFF;
}
.sidebar-ad-img {
  width: 64px;
  height: 44px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: cover;
}
.sidebar-ad-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
.sidebar-ad-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
}
.ad-badge-mini {
  flex-shrink: 0;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 11px;
  color: #ff7d00;
  background: rgba(255, 125, 0, 0.08);
  border: 1px solid rgba(255, 125, 0, 0.25);
  letter-spacing: 1px;
}
.ad-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 18px;
}

/* 广告标识角标（广告法合规） */
.ad-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.45);
  color: rgba(255, 255, 255, 0.85);
  font-size: 11px;
  line-height: 1.4;
  letter-spacing: 1px;
  pointer-events: none;
}

/* 兜底空状态 */
.home-empty {
  padding: 40px 20px;
}

/* 弹窗广告轮播指示点 */
.popup-ad-dots {
  position: absolute;
  bottom: 10px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 6px;
  z-index: 5;
}

.popup-ad-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.25s ease;
}

.popup-ad-dot.active {
  background: #fff;
  transform: scale(1.2);
}

/* 弹窗广告 */
.popup-ad-dialog :deep(.el-dialog__body) {
  padding: 0;
  position: relative;
}

.popup-ad-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

.popup-ad-image-wrap {
  position: relative;
}

.popup-ad-image {
  width: 100%;
  height: 280px;
  display: block;
}

.popup-ad-image .ad-image-placeholder.popup {
  height: 280px;
  font-size: 48px;
}

.popup-ad-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  padding: 16px 20px 8px;
}

.popup-ad-btn {
  margin-bottom: 20px;
}

/* 平台特色服务条 */
.feature-bar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.feature-item {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 13px;
  background: #fff;
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 3px 14px rgba(31, 92, 170, 0.07);
  border: 1px solid #eef1f6;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.feature-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 26px rgba(31, 92, 170, 0.12);
}

.feature-ico {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff2ef, #ffe9ef);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 23px;
  flex-shrink: 0;
}

.feature-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.45;
}

.feature-meta b {
  font-size: 14px;
  color: #1b2a4a;
  letter-spacing: 0.5px;
}

.feature-meta i {
  font-style: normal;
  font-size: 12px;
  color: #909399;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  .home-page {
    max-width: 100%;
    box-sizing: border-box;
    padding-bottom: 24px;
  }

  /* 轮播图：缩小高度，减小内边距 */
  .banner-section {
    padding: 0 12px;
    margin-bottom: 16px;
  }

  .banner-container {
    height: 190px;
    border-radius: 14px;
  }

  .banner-item {
    padding: 0 20px;
  }

  .banner-title {
    font-size: 22px;
    margin-bottom: 8px;
  }

  .banner-desc {
    font-size: 13px;
    margin-bottom: 14px;
  }

  .banner-btn {
    font-size: 13px;
    padding: 9px 22px;
  }

  .banner-prev,
  .banner-next {
    width: 34px;
    height: 34px;
    font-size: 15px;
  }

  .banner-prev {
    left: 8px;
  }

  .banner-next {
    right: 8px;
  }

  /* 通用区域：缩小左右留白 */
  .section {
    padding: 0 12px;
    margin-bottom: 20px;
  }

  .section-title {
    font-size: 18px;
  }

  /* 热门搜索条：标签换行 */
  .hot-search-card {
    padding: 12px 14px;
  }

  .hot-more {
    margin-left: 0;
    width: 100%;
    text-align: left;
  }

  /* 分类导航：两列网格 */
  .category-nav {
    padding: 16px 14px 14px;
  }

  .category-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 14px 8px;
  }

  .category-icon {
    width: 46px;
    height: 46px;
    border-radius: 14px;
    font-size: 22px;
  }

  /* 特色服务条：单列 */
  .feature-bar {
    flex-direction: column;
    gap: 10px;
  }

  .feature-item {
    min-width: 0;
    width: 100%;
    padding: 12px 14px;
  }

  /* 公告条：隐藏"查看全部"，标题独占一行 */
  .announcement-section {
    padding: 12px 14px;
  }

  .announcement-more {
    display: none;
  }

  /* 主体区与侧边栏：改为单列，侧边栏内容排在下方 */
  .main-with-sidebar {
    grid-template-columns: minmax(0, 1fr);
    padding: 0 12px;
    gap: 16px;
  }

  .sidebar-widgets {
    width: 100%;
  }

  /* 卡片网格：两列 */
  .activity-card {
    margin-bottom: 12px;
  }

  .card-cover {
    height: 132px;
  }

  .image-placeholder {
    height: 132px;
    font-size: 30px;
  }

  .card-info {
    padding: 10px 10px 12px;
  }

  .card-title {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .card-price {
    font-size: 17px;
  }

  .card-buy-btn {
    padding: 5px 12px;
    font-size: 11px;
  }

  .card-status-badge {
    top: 8px;
    font-size: 11px;
    padding: 3px 10px 3px 8px;
  }

  /* 弹窗广告：宽度自适应 */
  .popup-ad-image {
    height: 190px;
  }

  .popup-ad-image .ad-image-placeholder.popup {
    height: 190px;
    font-size: 36px;
  }
}

</style>

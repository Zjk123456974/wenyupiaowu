<template>
  <div class="user-layout">
    <!-- 顶部信息条 -->
    <div class="header-top">
      <div class="top-inner">
        <div class="top-left">
          <span class="top-slogan">{{ siteName }} · 一站式文娱活动购票平台</span>
        </div>
        <div class="top-right">
          <template v-if="userStore.isLoggedIn">
            <span class="top-greet">Hi，{{ userStore.nickname || userStore.username }}</span>
            <el-link v-if="userStore.role === 1" class="top-link" @click="$router.push('/operator/dashboard')">运营后台</el-link>
            <el-link v-if="userStore.role === 2" class="top-link" @click="$router.push('/admin/dashboard')">管理后台</el-link>
            <el-link class="top-link" @click="$router.push('/user/center/orders')">我的订单</el-link>
            <el-link class="top-link" @click="$router.push('/user/center/tickets')">我的票夹</el-link>
            <el-link class="top-link" @click="handleLogout">退出登录</el-link>
          </template>
          <template v-else>
            <el-link class="top-link" @click="$router.push('/login')">登录</el-link>
            <el-link class="top-link strong" @click="$router.push('/register')">免费注册</el-link>
          </template>
        </div>
      </div>
    </div>

    <!-- 主导航 -->
    <header class="site-header">
      <div class="header-inner">
        <div class="brand" @click="$router.push('/')">
          <div class="brand-logo">
            <img :src="siteConfig.logo || '/logo.jpg'" class="brand-logo-img" alt="logo" />
          </div>
          <div class="brand-text">
            <span class="brand-name">{{ siteName }}</span>
            <span class="brand-sub">智能文娱票务</span>
          </div>
        </div>

        <nav class="main-nav">
          <span class="nav-item" :class="{ active: isActive('/') }" @click="$router.push('/')">首页</span>
          <el-dropdown trigger="click" class="nav-dropdown" @command="handleCategory">
            <span class="nav-item" :class="{ active: route.path === '/activity/list' }">
              演出分类<el-icon class="caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="cat in categories" :key="cat.id" :command="cat.id">
                  <span class="cat-emoji">{{ categoryEmoji(cat.id) }}</span>{{ cat.name }}
                </el-dropdown-item>
                <el-dropdown-item divided command="all">全部演出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="nav-item" :class="{ active: isActive('/activity/list') && !route.query.categoryId }" @click="$router.push('/activity/list')">全部演出</span>
          <span class="nav-item" :class="{ active: isActive('/announcements') }" @click="$router.push('/announcements')">系统公告</span>
        </nav>

        <div class="header-search" @mouseleave="showHot = false">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              size="large"
              class="search-input"
              :placeholder="hotPlaceholder"
              clearable
              @focus="showHot = true"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" class="search-btn" @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
            <!-- 热门搜索下拉面板（对标大麦/猫眼 热搜榜） -->
            <div v-if="showHot" class="hot-panel" @mousedown.prevent>
              <div class="hot-head"><span class="hot-fire">🔥</span> 热门搜索</div>
              <div class="hot-tags">
                <span
                  v-for="(h, i) in hotSearches"
                  :key="h"
                  class="hot-tag"
                  :class="{ 'hot-top': i < 3 }"
                  @click="quickSearch(h)"
                >
                  <i class="hot-rank">{{ i + 1 }}</i>{{ h }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="userStore.unreadCount" :hidden="!userStore.unreadCount" class="msg-badge">
              <span class="msg-icon" @click="$router.push('/user/center/messages')">
                <el-icon :size="20"><Bell /></el-icon>
              </span>
            </el-badge>
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-info">
                <el-avatar :size="32" :src="userStore.avatar || undefined" class="user-avatar">
                  {{ (userStore.nickname || userStore.username || 'U')[0] }}
                </el-avatar>
                <span class="nickname ellipsis">{{ userStore.nickname || userStore.username }}</span>
                <el-icon class="caret-down"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人信息
                  </el-dropdown-item>
                  <el-dropdown-item command="orders">
                    <el-icon><Tickets /></el-icon>我的订单
                  </el-dropdown-item>
                  <el-dropdown-item command="tickets">
                    <el-icon><Ticket /></el-icon>我的电子票
                  </el-dropdown-item>
                  <el-dropdown-item command="reservations">
                    <el-icon><Calendar /></el-icon>我的预约
                  </el-dropdown-item>
                  <el-dropdown-item command="favorites">
                    <el-icon><Star /></el-icon>我的收藏
                  </el-dropdown-item>
                  <el-dropdown-item command="attendees">
                    <el-icon><Avatar /></el-icon>观演人管理
                  </el-dropdown-item>
                  <el-dropdown-item command="messages">
                    <el-icon><Bell /></el-icon>站内消息
                  </el-dropdown-item>
                  <el-dropdown-item command="consumption">
                    <el-icon><Wallet /></el-icon>消费记录
                  </el-dropdown-item>
                  <el-dropdown-item command="password" divided>
                    <el-icon><Lock /></el-icon>修改密码
                  </el-dropdown-item>
                  <el-dropdown-item command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button class="login-btn" round @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" round class="register-btn" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>

    <!-- 页脚推广广告条（站点通用，仅展示后台已启用的广告） -->
    <div v-if="footerAds.length" class="footer-ad-strip">
      <div class="footer-ad-inner">
        <div
          v-for="ad in footerAds"
          :key="'fad-' + ad.id"
          class="footer-ad-item"
          @click="handleAdClick(ad)"
        >
          <el-image :src="ad.imageUrl" fit="cover" class="footer-ad-img" lazy>
            <template #error>
              <div class="footer-ad-img footer-ad-img-placeholder"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>
          <div class="footer-ad-title">{{ ad.title }}</div>
          <span class="footer-ad-badge">推广</span>
        </div>
      </div>
    </div>

    <footer class="site-footer">
      <div class="footer-inner">
        <div class="footer-col footer-brand">
          <div class="footer-logo">
            <span class="footer-logo-icon">
              <img :src="siteConfig.logo || '/logo.jpg'" class="footer-logo-img" alt="logo" />
            </span>
            <span>{{ siteName }}</span>
          </div>
          <p class="footer-desc">{{ siteName }}智能文娱票务预约抢票系统，覆盖演唱会、话剧、展览、体育赛事等全品类文娱活动，为您提供安全、便捷、贴心的购票服务。</p>
          <div class="footer-contacts">
            <span><el-icon><Phone /></el-icon> 客服热线：{{ siteConfig.service_phone || '400-000-0000' }}</span>
            <span><el-icon><Message /></el-icon> service@ticket-online.cn</span>
          </div>
        </div>
        <div class="footer-col">
          <h4>购票服务</h4>
          <ul>
            <li @click="$router.push('/user/center/orders')">我的订单</li>
            <li @click="$router.push('/user/center/tickets')">电子票夹</li>
            <li @click="$router.push('/user/center/reservations')">预约记录</li>
            <li @click="$router.push('/user/center/refunds')">退票申请</li>
          </ul>
        </div>
        <div class="footer-col">
          <h4>平台导航</h4>
          <ul>
            <li @click="$router.push('/activity/list')">全部演出</li>
            <li @click="$router.push('/announcements')">平台公告</li>
            <li @click="$router.push('/activity/list')">热销榜</li>
            <li @click="$router.push('/')">回到首页</li>
          </ul>
        </div>
        <div class="footer-col">
          <h4>关于我们</h4>
          <ul>
            <li @click="$router.push('/announcements')">购票须知</li>
            <li @click="$router.push('/announcements')">退票规则</li>
            <li @click="$router.push('/user/center/attendees')">观演人管理</li>
            <li @click="$router.push('/user/center/messages')">联系客服</li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© 2026 {{ siteName }} · 智能文娱票务系统</span>
        <span class="footer-sep">|</span>
        <span>毕业设计项目 · 仅用于学习交流</span>
        <span v-if="siteConfig.icp" class="footer-sep">|</span>
        <a v-if="siteConfig.icp" class="footer-icp" href="https://beian.miit.gov.cn/" target="_blank">{{ siteConfig.icp }}</a>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/message'
import { getCategoryList } from '@/api/category'
import { getPublicConfig } from '@/api/config'
import { getActiveAds } from '@/api/ad'
import { ElNotification } from 'element-plus'
import { Search, Bell, Ticket, ArrowDown, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const searchKeyword = ref('')
const categories = ref([])
// 热门搜索（聚焦时弹出热搜面板，数据来自分类名，回退为默认词）
const showHot = ref(false)
const hotSearches = ref(['演唱会', '音乐节', '话剧', '脱口秀', '艺术展', '体育赛事'])
const hotPlaceholder = computed(() => `搜索 ${hotSearches.value[0]}、${hotSearches.value[1]} 等`)
// 站点公开配置（后台【系统配置】修改后全站同步生效）
const siteConfig = ref({ name: '', logo: '', icp: '', service_phone: '' })
const siteName = computed(() => siteConfig.value.name || '票务在线')
// 页脚推广广告条
const footerAds = ref([])

const loadSiteConfig = async () => {
  try {
    const res = await getPublicConfig()
    const data = res.data || {}
    siteConfig.value = {
      name: data.name || '票务在线',
      logo: data.logo || '',
      icp: data.icp || '',
      service_phone: data.service_phone || '',
    }
    document.title = `${siteConfig.value.name} · 一站式文娱活动购票平台`
  } catch {
    document.title = `${siteName.value} · 一站式文娱活动购票平台`
  }
}

const loadFooterAds = async () => {
  try {
    const res = await getActiveAds('footer')
    footerAds.value = (res.data || []).slice(0, 3)
  } catch {
    footerAds.value = []
  }
}

const handleAdClick = (ad) => {
  if (!ad) return
  if (ad.activityId) {
    router.push(`/activity/${ad.activityId}`)
  } else if (ad.linkUrl) {
    window.open(ad.linkUrl, '_blank', 'noopener')
  }
}
const categoryEmoji = (id) => {
  const map = { 1: '🎤', 2: '🎭', 3: '🎨', 4: '⚽', 5: '🎓' }
  return map[id] || '🎪'
}

const isActive = (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const handleSearch = () => {
  doSearch()
}

// 执行搜索：构造查询参数，按关键词（标题/场馆/分类）模糊匹配
const doSearch = () => {
  const kw = searchKeyword.value.trim()
  const q = {}
  if (kw) q.keyword = kw
  router.push({ path: '/activity/list', query: q })
}

// 点击热搜词直接搜索
const quickSearch = (kw) => {
  searchKeyword.value = kw
  showHot.value = false
  doSearch()
}

const handleCategory = (cmd) => {
  if (cmd === 'all') {
    router.push('/activity/list')
  } else {
    router.push({ path: '/activity/list', query: { categoryId: cmd } })
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = (res.data || []).filter((c) => c.status === 1)
    // 用真实分类名填充热搜词（保留前 6 个）
    if (categories.value.length) {
      hotSearches.value = categories.value.map((c) => c.name).slice(0, 6)
    }
  } catch {
    // 静默处理
  }
}

const handleCommand = (command) => {
  const map = {
    profile: '/user/center/profile',
    orders: '/user/center/orders',
    tickets: '/user/center/tickets',
    reservations: '/user/center/reservations',
    favorites: '/user/center/favorites',
    attendees: '/user/center/attendees',
    messages: '/user/center/messages',
    consumption: '/user/center/consumption',
    password: '/user/center/password'
  }
  if (command === 'logout') {
    handleLogout()
  } else if (map[command]) {
    router.push(map[command])
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

let pollingTimer = null
let sseSource = null
let lastUnreadCount = 0

const fetchUnreadCount = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getUnreadCount()
    const newCount = res.data || 0
    if (newCount > lastUnreadCount && lastUnreadCount >= 0) {
      ElNotification({
        title: '新消息通知',
        message: `您有 ${newCount} 条新消息`,
        type: 'info'
      })
    }
    lastUnreadCount = newCount
    userStore.setUnreadCount(newCount)
  } catch {
    // ignore
  }
}

const startPolling = () => {
  stopPolling()
  lastUnreadCount = -1
  fetchUnreadCount()
  pollingTimer = setInterval(fetchUnreadCount, 15000)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
  lastUnreadCount = 0
}

let sseReconnectTimer = null
const startSse = () => {
  stopSse()
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    sseSource = new EventSource('/api/sse/subscribe?token=' + encodeURIComponent(token))
    sseSource.onmessage = () => fetchUnreadCount()
    sseSource.onerror = () => {
      // 出错后停止当前连接，并在 3 秒后尝试重连（避免静默丢失实时推送）
      stopSse()
      if (sseReconnectTimer) clearTimeout(sseReconnectTimer)
      sseReconnectTimer = setTimeout(() => {
        if (userStore.isLoggedIn) startSse()
      }, 3000)
    }
  } catch {
    sseSource = null
  }
}

const stopSse = () => {
  if (sseReconnectTimer) {
    clearTimeout(sseReconnectTimer)
    sseReconnectTimer = null
  }
  if (sseSource) {
    sseSource.close()
    sseSource = null
  }
}

watch(
  () => userStore.isLoggedIn,
  (val) => {
    if (val) {
      startPolling()
      startSse()
    } else {
      stopPolling()
      stopSse()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadSiteConfig()
  loadFooterAds()
  loadCategories()
  if (userStore.isLoggedIn) {
    startPolling()
    startSse()
  }
})

onUnmounted(() => {
  stopPolling()
  stopSse()
})
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  min-width: 1200px;
}

/* Logo 图片（站点配置可换） */
.brand-logo-img {
  width: 100%;
  height: 100%;
  border-radius: 12px;
  object-fit: cover;
}

/* 页脚推广广告条 */
.footer-ad-strip {
  background: #fff;
  border-top: 1px solid #f0f2f5;
  border-bottom: 1px solid #f0f2f5;
  padding: 14px 0;
}
.footer-ad-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  gap: 16px;
  overflow: hidden;
}
.footer-ad-item {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  background: #fafbfc;
  border-radius: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.footer-ad-item:hover {
  background: #f2f4f8;
}
.footer-ad-img {
  width: 96px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  display: inline-flex;
}
.footer-ad-img-placeholder {
  align-items: center;
  justify-content: center;
  background: #eef1f6;
  color: #c0c4cc;
  font-size: 16px;
}
.footer-ad-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.footer-ad-badge {
  flex-shrink: 0;
  font-size: 11px;
  color: #ff7d00;
  border: 1px solid rgba(255, 125, 0, 0.3);
  border-radius: 4px;
  padding: 1px 5px;
  letter-spacing: 1px;
  background: rgba(255, 125, 0, 0.06);
}

/* ===== 顶部信息条 ===== */
.header-top {
  background: linear-gradient(90deg, #1b2a4a, #243a63 60%, #2b6cb8);
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.top-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 6px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-left .top-slogan {
  letter-spacing: 0.5px;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.top-greet {
  margin-right: 6px;
  color: rgba(255, 255, 255, 0.75);
}

.top-link {
  color: rgba(255, 255, 255, 0.85);
  padding: 0 8px;
  font-size: 13px;
}

.top-link:hover {
  color: #fff;
}

.top-link.strong {
  color: #ffd43b;
}

/* ===== 主头部 ===== */
.site-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 14px rgba(27, 42, 74, 0.12);
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 68px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}

/* Logo */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff7a45, #ff4d6d 60%, #ff2d55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 14px rgba(255, 77, 109, 0.35);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-name {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 1px;
  background: linear-gradient(90deg, #1b2a4a, #2b6cb8);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.brand-sub {
  font-size: 11px;
  color: #909399;
  letter-spacing: 2px;
}

/* 主导航 */
.main-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nav-item {
  position: relative;
  padding: 0 16px;
  height: 68px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  color: #303133;
  cursor: pointer;
  transition: color 0.2s ease;
  white-space: nowrap;
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(90deg, #ff7a45, #ff4d6d);
  transform: scaleX(0);
  transition: transform 0.25s ease;
}

.nav-item:hover {
  color: #ff4d6d;
}

.nav-item.active {
  color: #ff4d6d;
  font-weight: 600;
}

.nav-item.active::after {
  transform: scaleX(1);
}

.caret {
  font-size: 12px;
  margin-left: 2px;
}

.nav-dropdown {
  outline: none;
}

.cat-emoji {
  margin-right: 6px;
}

/* 头部搜索（对标大麦：大搜索框 + 城市切换 + 热搜面板） */
.header-search {
  flex: 1;
  min-width: 280px;
  max-width: 560px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  height: 48px;
  border-radius: 26px;
  background: #fff;
  box-shadow: 0 0 0 2px #ff7a45, 0 6px 18px rgba(255, 122, 69, 0.18);
  transition: box-shadow 0.2s ease;
}

.search-box:focus-within {
  box-shadow: 0 0 0 2px #ff4d6d, 0 8px 22px rgba(255, 77, 109, 0.22);
}

.search-input {
  flex: 1;
  min-width: 0;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 0;
  box-shadow: none !important;
  background: transparent;
  padding-left: 6px;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: none !important;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 26px 26px 0;
  overflow: hidden;
  background: transparent;
  box-shadow: none;
}

.search-input :deep(.search-btn) {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border: none;
  color: #fff;
  height: 40px;
  padding: 0 26px;
  border-radius: 0 26px 26px 0;
  font-size: 15px;
  font-weight: 600;
}

.search-input :deep(.search-btn:hover) {
  opacity: 0.92;
}

/* 热搜面板 */
.hot-panel {
  position: absolute;
  top: 56px;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 30px rgba(27, 42, 74, 0.16);
  padding: 14px 16px;
  z-index: 1200;
}

.hot-head {
  font-size: 14px;
  font-weight: 600;
  color: #1b2a4a;
  margin-bottom: 10px;
}

.hot-fire {
  margin-right: 2px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  border-radius: 14px;
  padding: 5px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hot-tag:hover {
  color: #ff4d6d;
  background: #fff0f1;
}

.hot-tag.hot-top {
  color: #ff4d6d;
  font-weight: 600;
}

.hot-rank {
  font-style: normal;
  font-size: 11px;
  width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  border-radius: 4px;
  background: #e9ecf3;
  color: #909399;
}

.hot-tag.hot-top .hot-rank {
  background: #ff4d6d;
  color: #fff;
}

/* 右侧操作区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.msg-badge {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.msg-icon {
  display: inline-flex;
  align-items: center;
  color: #303133;
  transition: color 0.2s ease;
}

.msg-icon:hover {
  color: #ff4d6d;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background 0.2s ease;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: linear-gradient(135deg, #409eff, #35c1f0);
  flex-shrink: 0;
}

.nickname {
  max-width: 80px;
  font-size: 14px;
  color: #303133;
}

.caret-down {
  font-size: 12px;
  color: #909399;
}

.login-btn {
  color: #303133;
}

.register-btn {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  border: none;
}

.register-btn:hover {
  opacity: 0.9;
}

/* ===== 主内容 ===== */
.main-content {
  flex: 1;
  width: 100%;
}

/* ===== 页脚 ===== */
.site-footer {
  background: linear-gradient(180deg, #1b2a4a 0%, #16213a 100%);
  color: rgba(255, 255, 255, 0.72);
  margin-top: 24px;
}

.footer-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 20px 24px;
  display: flex;
  justify-content: space-between;
  gap: 40px;
  flex-wrap: wrap;
}

.footer-col h4 {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
  letter-spacing: 1px;
}

.footer-col ul {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-col ul li {
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.footer-col ul li:hover {
  color: #ffd43b;
}

.footer-brand {
  max-width: 360px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 14px;
}

.footer-logo-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.footer-logo-img {
  width: 100%;
  height: 100%;
  border-radius: 9px;
  object-fit: cover;
}

.footer-desc {
  font-size: 13px;
  line-height: 1.8;
  margin-bottom: 16px;
}

.footer-contacts {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.footer-contacts span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.footer-icp {
  color: rgba(255, 255, 255, 0.55);
  text-decoration: none;
}
.footer-icp:hover {
  color: #ffd43b;
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 16px 20px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  display: flex;
  justify-content: center;
  gap: 10px;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  /* 取消桌面端 1200px 的最小宽度限制，否则窄屏会出现整页横向滚动 */
  .user-layout {
    min-width: 0;
    width: 100%;
    overflow-x: hidden;
  }

  /* 顶部信息条：标语隐藏，链接换行 */
  .header-top {
    display: none;
  }

  /* 主头部：改为两行（品牌 + 操作区），搜索框独占一行 */
  .header-inner {
    height: auto;
    min-height: 56px;
    padding: 10px 12px;
    flex-wrap: wrap;
    gap: 10px;
  }

  .brand {
    order: 1;
  }

  .brand-logo {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .brand-name {
    font-size: 18px;
  }

  .header-right {
    order: 2;
    margin-left: auto;
    gap: 8px;
  }

  /* 窄屏隐藏主导航与顶部搜索，保留品牌与用户入口 */
  .main-nav,
  .header-search {
    display: none;
  }

  .nickname {
    max-width: 60px;
  }

  /* 页脚：单列堆叠 */
  .footer-inner {
    padding: 24px 16px 16px;
    gap: 20px;
    flex-direction: column;
  }

  .footer-brand {
    max-width: 100%;
  }

  .footer-bottom {
    flex-wrap: wrap;
    font-size: 11px;
  }

  /* 推广广告条：窄屏横向滚动 */
  .footer-ad-inner {
    padding: 12px;
    gap: 10px;
    overflow-x: auto;
  }

  .footer-ad-item {
    flex: 0 0 200px;
  }
}
</style>

<style>
/* 下拉菜单图标色 */
.el-dropdown-menu .el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.el-dropdown-menu .el-dropdown-menu__item .el-icon {
  font-size: 15px;
}
</style>

<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" class="admin-aside">
      <div class="logo-area">
        <img src="/logo.jpg" class="logo-img" alt="logo" />
        <span>管理员中心</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#1d1e1f"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/activities">
          <el-icon><Tickets /></el-icon>
          <span>活动管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/tag">
          <el-icon><PriceTag /></el-icon>
          <span>标签管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/banners">
          <el-icon><Picture /></el-icon>
          <span>轮播图管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/ad">
          <el-icon><Promotion /></el-icon>
          <span>广告位管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/announcements">
          <el-icon><Bell /></el-icon>
          <span>公告管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/ticket-check">
          <el-icon><Checked /></el-icon>
          <span>检票核销</span>
        </el-menu-item>
        <el-menu-item index="/admin/refund">
          <el-icon><RefreshLeft /></el-icon>
          <span>退票审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/review">
          <el-icon><ChatDotRound /></el-icon>
          <span>评价管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/finance">
          <el-icon><Wallet /></el-icon>
          <span>财务对账</span>
        </el-menu-item>
        <el-menu-item index="/admin/log">
          <el-icon><Document /></el-icon>
          <span>系统日志</span>
        </el-menu-item>
        <el-menu-item index="/admin/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item
            v-for="item in breadcrumbs"
            :key="item.path"
            :to="item.path"
          >
            {{ item.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <el-button text @click="goHome">用户界面</el-button>
          <span class="username">{{ userStore.nickname || userStore.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { DataBoard, User, Tickets, List, Menu, Picture, Bell, DataAnalysis, Wallet, Setting, PriceTag, Promotion, Checked, RefreshLeft, ChatDotRound, Document } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const breadcrumbs = computed(() => {
  const crumbs = [{ path: '/admin/dashboard', title: '首页' }]
  const map = {
    '/admin/dashboard': '仪表盘',
    '/admin/users': '用户管理',
    '/admin/activities': '活动管理',
    '/admin/orders': '订单管理',
    '/admin/categories': '分类管理',
    '/admin/tag': '标签管理',
    '/admin/banners': '轮播图管理',
    '/admin/ad': '广告位管理',
    '/admin/announcements': '公告管理',
    '/admin/ticket-check': '检票核销',
    '/admin/refund': '退票审核',
    '/admin/review': '评价管理',
    '/admin/stats': '数据统计',
    '/admin/finance': '财务对账',
    '/admin/log': '系统日志',
    '/admin/config': '系统配置'
  }
  if (route.path !== '/admin/dashboard' && map[route.path]) {
    crumbs.push({ path: route.path, title: map[route.path] })
  }
  return crumbs
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.admin-aside {
  background-color: #1d1e1f;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.logo-area {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-img {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

.admin-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none;
}

/* 自定义滚动条样式 */
.admin-menu::-webkit-scrollbar {
  width: 6px;
}

.admin-menu::-webkit-scrollbar-track {
  background: #1d1e1f;
}

.admin-menu::-webkit-scrollbar-thumb {
  background: #4c4d4f;
  border-radius: 3px;
}

.admin-menu::-webkit-scrollbar-thumb:hover {
  background: #5c5d5f;
}

.app-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: #333;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  /* 侧边栏收窄 */
  .admin-aside {
    width: 160px !important;
    max-width: 160px;
    flex-shrink: 0;
  }

  .logo-area {
    height: 52px;
    padding: 0 10px;
    gap: 8px;
    font-size: 14px;
    letter-spacing: 0;
  }

  .logo-img {
    width: 28px;
    height: 28px;
    border-radius: 6px;
  }

  /* 菜单项：图标在上、文字在下，避免窄屏横向溢出 */
  .admin-menu :deep(.el-menu-item) {
    height: auto;
    min-height: 46px;
    padding: 6px 4px !important;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
  }

  .admin-menu :deep(.el-menu-item span) {
    font-size: 11px;
    line-height: 1.2;
    text-align: center;
  }

  /* 顶栏：面包屑省略，用户名隐藏 */
  .app-header {
    padding: 0 10px;
    height: 50px;
  }

  .app-header :deep(.el-breadcrumb) {
    white-space: nowrap;
    overflow: hidden;
  }

  .header-right {
    gap: 6px;
  }

  .username {
    display: none;
  }

  .app-main {
    padding: 10px;
  }
}
</style>

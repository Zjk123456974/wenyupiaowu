<template>
  <el-container style="height: 100vh">
    <el-aside width="224px" class="op-aside">
      <div class="logo-area" @click="router.push('/operator/dashboard')">
        <img src="/logo.jpg" class="logo-img" alt="logo" />
        <div class="logo-text">
          <span class="logo-name">运营管理中心</span>
          <span class="logo-sub">OPERATION CENTER</span>
        </div>
      </div>

      <el-scrollbar class="aside-scroll">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="transparent"
          text-color="#c3cbd9"
          active-text-color="#fff"
          class="op-menu"
        >
          <div class="menu-group-title">运营概览</div>
          <el-menu-item index="/operator/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/operator/stats">
            <el-icon><TrendCharts /></el-icon>
            <span>数据统计</span>
          </el-menu-item>

          <div class="menu-group-title">演出运营</div>
          <el-menu-item index="/operator/activity">
            <el-icon><Calendar /></el-icon>
            <span>活动管理</span>
          </el-menu-item>
          <el-menu-item index="/operator/activity/add">
            <el-icon><CirclePlus /></el-icon>
            <span>发布新活动</span>
          </el-menu-item>

          <div class="menu-group-title">订单与售后</div>
          <el-menu-item index="/operator/orders">
            <el-icon><Document /></el-icon>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/operator/refund">
            <el-icon><List /></el-icon>
            <span>退款审核</span>
          </el-menu-item>
          <el-menu-item index="/operator/messages">
            <el-icon><Bell /></el-icon>
            <span>消息中心</span>
          </el-menu-item>

          <!-- 现场核销：仅活动运营可见，普通运营保持原有功能不变 -->
          <template v-if="userStore.isActivityOperator">
            <div class="menu-group-title">现场核销</div>
            <el-menu-item index="/operator/ticket-check">
              <el-icon><Tickets /></el-icon>
              <span>电子票核销</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="返回前台" placement="bottom">
            <span class="header-btn" @click="router.push('/')">
              <el-icon :size="17"><Shop /></el-icon>
            </span>
          </el-tooltip>
          <el-tooltip content="重新加载" placement="bottom">
            <span class="header-btn" @click="refreshPage">
              <el-icon :size="17"><Refresh /></el-icon>
            </span>
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-chip">
              <el-avatar :size="30" class="user-avatar">
                {{ (userStore.nickname || userStore.username || 'U')[0] }}
              </el-avatar>
              <span class="username">{{ userStore.nickname || userStore.username }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-tag size="small" type="danger" effect="plain">
                    {{ userStore.isActivityOperator ? '活动运营' : '运营人员' }}
                  </el-tag>
                </el-dropdown-item>
                <el-dropdown-item command="home">
                  <el-icon><HomeFilled /></el-icon>回到前台
                </el-dropdown-item>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人资料
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import {
  DataBoard, TrendCharts, Calendar, CirclePlus,
  Shop, Refresh, ArrowDown, HomeFilled, User, SwitchButton, Tickets,
  Document, List, Bell
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/operator/activity') && p !== '/operator/activity') {
    return '/operator/activity'
  }
  return p
})

const breadcrumbs = computed(() => {
  const map = {
    '/operator/dashboard': '仪表盘',
    '/operator/stats': '数据统计',
    '/operator/activity': '活动管理',
    '/operator/activity/add': '发布新活动',
    '/operator/orders': '订单管理',
    '/operator/refund': '退款审核',
    '/operator/messages': '消息中心',
    '/operator/ticket-check': '电子票核销',
    '/operator/stats': '数据统计'
  }
  const title = map[route.path]
  const crumbs = [{ title: '运营中心' }]
  if (route.path.startsWith('/operator/activity/edit')) {
    crumbs.push({ title: '活动管理' })
    crumbs.push({ title: '编辑活动' })
  } else if (title) {
    crumbs.push({ title })
  }
  return crumbs
})

const refreshPage = () => {
  window.location.reload()
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'home') {
    router.push('/')
  } else if (command === 'profile') {
    router.push('/user/center/profile')
  }
}
</script>

<style scoped>
.op-aside {
  background: linear-gradient(180deg, #202d4f 0%, #1a2439 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logo-img {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.logo-name {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

.logo-sub {
  color: rgba(255, 255, 255, 0.45);
  font-size: 10px;
  letter-spacing: 1px;
}

.aside-scroll {
  flex: 1;
}

.op-menu {
  border-right: none;
  padding: 8px 0;
}

.menu-group-title {
  padding: 16px 22px 6px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 2px;
}

.op-menu :deep(.el-menu-item) {
  height: 46px;
  margin: 2px 10px;
  border-radius: 10px;
}

.op-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.op-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #3a6ff0, #5a8cff);
  color: #fff;
  box-shadow: 0 6px 14px rgba(58, 111, 240, 0.35);
}

.app-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  z-index: 10;
}

.header-left :deep(.el-breadcrumb__inner) {
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-btn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.header-btn:hover {
  background: #f0f2f5;
  color: #409eff;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  transition: background 0.2s ease;
}

.user-chip:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
}

.username {
  font-size: 14px;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  font-size: 12px;
  color: #909399;
}

.app-main {
  background: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  /* 侧边栏收窄，菜单文字改为图标+紧凑排列 */
  .op-aside {
    width: 160px !important;
    max-width: 160px;
    flex-shrink: 0;
  }

  .logo-area {
    height: 56px;
    padding: 0 10px;
    gap: 8px;
  }

  .logo-img {
    width: 30px;
    height: 30px;
    border-radius: 8px;
  }

  .logo-name {
    font-size: 13px;
    letter-spacing: 0;
  }

  .logo-sub {
    display: none;
  }

  .menu-group-title {
    padding: 12px 12px 4px;
    font-size: 10px;
    letter-spacing: 1px;
  }

  /* 菜单项：图标在上、文字在下，横向空间不足时不再溢出 */
  .op-menu :deep(.el-menu-item) {
    height: auto;
    min-height: 46px;
    margin: 2px 6px;
    padding: 6px 4px !important;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
  }

  .op-menu :deep(.el-menu-item span) {
    font-size: 11px;
    line-height: 1.2;
    text-align: center;
  }

  /* 顶栏：面包屑省略，右侧操作区压缩 */
  .app-header {
    padding: 0 10px;
    height: 50px;
  }

  .header-left {
    min-width: 0;
    overflow: hidden;
  }

  .header-left :deep(.el-breadcrumb) {
    white-space: nowrap;
  }

  .header-right {
    gap: 8px;
  }

  .username {
    display: none;
  }

  /* 内容区：减少内边距，避免表格被挤压 */
  .app-main {
    padding: 10px;
  }
}
</style>

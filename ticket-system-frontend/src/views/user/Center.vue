<template>
  <el-container class="user-center">
    <el-aside width="220px" class="center-aside">
      <div class="user-card">
        <el-avatar :size="64" :src="userStore.avatar || defaultAvatar" />
        <div class="user-name">{{ userStore.nickname || userStore.username }}</div>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="center-menu"
        router
      >
        <el-menu-item index="/user/center/profile">
          <el-icon><User /></el-icon>
          <span>个人信息</span>
        </el-menu-item>
        <el-menu-item index="/user/center/orders">
          <el-icon><List /></el-icon>
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="/user/center/tickets">
          <el-icon><Ticket /></el-icon>
          <span>我的电子票</span>
        </el-menu-item>
        <el-menu-item index="/user/center/reservations">
          <el-icon><Calendar /></el-icon>
          <span>我的预约</span>
        </el-menu-item>
        <el-menu-item index="/user/center/waitlist">
          <el-icon><Connection /></el-icon>
          <span>候补排队</span>
        </el-menu-item>
        <el-menu-item index="/user/center/refunds">
          <el-icon><RefreshLeft /></el-icon>
          <span>退票记录</span>
        </el-menu-item>
        <el-menu-item index="/user/center/favorites">
          <el-icon><Star /></el-icon>
          <span>我的收藏</span>
        </el-menu-item>
        <el-menu-item index="/user/center/attendees">
          <el-icon><Postcard /></el-icon>
          <span>观演人管理</span>
        </el-menu-item>
        <el-menu-item index="/user/center/my-reviews">
          <el-icon><ChatDotRound /></el-icon>
          <span>我的评价</span>
        </el-menu-item>
        <el-menu-item index="/user/center/consumption">
          <el-icon><Wallet /></el-icon>
          <span>消费记录</span>
        </el-menu-item>
        <el-menu-item index="/announcements">
          <el-icon><Bell /></el-icon>
          <span>系统公告</span>
        </el-menu-item>
        <el-menu-item index="/user/center/messages">
          <el-icon><Bell /></el-icon>
          <span>消息通知</span>
        </el-menu-item>
        <el-menu-item index="/user/center/password">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-menu-item>
      </el-menu>
      <!-- 运营/管理后台入口：从个人中心一键返回对应的后台工作台，避免“回不去后台” -->
      <div v-if="userStore.role === 1 || userStore.role === 2" class="back-admin-wrapper">
        <el-button type="primary" plain style="width: 100%" @click="backToBackend">
          <el-icon><Management /></el-icon>
          {{ userStore.role === 1 ? '返回运营后台' : '返回管理后台' }}
        </el-button>
      </div>
      <div class="logout-wrapper">
        <el-button type="danger" plain style="width: 100%" @click="handleLogout">退出登录</el-button>
      </div>
    </el-aside>
    <el-main class="center-main">
      <div class="statistics-row">
        <div class="stat-card stat-reservation">
          <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ statistics.reservationCount }}</div>
            <div class="stat-label">累计预约数</div>
          </div>
        </div>
        <div class="stat-card stat-ticket">
          <div class="stat-icon"><el-icon><Ticket /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ statistics.ticketCount }}</div>
            <div class="stat-label">购票数</div>
          </div>
        </div>
        <div class="stat-card stat-order">
          <div class="stat-icon"><el-icon><List /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ statistics.orderCount }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </div>
      </div>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserCenterStatistics } from '@/api/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Management } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const activeMenu = computed(() => route.path)

const statistics = ref({
  reservationCount: 0,
  ticketCount: 0,
  orderCount: 0
})

const fetchStatistics = async () => {
  try {
    const res = await getUserCenterStatistics()
    statistics.value = res.data
  } catch {
    // 静默处理
  }
}

// 一键返回运营/管理后台工作台（解决从个人中心“回不去后台”的问题）
const backToBackend = () => {
  if (userStore.role === 2) {
    router.push('/admin/dashboard')
  } else {
    router.push('/operator/dashboard')
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 取消退出
  }
}

onMounted(() => {
  fetchStatistics()
})
</script>

<style scoped>
.user-center {
  min-height: calc(100vh - 60px);
}

.center-aside {
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #f8faff, #fff);
}

.user-name {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.center-menu {
  border-right: none;
  flex: 1;
}

.center-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.center-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, #fff0f5, #ecf5ff);
  color: #ff4d6d;
  font-weight: 600;
}

.center-menu .el-menu-item:hover {
  background: #f5f7fa;
}

.logout-wrapper {
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.back-admin-wrapper {
  padding: 12px 16px 0;
}

.back-admin-wrapper .el-button {
  justify-content: center;
}

.center-main {
  background: #f5f7fa;
  padding: 20px;
}

.statistics-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 18px rgba(31, 92, 170, 0.08);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  overflow: hidden;
  position: relative;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  opacity: 0.08;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(31, 92, 170, 0.14);
}

.stat-reservation {
  background: linear-gradient(135deg, #ecf5ff, #e8f3ff);
}
.stat-reservation::before { background: #409eff; }
.stat-reservation .stat-icon { color: #409eff; background: rgba(64, 158, 255, 0.12); }

.stat-ticket {
  background: linear-gradient(135deg, #fff7f0, #fff3f5);
}
.stat-ticket::before { background: #ff7a45; }
.stat-ticket .stat-icon { color: #ff7a45; background: rgba(255, 122, 69, 0.12); }

.stat-order {
  background: linear-gradient(135deg, #f0fff4, #ecfff0);
}
.stat-order::before { background: #67c23a; }
.stat-order .stat-icon { color: #67c23a; background: rgba(103, 194, 58, 0.12); }

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  z-index: 1;
}

.stat-body {
  z-index: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
  color: #1b2a4a;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

/* ===== 移动端响应式适配（<=768px） ===== */
@media (max-width: 768px) {
  /* 侧栏改为顶部横向可滑动菜单，避免 220px 固定宽度把内容区挤没 */
  .user-center {
    flex-direction: column;
  }

  .center-aside {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }

  .user-card {
    flex-direction: row;
    justify-content: flex-start;
    gap: 12px;
    padding: 12px 16px;
  }

  .user-name {
    margin-top: 0;
    font-size: 15px;
  }

  .center-menu {
    display: flex;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 4px 8px;
    -webkit-overflow-scrolling: touch;
  }

  .center-menu::-webkit-scrollbar {
    display: none;
  }

  .center-menu :deep(.el-menu-item) {
    flex: 0 0 auto;
    height: 38px;
    line-height: 38px;
    margin: 0 4px;
    padding: 0 12px;
    font-size: 13px;
    white-space: nowrap;
  }

  .back-admin-wrapper,
  .logout-wrapper {
    padding: 8px 16px 12px;
  }

  .center-main {
    padding: 12px;
  }

  .statistics-row {
    gap: 10px;
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 12px;
    gap: 10px;
  }

  .stat-icon {
    width: 36px;
    height: 36px;
    font-size: 18px;
    border-radius: 10px;
  }

  .stat-value {
    font-size: 20px;
  }

  .stat-label {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  /* 极窄屏统计卡片改为单列，数值不被压扁 */
  .statistics-row {
    flex-direction: column;
  }
}
</style>

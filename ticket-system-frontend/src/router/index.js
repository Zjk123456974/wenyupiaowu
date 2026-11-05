import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/UserLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'activity/list', name: 'ActivityList', component: () => import('@/views/ActivityList.vue') },
      { path: 'activity/:id', name: 'ActivityDetail', component: () => import('@/views/ActivityDetail.vue') },
      { path: 'announcements', name: 'Announcements', component: () => import('@/views/user/Announcements.vue') },
      {
        path: 'user/center',
        name: 'UserCenter',
        component: () => import('@/views/user/Center.vue'),
        redirect: '/user/center/profile',
        children: [
          { path: 'profile', name: 'Profile', component: () => import('@/views/user/Profile.vue') },
          { path: 'orders', name: 'Orders', component: () => import('@/views/user/Orders.vue') },
          { path: 'reservations', name: 'Reservations', component: () => import('@/views/user/Reservations.vue') },
          { path: 'waitlist', name: 'Waitlist', component: () => import('@/views/user/Waitlist.vue') },
          { path: 'favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue') },
          { path: 'attendees', name: 'Attendees', component: () => import('@/views/user/Attendees.vue') },
          { path: 'messages', name: 'Messages', component: () => import('@/views/user/Messages.vue') },
          { path: 'password', name: 'ChangePassword', component: () => import('@/views/user/ChangePassword.vue') },
          { path: 'tickets', name: 'MyTickets', component: () => import('@/views/user/MyTickets.vue') },
          { path: 'refunds', name: 'RefundApply', component: () => import('@/views/user/RefundApply.vue') },
          { path: 'my-reviews', name: 'MyReviews', component: () => import('@/views/user/MyReviews.vue') },
          { path: 'consumption', name: 'Consumption', component: () => import('@/views/user/Consumption.vue') }
        ]
      }
    ]
  },
  {
    path: '/operator',
    component: () => import('@/layout/OperatorLayout.vue'),
    meta: { role: 1 },
    children: [
      { path: 'dashboard', name: 'OperatorDashboard', component: () => import('@/views/operator/Dashboard.vue') },
      { path: 'activity', name: 'OperatorActivity', component: () => import('@/views/operator/ActivityManage.vue') },
      { path: 'activity/add', name: 'OperatorActivityAdd', component: () => import('@/views/operator/ActivityForm.vue') },
      { path: 'activity/edit/:id', name: 'OperatorActivityEdit', component: () => import('@/views/operator/ActivityForm.vue') },
      { path: 'stats', name: 'OperatorStats', component: () => import('@/views/operator/Stats.vue') },
      { path: 'orders', name: 'OperatorOrders', component: () => import('@/views/admin/OrderManage.vue') },
      { path: 'refund', name: 'OperatorRefund', component: () => import('@/views/admin/RefundManage.vue') },
      { path: 'messages', name: 'OperatorMessages', component: () => import('@/views/user/Messages.vue') },
      { path: 'ticket-check', name: 'OperatorTicketCheck', component: () => import('@/views/admin/TicketCheck.vue'), meta: { role: 1, activityOperatorOnly: true } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { role: 2 },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'activities', name: 'AdminActivities', component: () => import('@/views/admin/ActivityAudit.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('@/views/admin/OrderManage.vue') },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/CategoryManage.vue') },
      { path: 'banners', name: 'AdminBanners', component: () => import('@/views/admin/BannerManage.vue') },
      { path: 'announcements', name: 'AdminAnnouncements', component: () => import('@/views/admin/AnnouncementManage.vue') },
      { path: 'stats', name: 'AdminStats', component: () => import('@/views/admin/Stats.vue') },
      { path: 'finance', name: 'AdminFinance', component: () => import('@/views/admin/Finance.vue') },
      { path: 'ticket-check', name: 'AdminTicketCheck', component: () => import('@/views/admin/TicketCheck.vue'), meta: { role: 2 } },
      { path: 'refund', name: 'AdminRefund', component: () => import('@/views/admin/RefundManage.vue') },
      { path: 'review', name: 'AdminReview', component: () => import('@/views/admin/ReviewManage.vue') },
      { path: 'tag', name: 'AdminTag', component: () => import('@/views/admin/TagManage.vue') },
      { path: 'log', name: 'AdminLog', component: () => import('@/views/admin/LogManage.vue') },
      { path: 'ad', name: 'AdminAd', component: () => import('@/views/admin/AdManage.vue') },
      { path: 'config', name: 'AdminConfig', component: () => import('@/views/admin/SystemConfig.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login' || to.path === '/register') {
    next()
    return
  }
  if (to.meta.role !== undefined) {
    if (!userStore.token) {
      next('/login')
      return
    }
    if (to.meta.role === 1 && userStore.role !== 1 && userStore.role !== 2) {
      next('/')
      return
    }
    if (to.meta.role === 2 && userStore.role !== 2) {
      next('/')
      return
    }
    // 电子票核销端：仅活动运营（role=1 且 operatorType=1）可使用，普通运营无核销权限
    if (to.meta.activityOperatorOnly && userStore.role === 1 && userStore.operatorType !== 1) {
      next('/operator/dashboard')
      return
    }
  }
  next()
})

export default router

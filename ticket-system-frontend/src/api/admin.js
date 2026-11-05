import request from '@/utils/request'

// 用户管理
export function getUserPage(params) {
  return request.get('/admin/user/page', { params })
}

export function updateUser(id, data) {
  return request.put(`/admin/user/${id}`, data)
}

export function banUser(id, banReason) {
  // 同时用 params 与 JSON body 传递，避免 data 为 null 时被浏览器识别成表单编码
  return request.put(`/admin/user/${id}/ban`, { banReason }, { params: { banReason } })
}

export function unbanUser(id) {
  return request.put(`/admin/user/${id}/unban`, {})
}

export function deleteUser(id) {
  return request.delete(`/admin/user/${id}`)
}

export function registerOperator(data) {
  return request.post('/admin/user/operator', data)
}

export function reviewOperator(id, approved) {
  // 同时用 params 与 JSON body 传递，避免 data 为 null 时被浏览器识别成表单编码
  return request.put(`/admin/user/operator/${id}/review`, { approved }, { params: { approved } })
}

// 活动审核
export function getAdminActivities(params) {
  return request.get('/admin/activity/page', { params })
}

export function auditActivity(id, data) {
  return request.put(`/admin/activity/${id}/audit`, data)
}

export function updateAdminActivity(id, data) {
  return request.put(`/admin/activity/${id}`, data)
}

export function forceOffline(id) {
  return request.put(`/admin/activity/${id}/force-offline`)
}

export function getStockInfo(id) {
  return request.get(`/admin/activity/${id}/stock`)
}

// 订单管理
export function getAdminOrders(params) {
  return request.get('/admin/order/page', { params })
}

export function handleAbnormalOrder(id, data) {
  return request.post(`/admin/order/${id}/handle`, data)
}

export function getHandleRecords(orderId) {
  return request.get(`/admin/order/${orderId}/handle-records`)
}

// 分类管理
export function getCategories() {
  return request.get('/category/list')
}

export function addCategory(data) {
  return request.post('/admin/category', data)
}

export function updateCategory(id, data) {
  return request.put(`/admin/category/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/admin/category/${id}`)
}

// 公告管理
export function getAdminAnnouncements(params) {
  return request.get('/admin/announcement/page', { params })
}

export function addAnnouncement(data) {
  return request.post('/admin/announcement', data)
}

export function updateAnnouncement(id, data) {
  return request.put(`/admin/announcement/${id}`, data)
}

export function deleteAnnouncement(id) {
  return request.delete(`/admin/announcement/${id}`)
}

export function topAnnouncement(id) {
  return request.put(`/admin/announcement/${id}/top`)
}

export function offlineAnnouncement(id) {
  return request.put(`/admin/announcement/${id}/offline`)
}

export function onlineAnnouncement(id) {
  return request.put(`/admin/announcement/${id}/online`)
}

// 数据统计
export function getOverview() {
  return request.get('/admin/stats/overview')
}

export function getHotActivitiesStats() {
  return request.get('/admin/stats/hot-activities')
}

export function getMonthlyOrders() {
  return request.get('/admin/stats/monthly-orders')
}

export function getUserGrowth() {
  return request.get('/admin/stats/user-growth')
}

export function getCategoryDistribution() {
  return request.get('/admin/stats/category-distribution')
}

export function getTicketUtilization() {
  return request.get('/admin/stats/ticket-utilization')
}

export function getDailyOrders() {
  return request.get('/admin/stats/daily-orders')
}

export function getWeeklyOrders() {
  return request.get('/admin/stats/weekly-orders')
}

export function getSummary() {
  return request.get('/admin/stats/summary')
}

// 轮播图管理
export function getBanners(params) {
  return request.get('/banner/admin/list', { params })
}

export function addBanner(data) {
  return request.post('/banner', data)
}

export function updateBanner(id, data) {
  return request.put(`/banner/${id}`, data)
}

export function deleteBanner(id) {
  return request.delete(`/banner/${id}`)
}

export function updateBannerStatus(id, status) {
  return request.put(`/banner/${id}/status`, { status })
}

import request from '@/utils/request'

export function addActivity(data) {
  return request.post('/operator/activity', data)
}

export function updateActivity(id, data) {
  return request.put(`/operator/activity/${id}`, data)
}

export function applyActivity(id) {
  return request.post(`/operator/activity/${id}/apply`)
}

export function offlineActivity(id) {
  return request.post(`/operator/activity/${id}/offline`)
}

export function getMyActivities(params) {
  return request.get('/operator/activity/list', { params })
}

export function getOperatorActivityDetail(id) {
  return request.get(`/operator/activity/${id}`)
}

export function updateTicketConfig(id, data) {
  return request.put(`/operator/activity/${id}/ticket-config`, data)
}

// 读取活动已配置的票档（供运营编辑表单回显）
export function getOperatorActivityTiers(activityId) {
  return request.get(`/ticket-price/activity/${activityId}`)
}

// 整单保存活动票档（新增/修改/删除一次完成，后端同步活动总库存与Redis）
export function saveActivityTiers(activityId, data) {
  return request.put(`/operator/activity/${activityId}/tiers`, data)
}

// 保存活动演出阵容 / 嘉宾（全量替换，运营后台可配置）
export function saveActivityLineup(activityId, data) {
  return request.put(`/operator/activity/${activityId}/lineup`, data)
}

// 读取活动已配置的演出阵容（运营编辑页回显）
export function getActivityLineup(activityId) {
  return request.get(`/activity/${activityId}/lineup`)
}

export function getActivityStats(activityId) {
  return request.get(`/operator/stats/activity/${activityId}`)
}

export function getActivityHotStats(activityId) {
  return request.get(`/operator/stats/activity/${activityId}/hot`)
}

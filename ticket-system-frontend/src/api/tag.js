import request from '@/utils/request'

// ========== 标签管理 ==========

export function getTagList() {
  return request.get('/tag/list')
}

export function getEnabledTags() {
  return request.get('/tag/enabled')
}

export function addTag(data) {
  return request.post('/tag', data)
}

export function updateTag(id, data) {
  return request.put(`/tag/${id}`, data)
}

export function deleteTag(id) {
  return request.delete(`/tag/${id}`)
}

export function bindActivityTags(activityId, tagIds) {
  return request.post(`/tag/bind/${activityId}`, tagIds)
}

export function getActivityTags(activityId) {
  return request.get(`/tag/activity/${activityId}`)
}

export function getTagStatistics() {
  return request.get('/tag/statistics')
}

import request from '@/utils/request'

// ========== 评价管理 ==========

export function addReview(data) {
  return request.post('/review/add', data)
}

export function getPublicReviews(activityId) {
  return request.get(`/review/public/${activityId}`)
}

export function getReviewStatistics(activityId) {
  return request.get(`/review/statistics/${activityId}`)
}

export function getMyReviews(params) {
  return request.get('/review/my', { params })
}

export function deleteMyReview(id) {
  return request.delete(`/review/my/${id}`)
}

export function getPendingReviews() {
  return request.get('/review/pending')
}

export function getReviewPage(params) {
  return request.get('/review/admin/page', { params })
}

export function approveReview(id) {
  return request.put(`/review/admin/${id}/approve`)
}

export function rejectReview(id) {
  return request.put(`/review/admin/${id}/reject`)
}

export function hideReview(id) {
  return request.put(`/review/admin/${id}/hide`)
}

export function deleteReview(id) {
  return request.delete(`/review/admin/${id}`)
}

export function replyReview(id, data) {
  return request.put(`/review/admin/${id}/reply`, data)
}

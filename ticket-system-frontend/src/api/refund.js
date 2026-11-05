import request from '@/utils/request'

// ========== 退票工单 ==========

export function applyRefund(data) {
  return request.post('/refund/apply', data)
}

export function getMyRefunds() {
  return request.get('/refund/my-refunds')
}

export function previewRefund(params) {
  return request.get('/refund/preview', { params })
}

export function getRefundPage(params) {
  return request.get('/refund/admin/page', { params })
}

export function approveRefund(id, data) {
  return request.put(`/refund/admin/${id}/approve`, data)
}

export function rejectRefund(id, data) {
  return request.put(`/refund/admin/${id}/reject`, data)
}

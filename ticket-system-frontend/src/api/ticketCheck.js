import request from '@/utils/request'

// ========== 电子票核销 ==========

export function getMyTickets() {
  return request.get('/ticket-check/my-tickets')
}

export function getTicketDetail(id) {
  return request.get(`/ticket-check/detail/${id}`)
}

export function verifyTicket(data) {
  return request.post('/ticket-check/verify', data)
}

export function getCheckRecords(params) {
  return request.get('/ticket-check/records', { params })
}

export function getCheckStatistics(activityId) {
  return request.get('/ticket-check/statistics', { params: { activityId } })
}

// 当前登录人可核销的活动（管理员=全部，活动运营=仅本人负责）
export function getMyCheckActivities() {
  return request.get('/ticket-check/my-activities')
}

import request from '@/utils/request'

export function applyReservation(data) {
  return request.post('/reservation/apply', data)
}

export function getMyReservations(params) {
  return request.get('/reservation/list', { params })
}

export function getReservationDetail(activityId) {
  return request.get(`/reservation/detail/${activityId}`)
}

export function cancelReservation(activityId) {
  return request.delete(`/reservation/cancel/${activityId}`)
}

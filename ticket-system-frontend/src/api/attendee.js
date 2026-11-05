import request from '@/utils/request'

export function getAttendeeList() {
  return request.get('/attendee/list')
}

export function getAttendeePage(params) {
  return request.get('/attendee/page', { params })
}

export function addAttendee(data) {
  return request.post('/attendee', data)
}

export function updateAttendee(id, data) {
  return request.put(`/attendee/${id}`, data)
}

export function deleteAttendee(id) {
  return request.delete(`/attendee/${id}`)
}

export function setDefaultAttendee(id) {
  return request.put(`/attendee/${id}/default`)
}

export function checkDuplicate(activityId, idCard) {
  return request.get('/attendee/check-duplicate', { params: { activityId, idCard } })
}

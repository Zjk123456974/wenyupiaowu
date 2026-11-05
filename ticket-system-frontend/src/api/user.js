import request from '@/utils/request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

export function changePassword(data) {
  return request.put('/user/password', data)
}

export function resetPassword(data) {
  return request.post('/user/reset-password', data)
}

export function getUserStatus() {
  return request.get('/user/status')
}

export function getUserCenterStatistics() {
  return request.get('/user/center/statistics')
}

// ========== 消费记录 ==========

export function getConsumptionStats() {
  return request.get('/user/center/consumption/stats')
}

export function getConsumptionFlows(params) {
  return request.get('/user/center/consumption/flows', { params })
}

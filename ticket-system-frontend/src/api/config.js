import request from '@/utils/request'

export function getConfigList(params) {
  return request.get('/admin/config/list', { params })
}

export function updateConfig(id, data) {
  return request.put(`/admin/config/${id}`, data)
}

export function getConfigValue(key) {
  return request.get(`/admin/config/value/${key}`)
}

// 公开站点信息（无需登录，供用户端全站渲染站点名/Logo/客服电话/备案号）
export function getPublicConfig() {
  return request.get('/admin/config/public')
}

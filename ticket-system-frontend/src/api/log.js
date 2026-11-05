import request from '@/utils/request'

// ========== 日志管理 ==========

export function getOperationLogs(params) {
  return request.get('/admin/log/operation', { params })
}

export function getLoginLogs(params) {
  return request.get('/admin/log/login', { params })
}

export function exportOperationLogs(params) {
  return request.get('/admin/log/export/operation', { params, responseType: 'blob' })
}

export function exportLoginLogs(params) {
  return request.get('/admin/log/export/login', { params, responseType: 'blob' })
}

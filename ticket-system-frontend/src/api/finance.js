import request from '@/utils/request'

export function getFinanceSummary(params) {
  return request.get('/admin/finance/summary', { params })
}

export function getDailyReconciliation() {
  return request.get('/admin/finance/daily')
}

export function getFinanceFlows(params) {
  return request.get('/admin/finance/flows', { params })
}

export function exportOrders(params) {
  return request.get('/admin/finance/export/orders', {
    params,
    responseType: 'blob'
  })
}

export function exportUsers() {
  return request.get('/admin/finance/export/users', {
    responseType: 'blob'
  })
}

export function exportActivities() {
  return request.get('/admin/finance/export/activities', {
    responseType: 'blob'
  })
}

export function exportFinance(params) {
  return request.get('/admin/finance/export/finance', {
    params,
    responseType: 'blob'
  })
}

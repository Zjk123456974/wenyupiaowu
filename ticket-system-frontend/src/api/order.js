import request from '@/utils/request'

export function getMyOrders(params) {
  return request.get('/order/list', { params })
}

export function getOrderDetail(id) {
  return request.get(`/order/detail/${id}`)
}

export function payOrder(id) {
  return request.post(`/order/pay/${id}`)
}

export function cancelOrder(id) {
  return request.post(`/order/cancel/${id}`)
}

export function getElectronicTicket(id) {
  return request.get(`/order/ticket/${id}`)
}

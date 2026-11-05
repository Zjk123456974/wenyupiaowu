import request from '@/utils/request'

// ========== 管理端 ==========
export function getAdPage(params) { return request.get('/ad/page', { params }) }
export function addAd(data) { return request.post('/ad', data) }
export function updateAd(id, data) { return request.put(`/ad/${id}`, data) }
export function deleteAd(id) { return request.delete(`/ad/${id}`) }

// ========== 用户端 ==========
export function getActiveAds(position) {
  return request.get('/ad/active', { params: { position } })
}

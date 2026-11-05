import request from '@/utils/request'

export function addFavorite(activityId) {
  return request.post(`/user/center/favorite/${activityId}`)
}

export function removeFavorite(activityId) {
  return request.delete(`/user/center/favorite/${activityId}`)
}

export function getMyFavorites(params) {
  return request.get('/user/center/favorites', { params })
}

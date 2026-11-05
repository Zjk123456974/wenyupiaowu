import request from '@/utils/request'

export function getActivityList(params) {
  return request.get('/activity/list', { params })
}

export function getActivityDetail(id) {
  return request.get(`/activity/detail/${id}`)
}

export function getHotActivities() {
  return request.get('/activity/hot')
}

// 个性化推荐：猜你喜欢（基于想看/收藏/已购分类偏好，对标大麦）
export function getRecommendActivities(size = 8) {
  return request.get('/activity/recommend', { params: { size } })
}

export function getHomeBanners() {
  return request.get('/banner/list')
}

// 想看 / 取消想看（对标大麦“X人想看”）
export function toggleWish(id) {
  return request.post(`/activity/${id}/wish`)
}

// 查询当前用户是否已想看
export function getMyWish(id) {
  return request.get(`/activity/${id}/wish/me`)
}

// 到货提醒开关（独立于收藏，避免污染收藏夹）
export function toggleNotify(id) {
  return request.post(`/activity/${id}/notify`)
}

// 查询当前用户是否已开启到货提醒
export function getMyNotify(id) {
  return request.get(`/activity/${id}/notify/me`)
}

// 公开：查询某活动的演出阵容 / 嘉宾列表（详情页「演出阵容」模块）
export function getActivityLineup(id) {
  return request.get(`/activity/${id}/lineup`)
}


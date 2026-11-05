import request from '@/utils/request'

/**
 * 加入候补补票队列
 */
export function joinWaitQueue(activityId) {
  return request.post('/reservation/wait/join', null, { params: { activityId } })
}

/**
 * 退出候补补票队列
 */
export function leaveWaitQueue(activityId) {
  return request.delete(`/reservation/wait/leave/${activityId}`)
}

/**
 * 查询某活动候补队列中我的位置
 */
export function getWaitPosition(activityId) {
  return request.get(`/reservation/wait/position/${activityId}`)
}

/**
 * 我的候补列表
 */
export function getMyWaitList(params) {
  return request.get('/reservation/wait/list', { params })
}

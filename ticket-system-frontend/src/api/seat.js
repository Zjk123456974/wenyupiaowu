import request from '@/utils/request'

/**
 * 获取活动座位图（对标大麦「在线选座」）
 * 返回：{ seatMapEnabled, layout, occupied:[座位编码], locked:[座位编码] }
 * layout 为 JSON 字符串：{ sections:[{ id, name, rows, cols, price }] }
 */
export function getSeatMap(activityId) {
  return request.get('/seat/map', { params: { activityId } })
}

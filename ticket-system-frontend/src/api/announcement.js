import request from '@/utils/request'

export function getAnnouncements(params) {
  return request.get('/announcement/list', { params })
}

export function getAnnouncementDetail(id) {
  return request.get(`/announcement/detail/${id}`)
}

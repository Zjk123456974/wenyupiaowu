import request from '@/utils/request'

export function getCategoryList() {
  return request.get('/category/list')
}
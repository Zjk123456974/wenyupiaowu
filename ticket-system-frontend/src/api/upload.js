import request from '@/utils/request'

export function uploadImage(formData) {
  return request.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}

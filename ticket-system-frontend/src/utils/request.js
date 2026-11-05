import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

function handleUnauthorized() {
  try {
    useUserStore().logout()
  } catch (e) {
    localStorage.removeItem('token')
  }
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    // 处理blob文件下载响应
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      // grouping：同一时刻多个请求报同样的错时合并成一条，避免弹窗刷屏
      ElMessage.error({ message: res.message || '请求失败', grouping: true })
      if (res.code === 401) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        handleUnauthorized()
        ElMessage.error({ message: '登录已过期，请重新登录', grouping: true })
      } else {
        ElMessage.error({ message: error.response.data?.message || '请求失败', grouping: true })
      }
    } else {
      ElMessage.error({ message: '网络错误，请检查网络连接', grouping: true })
    }
    return Promise.reject(error)
  }
)

export default request

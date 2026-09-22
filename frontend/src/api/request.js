import axios from 'axios'
import { clearUser, getToken } from '../auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // 文件下载（blob）不遵循统一的 Result 包装，直接返回原始数据
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const result = response.data
    if (result.code !== 200) {
      ElMessage.error(result.message || '操作失败')
      return Promise.reject(new Error(result.message))
    }
    return result.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (status === 401) {
      clearUser()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request

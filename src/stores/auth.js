import { defineStore } from 'pinia'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'

// 禁用 NProgress spinner
NProgress.configure({ showSpinner: false })

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// ===== 创建 Axios 实例 =====
const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// ===== 请求拦截器：附加 JWT Token =====
apiClient.interceptors.request.use(
  (config) => {
    NProgress.start()
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// ===== 响应拦截器：统一处理错误 =====
// 注意：成功响应直接返回 response.data（即后端 { code, message, data }）
apiClient.interceptors.response.use(
  (response) => {
    NProgress.done()
    return response.data  // res = { code, message, data }
  },
  async (error) => {
    NProgress.done()
    const { response, config } = error

    if (response) {
      const msg = response.data?.message || '请求失败'

      // 如果是业务层返回的 code 不为 200，也要显示错误
      if (response.data?.code && response.data.code !== 200) {
        ElMessage.error(msg)
        return Promise.reject(error)
      }

      switch (response.status) {
        case 401: {
          // 登录页的 401 直接提示，不跳转
          if (config.url?.includes('/auth/login')) {
            ElMessage.error(msg)
            return Promise.reject(error)
          }
          // 防止刷新失败后继续重试
          if (config._retry) {
            localStorage.clear()
            window.location.href = '/login'
            return Promise.reject(error)
          }
          // 尝试刷新 Token
          const refreshToken = localStorage.getItem('refreshToken')
          if (refreshToken) {
            config._retry = true
            try {
              const res = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken })
              const newToken = res.data?.data?.accessToken
              if (newToken) {
                localStorage.setItem('accessToken', newToken)
                config.headers['Authorization'] = `Bearer ${newToken}`
                return apiClient(config)
              }
            } catch {
              // 刷新失败，清除登录状态
            }
          }
          localStorage.clear()
          window.location.href = '/login'
          break
        }
        case 403:
          ElMessage.error('您没有权限执行此操作')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 429:
          ElMessage.error('请求过于频繁，请稍后再试')
          break
        case 409:
          // 冲突（如注册用户名重复），由页面处理
          return Promise.reject(error)
        default:
          ElMessage.error(msg)
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

// ===== Auth Store =====
export const useAuthStore = defineStore('auth', {
  state: () => ({
    userInfo: null,
    accessToken: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    userRole: localStorage.getItem('userRole') || null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    isAdmin: (state) => state.userRole === 'ADMIN',
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '',
  },

  actions: {
    /**
     * 登录
     * 后端返回: { code, message, data: { accessToken, refreshToken, role, userId, username, nickname } }
     * 拦截器已 unwrap: res = { code, message, data: {...} }
     */
    async login(username, password) {
      const res = await apiClient.post('/auth/login', { username, password })
      const tokenData = res.data

      this.accessToken = tokenData.accessToken
      this.refreshToken = tokenData.refreshToken
      this.userRole = tokenData.role
      this.userInfo = {
        id: tokenData.userId,
        username: tokenData.username,
        nickname: tokenData.nickname,
        role: tokenData.role,
      }
      localStorage.setItem('accessToken', tokenData.accessToken)
      localStorage.setItem('refreshToken', tokenData.refreshToken)
      localStorage.setItem('userRole', tokenData.role)

      return res
    },

    /**
     * 注册（返回 { code, message }）
     */
    async register(username, password, nickname) {
      const res = await apiClient.post('/auth/register', { username, password, nickname })
      return res
    },

    /** 获取当前用户信息 */
    async fetchCurrentUser() {
      try {
        const res = await apiClient.get('/auth/current')
        this.userInfo = res.data
        return res.data
      } catch {
        return null
      }
    },

    /** 登出 */
    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.userRole = null
      this.userInfo = null
      localStorage.clear()
    },

    /** 刷新Token */
    async refreshAccessToken() {
      try {
        const res = await axios.post(`${BASE_URL}/auth/refresh`, {
          refreshToken: this.refreshToken,
        })
        const newToken = res.data?.data?.accessToken
        if (newToken) {
          this.accessToken = newToken
          localStorage.setItem('accessToken', newToken)
          return newToken
        }
      } catch {
        this.logout()
        window.location.href = '/login'
      }
    },
  },
})

// ===== 导出封装的 API 实例 =====
export { apiClient }
export default apiClient

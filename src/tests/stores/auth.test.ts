import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock localStorage - 在 vi.mock 之前定义，这样 store 初始化时就能获取到
const storage = { accessToken: null, refreshToken: null, userRole: null }
Object.defineProperty(window, 'localStorage', {
  value: {
    getItem: vi.fn((key) => storage[key] ?? null),
    setItem: vi.fn((key, value) => { storage[key] = value }),
    removeItem: vi.fn((key) => { delete storage[key] }),
    clear: vi.fn(() => { Object.keys(storage).forEach(k => delete storage[k]) })
  },
  writable: true
})

// Mock axios
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      },
      post: vi.fn(),
      get: vi.fn()
    }))
  }
}))

// Mock NProgress
vi.mock('nprogress', () => ({
  default: {
    configure: vi.fn(),
    start: vi.fn(),
    done: vi.fn()
  }
}))

// 每个测试使用独立的 Pinia 实例，确保状态隔离
function createFreshStore() {
  const pinia = createPinia()
  setActivePinia(pinia)
  // 需要在 pinia 设置后才 import，否则 store 会在错误 pinia 下初始化
  // 使用 dynamic import 解决
  return null // 下面用动态 import
}

// 动态导入 store（确保在 pinia 设置之后）
async function getAuthStore() {
  const { useAuthStore } = await import('@/stores/auth')
  const pinia = createPinia()
  setActivePinia(pinia)
  return useAuthStore()
}

describe('Auth Store 测试', () => {
  beforeEach(() => {
    // 重置存储
    Object.keys(storage).forEach(k => delete storage[k])
    vi.clearAllMocks()
  })

  it('应正确设置accessToken', async () => {
    const authStore = await getAuthStore()
    authStore.accessToken = 'test-token'

    expect(authStore.accessToken).toBe('test-token')
    expect(authStore.isLoggedIn).toBe(true)
  })

  it('应正确设置用户信息', async () => {
    const authStore = await getAuthStore()
    authStore.userInfo = {
      id: 1,
      username: 'admin',
      nickname: '管理员',
      role: 'ADMIN'
    }

    expect(authStore.userInfo.id).toBe(1)
    expect(authStore.username).toBe('admin')
    expect(authStore.nickname).toBe('管理员')
    expect(authStore.userInfo.role).toBe('ADMIN')
  })

  it('应正确清除登录信息', async () => {
    const authStore = await getAuthStore()
    authStore.accessToken = 'test-token'
    authStore.userInfo = { id: 1, username: 'admin', role: 'ADMIN' }
    authStore.userRole = 'ADMIN'

    authStore.logout()

    expect(authStore.accessToken).toBeNull()
    expect(authStore.userInfo).toBeNull()
    expect(authStore.username).toBe('')
  })

  it('isLoggedIn应正确反映登录状态', async () => {
    const authStore = await getAuthStore()

    expect(authStore.isLoggedIn).toBe(false)

    authStore.accessToken = 'test-token'

    expect(authStore.isLoggedIn).toBe(true)
  })

  it('isAdmin应正确反映管理员身份', async () => {
    const authStore = await getAuthStore()

    expect(authStore.isAdmin).toBe(false)

    authStore.userRole = 'ADMIN'

    expect(authStore.isAdmin).toBe(true)
  })

  it('isAdmin对普通用户应返回false', async () => {
    const authStore = await getAuthStore()
    authStore.userRole = 'USER'

    expect(authStore.isAdmin).toBe(false)
  })

  it('应从localStorage恢复登录状态', async () => {
    // 预填充存储（模拟页面刷新时的状态）
    storage.accessToken = 'saved-token'
    storage.refreshToken = 'saved-refresh'
    storage.userRole = 'ADMIN'

    const authStore = await getAuthStore()

    expect(authStore.accessToken).toBe('saved-token')
    expect(authStore.isAdmin).toBe(true)
    expect(authStore.isLoggedIn).toBe(true)
  })

  it('应正确设置用户ID', async () => {
    const authStore = await getAuthStore()
    authStore.userInfo = { id: 42, username: 'test', role: 'USER' }

    expect(authStore.userInfo.id).toBe(42)
  })
})

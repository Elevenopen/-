import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

// ===== 路由懒加载（减少首屏体积） =====
const Login = () => import('@/views/login/index.vue')
const Register = () => import('@/views/register/index.vue')
const Layout = () => import('@/views/layout/index.vue')
const Dashboard = () => import('@/views/admin/dashboard/index.vue')
const TrafficPlan = () => import('@/views/admin/traffic-plan/index.vue')
const Monitor = () => import('@/views/admin/monitor/index.vue')
const NetworkStability = () => import('@/views/admin/network-stability/index.vue')
const DeviceUsage = () => import('@/views/admin/device-usage/index.vue')
const ServerPeak = () => import('@/views/admin/server-peak/index.vue')
const Announcement = () => import('@/views/admin/announcement/index.vue')
const NetworkAccess = () => import('@/views/admin/NetworkAccess.vue')
const AiAssistant = () => import('@/views/ai-assistant/index.vue')
const Statistics = () => import('@/views/admin/statistics/index.vue')
const UserHome = () => import('@/views/user/home.vue')
const AnnouncementList = () => import('@/views/user/announcement-list.vue')

// ===== 路由配置 =====
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册', requiresAuth: false },
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      // ===== 用户端路由 =====
      {
        path: 'user/home',
        name: 'UserHome',
        component: UserHome,
        meta: { title: '个人首页', roles: ['USER'] },
      },
      {
        path: 'announcements',
        name: 'AnnouncementList',
        component: AnnouncementList,
        meta: { title: '公告列表', roles: ['USER'] },
      },
      // ===== 管理员路由 =====
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '管理仪表盘', roles: ['ADMIN'] },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: Statistics,
        meta: { title: '数据统计', roles: ['ADMIN'] },
      },
      {
        path: 'traffic-plan',
        name: 'TrafficPlan',
        component: TrafficPlan,
        meta: { title: '流量规划管理', roles: ['ADMIN'] },
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: Monitor,
        meta: { title: '实时监控管理', roles: ['ADMIN'] },
      },
      {
        path: 'network-stability',
        name: 'NetworkStability',
        component: NetworkStability,
        meta: { title: '网络稳定性管理', roles: ['ADMIN'] },
      },
      {
        path: 'device-usage',
        name: 'DeviceUsage',
        component: DeviceUsage,
        meta: { title: '设备使用统计', roles: ['ADMIN'] },
      },
      {
        path: 'server-peak',
        name: 'ServerPeak',
        component: ServerPeak,
        meta: { title: '服务器峰值管理', roles: ['ADMIN'] },
      },
      {
        path: 'announcement',
        name: 'AnnouncementAdmin',
        component: Announcement,
        meta: { title: '公告管理', roles: ['ADMIN'] },
      },
      {
        path: 'network-access',
        name: 'NetworkAccess',
        component: NetworkAccess,
        meta: { title: '校园网接入管理', roles: ['ADMIN'] },
      },
      // ===== AI助手（所有登录用户）=====
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: AiAssistant,
        meta: { title: 'AI智能助手', roles: ['ADMIN', 'USER'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

// ===== 创建路由实例 =====
const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ===== 导航守卫 =====
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  const userRole = localStorage.getItem('userRole')

  if (to.meta.requiresAuth === false) {
    // 无需认证的页面（如登录页），已登录则跳转首页
    if (token && to.path === '/login') {
      return next(userRole === 'ADMIN' ? '/dashboard' : '/user/home')
    }
    return next()
  }

  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 角色权限校验
  if (to.meta.roles && !to.meta.roles.includes(userRole)) {
    return next(userRole === 'ADMIN' ? '/dashboard' : '/user/home')
  }

  // 设置页面标题
  document.title = `${to.meta.title || '页面'} - 网络流量管理系统`
  next()
})

export default router

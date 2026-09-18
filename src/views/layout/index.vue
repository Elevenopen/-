<template>
  <div class="app-layout">
    <!-- ===== 侧边栏 ===== -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <!-- Logo区域 -->
      <div class="logo-area">
        <img src="@/assets/logo.svg" alt="logo" class="logo-icon" />
        <span v-if="!isCollapse" class="logo-text">流量管理系统</span>
      </div>

      <!-- 导航菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        background-color="#1d1e23"
        text-color="#b1b3b9"
        active-text-color="#409EFF"
        class="sidebar-menu"
      >
        <!-- 动态生成菜单 -->
        <template v-for="item in filteredMenus" :key="item.path">
          <!-- 带子菜单 -->
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="sub in item.children"
              :key="sub.path"
              :index="sub.path"
            >
              <el-icon><component :is="sub.icon" /></el-icon>
              <span>{{ sub.title }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 一级菜单 -->
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- ===== 主内容区 ===== -->
    <el-container class="main-container">
      <!-- 顶部导航 -->
      <el-header class="top-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRoute.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- AI助手入口 -->
          <el-badge :value="aiUnread" :hidden="aiUnread === 0" class="ai-badge">
            <el-button text @click="router.push('/ai-assistant')">
              <el-icon size="18"><MagicStick /></el-icon>
              AI助手
            </el-button>
          </el-badge>

          <!-- 用户信息 -->
          <el-dropdown @command="handleUserCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar" />
              <span class="username">{{ authStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="page-main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="['Dashboard', 'TrafficPlan']">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isCollapse = ref(false)
const aiUnread = ref(0)
const userAvatar = ref('')

const currentRoute = computed(() => route)

// ===== 菜单配置 =====
const allMenus = [
  {
    path: '/dashboard', title: '管理仪表盘', icon: 'DataBoard', roles: ['ADMIN'],
  },
  {
    path: '/statistics', title: '数据统计', icon: 'DataLine', roles: ['ADMIN'],
  },
  {
    path: '/traffic-plan', title: '流量规划管理', icon: 'Guide', roles: ['ADMIN'],
  },
  {
    path: '/monitor', title: '实时监控管理', icon: 'Monitor', roles: ['ADMIN'],
  },
  {
    path: '/network-stability', title: '网络稳定性', icon: 'Connection', roles: ['ADMIN'],
  },
  {
    path: '/device-usage', title: '设备使用统计', icon: 'Laptop', roles: ['ADMIN'],
  },
  {
    path: '/server-peak', title: '服务器峰值', icon: 'Cpu', roles: ['ADMIN'],
  },
  {
    path: '/announcement', title: '公告管理', icon: 'Bell', roles: ['ADMIN'],
  },
  {
    path: '/network-access', title: '校园网接入管理', icon: 'Wifi', roles: ['ADMIN'],
  },
  {
    path: '/ai-assistant', title: 'AI智能助手', icon: 'MagicStick', roles: ['ADMIN', 'USER'],
  },
  {
    path: '/user/home', title: '个人首页', icon: 'HomeFilled', roles: ['USER'],
  },
  {
    path: '/announcements', title: '公告列表', icon: 'Bell', roles: ['USER'],
  },
]

const filteredMenus = computed(() =>
  allMenus.filter(m => m.roles.includes(authStore.userRole))
)

const activeMenu = computed(() => route.path)

function handleUserCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      authStore.logout()
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    ElMessageBox.alert('个人中心功能开发中...', '提示')
  }
}

onMounted(() => {
  authStore.fetchCurrentUser()
})
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  background: #1d1e23;
  transition: width 0.3s;
  overflow-x: hidden;
  overflow-y: auto;
  flex-shrink: 0;

  .logo-area {
    height: 60px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    gap: 10px;
    border-bottom: 1px solid #2d2e33;

    .logo-icon { width: 28px; height: 28px; }
    .logo-text {
      color: #fff;
      font-size: 14px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    border-right: none;
    background: transparent;
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #606266;
      &:hover { color: #409EFF; }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;

    .ai-badge { margin-right: 8px; }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      &:hover { background: #f5f7fa; }

      .username { font-size: 14px; color: #303133; }
    }
  }
}

.page-main {
  overflow-y: auto;
  background: #f0f2f5;
  padding: 20px;
}
</style>

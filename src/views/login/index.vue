<template>
  <div class="login-container" :class="{ 'is-admin': activeRole === 'ADMIN' }">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="login-header">
        <h1 class="title">网络流量管理系统</h1>
        <p class="subtitle">Flow Control Management System</p>
      </div>

      <!-- 角色选择 -->
      <div class="role-selector">
        <button
          type="button"
          class="role-btn"
          :class="{ active: activeRole === 'ADMIN' }"
          @click="switchRole('ADMIN')"
        >
          <svg class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L3 7v10l9 5 9-5V7l-9-5z"/>
            <path d="M12 22V12"/>
            <path d="M3 7l9 5 9-5"/>
          </svg>
          <span>管理员入口</span>
        </button>
        <button
          type="button"
          class="role-btn"
          :class="{ active: activeRole === 'USER' }"
          @click="switchRole('USER')"
        >
          <svg class="role-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          <span>普通用户入口</span>
        </button>
      </div>

      <!-- 角色提示 -->
      <div class="role-tip">
        <template v-if="activeRole === 'ADMIN'">
          <el-icon><Lock /></el-icon>
          <span>管理员登录入口</span>
        </template>
        <template v-else>
          <el-icon><User /></el-icon>
          <span>普通用户登录入口</span>
        </template>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :placeholder="activeRole === 'ADMIN' ? '请输入管理员账号' : '请输入用户名'"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            :class="{ 'admin-btn': activeRole === 'ADMIN' }"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <template v-if="activeRole === 'ADMIN'">
          <span class="tips"></span>
        </template>
        <template v-else>
          <span class="tips">没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { Lock, User } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const activeRole = ref('ADMIN') // 默认管理员

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, max: 32, message: '密码长度为3-32位', trigger: 'blur' },
  ],
}

// 切换角色
function switchRole(role) {
  activeRole.value = role
  form.username = ''
  form.password = ''
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await authStore.login(form.username, form.password)
    ElMessage.success(result.message || '登录成功')

    // 跳转到来源页面或首页
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else {
      // 根据返回的角色和选择的角色跳转
      const userRole = result.data?.role
      if (userRole === 'ADMIN') {
        router.push('/dashboard')
      } else {
        router.push('/user/home')
      }
    }
  } catch (e) {
    // 错误已在 Axios 拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  transition: background 0.5s ease;

  // 普通用户主题
  &.is-admin {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  }
}

.bg-decoration {
  position: absolute;
  inset: 0;
  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
  }
  .circle-1 { width: 400px; height: 400px; top: -100px; left: -100px; }
  .circle-2 { width: 300px; height: 300px; bottom: -50px; right: -50px; }
  .circle-3 { width: 200px; height: 200px; top: 50%; right: 20%; }
}

.login-card {
  width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
  .title {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 8px;
  }
  .subtitle {
    font-size: 13px;
    color: #909399;
    letter-spacing: 1px;
  }
}

// 角色选择器
.role-selector {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;

  .role-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 14px 16px;
    border: 2px solid #e4e7ed;
    border-radius: 10px;
    background: #fff;
    color: #606266;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;

    .role-icon {
      width: 20px;
      height: 20px;
    }

    &:hover {
      border-color: #c0c4cc;
      color: #409EFF;
    }

    &.active {
      border-color: #409EFF;
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.08) 0%, rgba(64, 158, 255, 0.03) 100%);
      color: #409EFF;

      .role-icon {
        color: #409EFF;
      }
    }
  }
}

// 管理员激活时的样式
.login-container.is-admin .role-selector .role-btn.active {
  border-color: #E6A23C;
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.1) 0%, rgba(230, 162, 60, 0.03) 100%);
  color: #E6A23C;

  .role-icon {
    color: #E6A23C;
  }
}

// 角色提示
.role-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 20px;
  padding: 10px;
  background: #f4f4f5;
  border-radius: 6px;
  font-size: 13px;
  color: #909399;
  transition: all 0.3s ease;

  .el-icon {
    font-size: 14px;
  }
}

.login-container.is-admin .role-tip {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.1) 0%, rgba(245, 245, 245, 1) 100%);
  color: #E6A23C;
}

.login-form {
  .login-btn {
    width: 100%;
    font-size: 16px;
    letter-spacing: 4px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }

    &.admin-btn {
      background: linear-gradient(135deg, #E6A23C 0%, #F56C6C 100%);
    }
  }
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  .tips {
    font-size: 13px;
    color: #909399;
  }
  .register-link {
    font-size: 13px;
    color: #409EFF;
    text-decoration: none;
    font-weight: 500;
    &:hover {
      text-decoration: underline;
    }
  }
}
</style>

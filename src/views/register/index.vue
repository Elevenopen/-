<template>
  <div class="register-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <!-- 注册卡片 -->
    <div class="register-card">
      <div class="register-header">
        <h1 class="title">新用户注册</h1>
        <p class="subtitle">创建您的网络流量管理系统账号</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名（3位以上，字母/数字/下划线）"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="昵称（选填）"
            size="large"
            prefix-icon="UserFilled"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（6位以上）"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="register-btn"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span class="login-link">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

// 自定义校验：确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '只能包含字母、数字、下划线和中文字符', trigger: 'blur' },
  ],
  nickname: [
    { max: 30, message: '昵称最多30个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.register(form.username, form.password, form.nickname || form.username)
    ElMessage.success('注册成功！请使用账号密码登录')
    router.push('/login')
  } catch (e) {
    // 注册错误（409用户名重复等）已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
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

.register-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
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

.register-form {
  .register-btn {
    width: 100%;
    font-size: 16px;
    letter-spacing: 4px;
  }
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  .login-link {
    font-size: 13px;
    color: #909399;
    a {
      color: #409EFF;
      text-decoration: none;
      font-weight: 500;
      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>

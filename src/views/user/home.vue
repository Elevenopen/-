<template>
  <div class="user-home page-container">
    <div class="welcome-banner">
      <h2>👋 你好，{{ authStore.nickname }}！</h2>
      <p>欢迎使用网络流量管理系统</p>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <div class="card-container">
          <div class="card-header">
            <span class="card-title">📢 最新公告</span>
            <el-button text type="primary" size="small" @click="router.push('/announcements')">
              查看全部
            </el-button>
          </div>
          <div class="announcement-list">
            <div
              v-for="item in announcements"
              :key="item.id"
              class="announcement-item"
              @click="showDetail(item)"
            >
              <el-tag v-if="item.priority === 'TOP'" type="danger" size="small" effect="dark">置顶</el-tag>
              <span class="ann-title">{{ item.title }}</span>
              <span class="ann-time">{{ formatTime(item.publishTime) }}</span>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :md="8">
        <div class="card-container">
          <div class="card-header">
            <span class="card-title">💻 我的设备</span>
          </div>
          <el-empty v-if="devices.length === 0" description="暂无设备数据" />
          <div v-else class="device-list">
            <div v-for="d in devices" :key="d.deviceId" class="device-item">
              <div class="device-info">
                <span class="device-name">{{ d.deviceName }}</span>
                <el-tag :type="d.status === 'ONLINE' ? 'success' : 'info'" size="small">
                  {{ d.status === 'ONLINE' ? '在线' : '离线' }}
                </el-tag>
              </div>
              <div class="device-stats">
                <span>在线 {{ formatDuration(d.onlineDuration) }}</span>
                <span>流量 {{ d.trafficUsed }} MB</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- AI助手快捷入口 -->
    <div class="ai-card" @click="router.push('/ai-assistant')">
      <div class="ai-content">
        <el-icon size="32" color="#9f7aea"><MagicStick /></el-icon>
        <div>
          <h3>🤖 AI 智能助手</h3>
          <p>上传数据，获取AI分析报告，或咨询任何问题</p>
        </div>
      </div>
      <el-button type="primary">立即使用 →</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import apiClient from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const announcements = ref([])
const devices = ref([])

function formatTime(time) {
  return dayjs(time).format('MM-DD HH:mm')
}

function formatDuration(seconds) {
  if (!seconds) return '0秒'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

function showDetail(item) {
  ElMessage.info(`公告: ${item.title}`)
}

onMounted(async () => {
  try {
    const [annRes, devRes] = await Promise.all([
      apiClient.get('/announcement/public/active?pageNum=1&pageSize=5'),
      apiClient.get('/device/my'),
    ])
    announcements.value = annRes.data?.records || []
    devices.value = devRes.data || []
  } catch (e) {
    console.error('加载数据失败:', e)
  }
})
</script>

<style scoped lang="scss">
.user-home { max-width: 1200px; margin: 0 auto; }

.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 28px 32px;
  color: #fff;
  margin-bottom: 20px;
  h2 { font-size: 22px; margin-bottom: 6px; }
  p { font-size: 14px; opacity: 0.85; }
}

.card-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  .card-title { font-size: 15px; font-weight: 600; color: #303133; }
}

.announcement-list {
  .announcement-item {
    display: flex; align-items: center; gap: 8px;
    padding: 10px 0; border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    &:last-child { border-bottom: none; }
    .ann-title { flex: 1; font-size: 13px; color: #606266; }
    .ann-time { font-size: 12px; color: #c0c4cc; }
  }
}

.device-list {
  .device-item {
    padding: 12px 0; border-bottom: 1px solid #f0f0f0;
    &:last-child { border-bottom: none; }
    .device-info {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 6px;
      .device-name { font-size: 14px; font-weight: 500; }
    }
    .device-stats {
      display: flex; gap: 16px; font-size: 12px; color: #909399;
    }
  }
}

.ai-card {
  background: linear-gradient(135deg, #553c9a 0%, #6b46c1 100%);
  border-radius: 12px;
  padding: 24px 32px;
  display: flex; align-items: center; justify-content: space-between;
  color: #fff; cursor: pointer; transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  h3 { font-size: 18px; margin-bottom: 6px; }
  p { font-size: 13px; opacity: 0.85; }
  .ai-content { display: flex; align-items: center; gap: 16px; }
}
</style>

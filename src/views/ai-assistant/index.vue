<template>
  <div class="ai-assistant page-container">
    <div class="ai-layout">
      <!-- 对话历史 -->
      <div class="history-sidebar">
        <div class="history-header">
          <span>对话历史</span>
          <el-button text size="small" @click="createNewChat">
            <el-icon><Plus /></el-icon> 新对话
          </el-button>
        </div>
        <div class="history-list">
          <div
            v-for="(chat, index) in chatHistory"
            :key="index"
            :class="['history-item', { active: currentChatIndex === index }]"
            @click="switchChat(index)"
          >
            <el-icon><ChatLineSquare /></el-icon>
            <span>{{ chat.title }}</span>
          </div>
        </div>
      </div>

      <!-- 对话主界面 -->
      <div class="chat-main">
        <!-- 消息列表 -->
        <div class="messages-area" ref="messagesRef">
          <div v-if="messages.length === 0" class="welcome-msg">
            <div class="welcome-icon">🤖</div>
            <h3>您好，我是 AI 智能助手</h3>
            <p>我可以帮您：</p>
            <ul>
              <li>📊 分析网络流量数据，生成报告</li>
              <li>🔍 解读服务器告警，提供处理建议</li>
              <li>📋 解答系统使用问题</li>
              <li>⚡ 根据历史数据给出优化建议</li>
            </ul>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message-bubble', msg.role]"
          >
            <div class="bubble-avatar">
              <span>{{ msg.role === 'user' ? '👤' : '🤖' }}</span>
            </div>
            <div class="bubble-content">
              <div class="bubble-text" v-html="renderMarkdown(msg.content)"></div>
              <div class="bubble-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>

          <div v-if="loading" class="message-bubble assistant loading-bubble">
            <div class="bubble-avatar">🤖</div>
            <div class="bubble-content">
              <div class="bubble-text typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <el-upload
            ref="uploadRef"
            :action="uploadUrl"
            :headers="{ Authorization: `Bearer ${token}` }"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <el-button text>
              <el-icon size="20"><Upload /></el-icon>
            </el-button>
          </el-upload>
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入消息，或上传文件让AI分析...（Enter发送）"
            @keydown.enter.exact.prevent="sendMessage"
            class="input-textarea"
          />
          <el-button type="primary" :loading="loading" @click="sendMessage" :disabled="!inputText.trim()">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import apiClient from '@/stores/auth'

const messagesRef = ref(null)
const inputText = ref('')
const loading = ref(false)
const messages = ref([])
const chatHistory = ref([{ title: '新对话 1', messages: [] }])
const currentChatIndex = ref(0)
const token = localStorage.getItem('accessToken')
const uploadUrl = computed(() => `${import.meta.env.VITE_API_BASE_URL || '/api'}/ai/analyze`)

function formatTime(ts) {
  return dayjs(ts).format('HH:mm')
}

function renderMarkdown(text) {
  return text
    .replace(/```(\w+)?\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text, timestamp: Date.now() })
  inputText.value = ''
  scrollToBottom()
  loading.value = true
  try {
    const res = await apiClient.post('/ai/chat', { message: text })
    messages.value.push({
      role: 'assistant',
      content: res.data?.content || res.message || '抱歉，AI暂时无法响应',
      timestamp: Date.now(),
    })
  } catch {
    messages.value.push({ role: 'assistant', content: '❌ 请求失败，请检查网络或AI服务配置', timestamp: Date.now() })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function createNewChat() {
  chatHistory.value.unshift({ title: `新对话 ${chatHistory.value.length + 1}`, messages: [] })
  currentChatIndex.value = 0
  messages.value = []
}

function switchChat(index) {
  currentChatIndex.value = index
  messages.value = chatHistory.value[index].messages
}

function handleUploadSuccess(res) {
  ElMessage.success('文件上传成功，AI正在分析...')
  if (res.data) {
    messages.value.push({ role: 'assistant', content: `📄 已收到文件，分析结果：\n\n${res.data}`, timestamp: Date.now() })
  }
}

function beforeUpload(file) {
  const isExcel = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel'].includes(file.type)
  if (!isExcel) { ElMessage.error('仅支持 Excel 文件（.xlsx / .xls）'); return false }
  if (file.size / 1024 / 1024 > 10) { ElMessage.error('文件大小不能超过 10MB'); return false }
  return true
}

onMounted(() => { messages.value = chatHistory.value[0].messages })
</script>

<style scoped lang="scss">
.ai-assistant { height: calc(100vh - 80px); padding: 0; }
.ai-layout { display: flex; height: 100%; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.history-sidebar { width: 220px; background: #fafafa; border-right: 1px solid #ebeef5; display: flex; flex-direction: column; flex-shrink: 0;
  .history-header { padding: 16px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #ebeef5; font-weight: 600; font-size: 14px; }
  .history-list { flex: 1; overflow-y: auto; padding: 8px;
    .history-item { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-radius: 6px; cursor: pointer; font-size: 13px; color: #606266; margin-bottom: 2px;
      &:hover { background: #ecf5ff; color: #409EFF; }
      &.active { background: #409EFF; color: #fff; }
    }
  }
}
.chat-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.messages-area { flex: 1; overflow-y: auto; padding: 24px; }
.welcome-msg { text-align: center; padding: 60px 20px;
  .welcome-icon { font-size: 56px; margin-bottom: 16px; }
  h3 { font-size: 20px; color: #303133; margin-bottom: 12px; }
  p { color: #909399; margin-bottom: 12px; }
  ul { list-style: none; text-align: left; max-width: 400px; margin: 0 auto;
    li { padding: 6px 0; font-size: 14px; color: #606266; }
  }
}
.message-bubble { display: flex; gap: 12px; margin-bottom: 20px;
  &.user { flex-direction: row-reverse; .bubble-content { align-items: flex-end; } }
  .bubble-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; background: #f0f2f5; }
  .bubble-content { max-width: 70%; }
  .bubble-text { background: #f4f4f5; padding: 12px 16px; border-radius: 12px; font-size: 14px; line-height: 1.7; color: #303133; word-break: break-word; }
  .bubble-time { font-size: 11px; color: #c0c4cc; margin-top: 4px; }
  &.user .bubble-text { background: #409EFF; color: #fff; }
  &.assistant .bubble-avatar { background: #553c9a; }
}
.loading-bubble .bubble-text.typing { display: flex; gap: 4px; align-items: center; background: transparent;
  span { width: 8px; height: 8px; border-radius: 50%; background: #909399; animation: typing 1.4s infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}
@keyframes typing { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-6px); opacity: 1; } }
.input-area { padding: 16px 24px; border-top: 1px solid #ebeef5; display: flex; align-items: flex-end; gap: 12px;
  .input-textarea { flex: 1; .el-textarea__inner { border-radius: 20px; padding: 8px 16px; } }
}
</style>

<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">校园网接入管理</h1>
      <el-button type="primary" @click="showDialog('add')">
        <el-icon><Plus /></el-icon> 配置用户接入
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card enabled">
        <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.enabledCount }}</div>
          <div class="stat-label">已启用</div>
        </div>
      </div>
      <div class="stat-card disabled">
        <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.disabledCount }}</div>
          <div class="stat-label">已禁用</div>
        </div>
      </div>
      <div class="stat-card online">
        <div class="stat-icon"><el-icon><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.onlineCount }}</div>
          <div class="stat-label">在线用户</div>
        </div>
      </div>
      <div class="stat-card traffic">
        <div class="stat-icon"><el-icon><DataLine /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ formatTraffic(statistics.totalDataUsed) }}</div>
          <div class="stat-label">总流量</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="search-bar">
      <el-select v-model="queryParams.status" placeholder="接入状态" clearable @change="loadData">
        <el-option label="全部" value="" />
        <el-option label="已启用" value="ENABLED" />
        <el-option label="已禁用" value="DISABLED" />
      </el-select>
      <el-input v-model="queryParams.keyword" placeholder="搜索用户名" clearable @change="loadData" style="width: 200px;">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button @click="loadData"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <!-- 数据表格 -->
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.accessStatus === 'ENABLED' ? 'success' : 'danger'">
              {{ row.accessStatus === 'ENABLED' ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="流量配额" width="180">
          <template #default="{ row }">
            <span v-if="row.dataQuota">
              {{ row.dataUsed }} / {{ row.dataQuota }} MB
              <el-progress :percentage="Math.min(100, (row.dataUsed / row.dataQuota) * 100)" :stroke-width="6" />
            </span>
            <span v-else>无限制</span>
          </template>
        </el-table-column>
        <el-table-column label="时长配额" width="180">
          <template #default="{ row }">
            <span v-if="row.timeQuota">
              {{ formatDuration(row.timeUsed) }} / {{ formatDuration(row.timeQuota) }}
            </span>
            <span v-else>无限制</span>
          </template>
        </el-table-column>
        <el-table-column label="设备限制" width="90">
          <template #default="{ row }">
            {{ row.deviceLimit }} 台
          </template>
        </el-table-column>
        <el-table-column label="接入类型" width="100">
          <template #default="{ row }">
            {{ row.accessType || 'WIFI' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleEnable(row)" v-if="row.accessStatus === 'DISABLED'">
              启用
            </el-button>
            <el-button size="small" type="warning" @click="handleDisable(row)" v-else>
              禁用
            </el-button>
            <el-button size="small" type="primary" @click="showDialog('edit', row)">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="用户" prop="userId">
          <el-select v-model="form.userId" placeholder="选择用户" filterable :disabled="formType === 'edit'">
            <el-option v-for="user in userList" :key="user.id" :label="user.nickname" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="接入状态" prop="accessStatus">
          <el-radio-group v-model="form.accessStatus">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="接入类型" prop="accessType">
          <el-select v-model="form.accessType" placeholder="选择类型">
            <el-option label="WIFI" value="WIFI" />
            <el-option label="宽带" value="宽带" />
            <el-option label="移动网络" value="移动网络" />
          </el-select>
        </el-form-item>
        <el-form-item label="流量配额(MB)" prop="dataQuota">
          <el-input-number v-model="form.dataQuota" :min="0" :step="1024" placeholder="0表示无限制" />
        </el-form-item>
        <el-form-item label="时长配额(分钟)" prop="timeQuota">
          <el-input-number v-model="form.timeQuota" :min="0" :step="60" placeholder="0表示无限制" />
        </el-form-item>
        <el-form-item label="设备数量限制" prop="deviceLimit">
          <el-input-number v-model="form.deviceLimit" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="禁用原因" prop="reason" v-if="form.accessStatus === 'DISABLED'">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="请输入禁用原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 禁用对话框 -->
    <el-dialog v-model="disableDialogVisible" title="禁用网络接入" width="400px">
      <el-form>
        <el-form-item label="禁用原因">
          <el-input v-model="disableReason" type="textarea" :rows="3" placeholder="请输入禁用原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDisable">确定禁用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, CircleCheck, CircleClose, User, DataLine } from '@element-plus/icons-vue'
import apiClient from '@/stores/auth'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const statistics = ref({ enabledCount: 0, disabledCount: 0, onlineCount: 0, totalDataUsed: 0 })
const userList = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  keyword: ''
})

const dialogVisible = ref(false)
const disableDialogVisible = ref(false)
const dialogTitle = ref('')
const formType = ref('add')
const submitting = ref(false)
const disableReason = ref('')
const currentRow = ref(null)

const form = reactive({
  id: null,
  userId: null,
  username: '',
  accessStatus: 'DISABLED',
  accessType: 'WIFI',
  dataQuota: null,
  timeQuota: null,
  deviceLimit: 3,
  reason: ''
})

const rules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }]
}

onMounted(() => {
  loadData()
  loadStatistics()
  loadUsers()
})

async function loadData() {
  loading.value = true
  try {
    const res = await apiClient.get('/network-access/list', { params: queryParams })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const res = await apiClient.get('/network-access/statistics')
    statistics.value = res.data || {}
  } catch (e) {
    console.error(e)
  }
}

async function loadUsers() {
  try {
    const res = await apiClient.get('/auth/users')
    userList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function showDialog(type, row = null) {
  formType.value = type
  dialogTitle.value = type === 'add' ? '配置用户接入' : '编辑接入配置'
  
  if (type === 'edit' && row) {
    Object.assign(form, {
      id: row.id,
      userId: row.userId,
      username: row.username,
      accessStatus: row.accessStatus,
      accessType: row.accessType || 'WIFI',
      dataQuota: row.dataQuota,
      timeQuota: row.timeQuota,
      deviceLimit: row.deviceLimit || 3,
      reason: row.reason || ''
    })
  } else {
    Object.assign(form, {
      id: null,
      userId: null,
      username: '',
      accessStatus: 'DISABLED',
      accessType: 'WIFI',
      dataQuota: null,
      timeQuota: null,
      deviceLimit: 3,
      reason: ''
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (formType.value === 'add') {
      await apiClient.post('/network-access', form)
      ElMessage.success('配置成功')
    } else {
      await apiClient.post('/network-access', form)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    loadData()
    loadStatistics()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleEnable(row) {
  try {
    await ElMessageBox.confirm(`确定启用用户「${row.username}」的网络接入？`, '确认', { type: 'success' })
    await apiClient.put(`/network-access/${row.id}/enable`)
    ElMessage.success('已启用')
    loadData()
    loadStatistics()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

function handleDisable(row) {
  currentRow.value = row
  disableReason.value = ''
  disableDialogVisible.value = true
}

async function confirmDisable() {
  try {
    await apiClient.put(`/network-access/${currentRow.value.id}/disable`, { reason: disableReason.value })
    ElMessage.success('已禁用')
    disableDialogVisible.value = false
    loadData()
    loadStatistics()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」的接入配置？`, '警告', { type: 'warning' })
    await apiClient.delete(`/network-access/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
    loadStatistics()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

function formatTraffic(mb) {
  if (!mb) return '0 MB'
  if (mb >= 1024) return (mb / 1024).toFixed(2) + ' GB'
  return mb + ' MB'
}

function formatDuration(minutes) {
  if (!minutes) return '0分钟'
  if (minutes >= 60) {
    const h = Math.floor(minutes / 60)
    const m = minutes % 60
    return m > 0 ? `${h}小时${m}分钟` : `${h}小时`
  }
  return `${minutes}分钟`
}
</script>

<style scoped>
.page-container { padding: 20px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 600; }

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.stat-card.enabled .stat-icon { background: #e6f7ed; color: #52c41a; }
.stat-card.disabled .stat-icon { background: #fff1f0; color: #ff4d4f; }
.stat-card.online .stat-icon { background: #e6f7ff; color: #1890ff; }
.stat-card.traffic .stat-icon { background: #fff7e6; color: #fa8c16; }

.stat-value { font-size: 24px; font-weight: 600; }
.stat-label { font-size: 14px; color: #666; }

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.card-container { background: #fff; border-radius: 8px; padding: 16px; }

.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

<template>
  <div class="page-container">
    <div class="page-title">流量规划管理</div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #67C23A">
          <div class="stat-icon"><el-icon size="28"><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.active || 0 }}</div>
            <div class="stat-label">活跃规划</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #909399">
          <div class="stat-icon"><el-icon size="28"><Remove /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.inactive || 0 }}</div>
            <div class="stat-label">已停用</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #F56C6C">
          <div class="stat-icon"><el-icon size="28"><Clock /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.expired || 0 }}</div>
            <div class="stat-label">已过期</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #409EFF">
          <div class="stat-icon"><el-icon size="28"><Guide /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.total || 0 }}</div>
            <div class="stat-label">总规划数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <div class="card-container search-bar">
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:130px">
        <el-option label="全部" value="" />
        <el-option label="活跃" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
        <el-option label="过期" value="EXPIRED" />
      </el-select>
      <el-select v-model="searchForm.priority" placeholder="优先级" clearable style="width:130px">
        <el-option label="全部" value="" />
        <el-option label="高 HIGH" value="HIGH" />
        <el-option label="中 MEDIUM" value="MEDIUM" />
        <el-option label="低 LOW" value="LOW" />
      </el-select>
      <el-input v-model="searchForm.keyword" placeholder="搜索规划名称" clearable style="width:180px"
        @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
      <el-button @click="handleReset"><el-icon><RefreshLeft /></el-icon>重置</el-button>
    </div>

    <!-- 工具栏 -->
    <div class="card-container toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新增</el-button>
        <el-button type="danger" plain :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button @click="handleExport"><el-icon><Download /></el-icon>导出</el-button>
        <el-button @click="handleDownloadTemplate"><el-icon><Document /></el-icon>模板</el-button>
        <el-upload ref="uploadRef" :action="importUrl" :headers="authHeaders" :show-file-list="false"
          :auto-upload="false" :on-change="handleImportChange">
          <el-button type="success"><el-icon><Upload /></el-icon>导入</el-button>
        </el-upload>
        <el-button @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" size="default">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="planName" label="规划名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="targetBandwidth" label="目标带宽" width="110" />
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'MEDIUM' ? 'warning' : 'info'" size="small">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" width="220">
          <template #default="{ row }">
            {{ formatDT(row.startTime) }} ~ {{ formatDT(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createUsername" label="创建人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDT(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规划名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入规划名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="目标带宽" prop="targetBandwidth">
          <el-input v-model="form.targetBandwidth" placeholder="如: 100Mbps" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" style="width:100%">
            <el-option label="高 HIGH" value="HIGH" />
            <el-option label="中 MEDIUM" value="MEDIUM" />
            <el-option label="低 LOW" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择开始时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择结束时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import apiClient, { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const authHeaders = { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

// ===== 状态 =====
const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const selectedRows = ref([])
const uploadRef = ref(null)

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const stats = ref({})

const searchForm = reactive({ status: '', priority: '', keyword: '' })

// ===== Dialog =====
const dialogVisible = ref(false)
const dialogTitle = ref('新增规划')
const editId = ref(null)
const formRef = ref(null)
const form = reactive({
  planName: '', targetBandwidth: '', priority: 'MEDIUM',
  startTime: null, endTime: null, status: 'ACTIVE', description: ''
})
const rules = {
  planName: [{ required: true, message: '请输入规划名称', trigger: 'blur' }],
  targetBandwidth: [{ required: true, message: '请输入目标带宽', trigger: 'blur' }],
}

// ===== 工具函数 =====
function formatDT(dt) {
  return dt ? dayjs(dt).format('YYYY-MM-DD HH:mm') : '-'
}
function statusLabel(s) {
  return { ACTIVE: '活跃', INACTIVE: '停用', EXPIRED: '已过期' }[s] || s
}
function statusTagType(s) {
  return { ACTIVE: 'success', INACTIVE: 'info', EXPIRED: 'danger' }[s] || 'info'
}

// ===== 数据加载 =====
async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.priority) params.priority = searchForm.priority
    if (searchForm.keyword) params.keyword = searchForm.keyword
    const res = await apiClient.get('/traffic-plan/list', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { /* 错误已在拦截器处理 */ }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await apiClient.get('/traffic-plan/statistics')
    stats.value = res.data || {}
  } catch {}
}

// ===== 搜索 =====
function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() {
  searchForm.status = ''; searchForm.priority = ''; searchForm.keyword = ''
  pagination.pageNum = 1; loadData()
}

// ===== 表格操作 =====
function handleSelectionChange(rows) { selectedRows.value = rows }

function handleAdd() {
  editId.value = null; dialogTitle.value = '新增规划'
  Object.assign(form, { planName: '', targetBandwidth: '', priority: 'MEDIUM',
    startTime: null, endTime: null, status: 'ACTIVE', description: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  editId.value = row.id; dialogTitle.value = '编辑规划'
  Object.assign(form, {
    planName: row.planName, targetBandwidth: row.targetBandwidth,
    priority: row.priority, startTime: row.startTime, endTime: row.endTime,
    status: row.status, description: row.description
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) {
      await apiClient.put(`/traffic-plan/${editId.value}`, form)
    } else {
      await apiClient.post('/traffic-plan', form)
    }
    ElMessage.success(dialogTitle.value + '成功')
    dialogVisible.value = false
    loadData(); loadStats()
  } catch {}
  finally { submitting.value = false }
}

async function handleDelete(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  await ElMessageBox.confirm(`确定删除规划「${row.planName}」？`, '确认删除', { type: 'warning' })
  await apiClient.delete(`/traffic-plan/${row.id}`)
  ElMessage.success('删除成功')
  loadData(); loadStats()
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 条规划？`, '批量删除', { type: 'warning' })
  await apiClient.delete('/traffic-plan/batch', { data: selectedRows.value.map(r => r.id) })
  ElMessage.success('批量删除成功')
  selectedRows.value = []
  loadData(); loadStats()
}

// ===== 导入/导出 =====
const importUrl = `${BASE_URL}/traffic-plan/import`

async function handleImportChange(file) {
  const isExcel = /\.(xlsx|xls)$/i.test(file.name)
  if (!isExcel) { ElMessage.error('仅支持 Excel 文件'); return }
  const formData = new FormData()
  formData.append('file', file.raw)
  try {
    const res = await apiClient.post('/traffic-plan/import', formData,
      { headers: { 'Content-Type': 'multipart/form-data' } })
    const d = res.data || {}
    ElMessage.success(`导入完成：成功 ${d.success || 0} 条，失败 ${d.fail || 0} 条`)
    loadData(); loadStats()
  } catch {}
}

async function handleExport() {
  try {
    const token = localStorage.getItem('accessToken')
    const res = await fetch(`${BASE_URL}/traffic-plan/export`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = `流量规划_${dayjs().format('YYYYMMDD')}.xlsx`; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

async function handleDownloadTemplate() {
  try {
    const token = localStorage.getItem('accessToken')
    const res = await fetch(`${BASE_URL}/traffic-plan/template`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = '流量规划导入模板.xlsx'; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('模板下载失败') }
}

onMounted(() => { loadData(); loadStats() })
</script>

<style scoped lang="scss">
.stat-cards { margin-bottom: 16px; }
.stat-card {
  background: #fff; border-radius: 10px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); margin-bottom: 16px;
  border-left: 4px solid var(--accent);
  .stat-icon {
    width: 52px; height: 52px; border-radius: 12px;
    background: rgba(0,0,0,0.04); display: flex; align-items: center; justify-content: center;
    color: var(--accent);
  }
  .stat-value { font-size: 26px; font-weight: 700; color: #303133; }
  .stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
}
.toolbar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .toolbar-left, .toolbar-right { display: flex; gap: 8px; flex-wrap: wrap; }
}
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>

<template>
  <div class="page-container">
    <div class="page-title">实时监控管理</div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #67C23A">
          <div class="stat-icon"><el-icon size="28"><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.normal || 0 }}</div>
            <div class="stat-label">正常记录</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #F56C6C">
          <div class="stat-icon"><el-icon size="28"><CircleClose /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.abnormal || 0 }}</div>
            <div class="stat-label">异常记录</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #409EFF">
          <div class="stat-icon"><el-icon size="28"><TrendCharts /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgTraffic || 0 }} MB</div>
            <div class="stat-label">平均流量</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #E6A23C">
          <div class="stat-icon"><el-icon size="28"><Top /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.maxTraffic || 0 }} MB</div>
            <div class="stat-label">最大流量</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <div class="card-container search-bar">
      <el-input v-model="searchForm.deviceId" placeholder="设备ID" clearable style="width:150px" />
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:130px">
        <el-option label="全部" value="" />
        <el-option label="正常" value="NORMAL" />
        <el-option label="异常" value="ABNORMAL" />
      </el-select>
      <el-select v-model="searchForm.anomalyFlag" placeholder="异常标记" clearable style="width:130px">
        <el-option label="全部" value="" />
        <el-option label="正常" value="0" />
        <el-option label="异常" value="1" />
      </el-select>
      <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至"
        start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss"
        style="width:320px" />
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
        <el-table-column prop="deviceId" label="设备ID" width="140" show-overflow-tooltip />
        <el-table-column prop="deviceName" label="设备名称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="trafficValue" label="流量值(MB)" width="110" />
        <el-table-column prop="trafficRate" label="速率(Mbps)" width="100" />
        <el-table-column prop="recordTime" label="记录时间" width="160">
          <template #default="{ row }">{{ formatDT(row.recordTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'" size="small">
              {{ row.status === 'NORMAL' ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="anomalyFlag" label="异常标记" width="90">
          <template #default="{ row }">
            <el-tag :type="row.anomalyFlag === 1 ? 'danger' : 'info'" size="small">
              {{ row.anomalyFlag === 1 ? '异常' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="anomalyDesc" label="异常描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize"
          :total="pagination.total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="流量值(MB)" prop="trafficValue">
          <el-input-number v-model="form.trafficValue" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="流量速率">
          <el-input-number v-model="form.trafficRate" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="记录时间">
          <el-date-picker v-model="form.recordTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="正常" value="NORMAL" />
            <el-option label="异常" value="ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常标记">
          <el-select v-model="form.anomalyFlag" style="width:100%">
            <el-option label="正常" :value="0" />
            <el-option label="异常" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常描述">
          <el-input v-model="form.anomalyDesc" type="textarea" :rows="2" placeholder="可选" />
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
import apiClient from '@/stores/auth'

const authHeaders = { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const selectedRows = ref([])
const uploadRef = ref(null)
const dateRange = ref(null)

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const stats = ref({})
const searchForm = reactive({ deviceId: '', status: '', anomalyFlag: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('新增记录')
const editId = ref(null)
const formRef = ref(null)
const form = reactive({
  deviceId: '', deviceName: '', trafficValue: null, trafficRate: null,
  recordTime: null, status: 'NORMAL', anomalyFlag: 0, anomalyDesc: ''
})
const rules = {
  deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  trafficValue: [{ required: true, message: '请输入流量值', trigger: 'blur' }]
}

function formatDT(dt) { return dt ? dayjs(dt).format('YYYY-MM-DD HH:mm') : '-' }

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    if (searchForm.deviceId) params.deviceId = searchForm.deviceId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.anomalyFlag !== '' && searchForm.anomalyFlag !== null) params.anomalyFlag = searchForm.anomalyFlag
    if (dateRange.value) { params.startTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
    const res = await apiClient.get('/monitor/list', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {}
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await apiClient.get('/monitor/statistics')
    stats.value = res.data || {}
  } catch {}
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() {
  searchForm.deviceId = ''; searchForm.status = ''; searchForm.anomalyFlag = ''
  dateRange.value = null; pagination.pageNum = 1; loadData()
}

function handleSelectionChange(rows) { selectedRows.value = rows }

function handleAdd() {
  editId.value = null; dialogTitle.value = '新增记录'
  Object.assign(form, { deviceId: '', deviceName: '', trafficValue: null, trafficRate: null,
    recordTime: null, status: 'NORMAL', anomalyFlag: 0, anomalyDesc: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  editId.value = row.id; dialogTitle.value = '编辑记录'
  Object.assign(form, { deviceId: row.deviceId, deviceName: row.deviceName, trafficValue: row.trafficValue,
    trafficRate: row.trafficRate, recordTime: row.recordTime, status: row.status,
    anomalyFlag: row.anomalyFlag, anomalyDesc: row.anomalyDesc })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await apiClient.put(`/monitor/${editId.value}`, form)
    else await apiClient.post('/monitor', form)
    ElMessage.success(dialogTitle.value + '成功')
    dialogVisible.value = false; loadData(); loadStats()
  } catch {}
  finally { submitting.value = false }
}

async function handleDelete(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  await ElMessageBox.confirm(`确定删除该记录（ID: ${row.id}）？`, '确认删除', { type: 'warning' })
  await apiClient.delete(`/monitor/${row.id}`)
  ElMessage.success('删除成功'); loadData(); loadStats()
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 条记录？`, '批量删除', { type: 'warning' })
  await apiClient.delete('/monitor/batch', { data: selectedRows.value.map(r => r.id) })
  ElMessage.success('批量删除成功'); selectedRows.value = []; loadData(); loadStats()
}

const importUrl = `${BASE_URL}/monitor/import`

async function handleImportChange(file) {
  const isExcel = /\.(xlsx|xls)$/i.test(file.name)
  if (!isExcel) { ElMessage.error('仅支持 Excel 文件'); return }
  const fd = new FormData(); fd.append('file', file.raw)
  try {
    const res = await apiClient.post('/monitor/import', fd,
      { headers: { 'Content-Type': 'multipart/form-data' } })
    const d = res.data || {}
    ElMessage.success(`导入完成：成功 ${d.success || 0} 条，失败 ${d.fail || 0} 条`)
    loadData(); loadStats()
  } catch {}
}

async function handleExport() {
  try {
    const token = localStorage.getItem('accessToken')
    const res = await fetch(`${BASE_URL}/monitor/export`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = `监控记录_${dayjs().format('YYYYMMDD')}.xlsx`; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

async function handleDownloadTemplate() {
  try {
    const token = localStorage.getItem('accessToken')
    const res = await fetch(`${BASE_URL}/monitor/template`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = '监控记录导入模板.xlsx'; a.click()
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
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .toolbar-left, .toolbar-right { display: flex; gap: 8px; flex-wrap: wrap; }
}
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>

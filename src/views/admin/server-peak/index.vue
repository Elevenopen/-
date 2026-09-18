<template>
  <div class="page-container">
    <div class="page-title">服务器峰值管理</div>

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
          <div class="stat-icon"><el-icon size="28"><Warning /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.alert || 0 }}</div>
            <div class="stat-label">告警记录</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #409EFF">
          <div class="stat-icon"><el-icon size="28"><Cpu /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgCpu || 0 }}%</div>
            <div class="stat-label">平均CPU</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="--accent: #E6A23C">
          <div class="stat-icon"><el-icon size="28"><Monitor /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgMemory || 0 }}%</div>
            <div class="stat-label">平均内存</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <div class="card-container search-bar">
      <el-input v-model="searchForm.serverName" placeholder="服务器名称" clearable style="width:160px"
        @keyup.enter="handleSearch" />
      <el-select v-model="searchForm.alertFlag" placeholder="告警状态" clearable style="width:130px">
        <el-option label="全部" value="" />
        <el-option label="正常" value="0" />
        <el-option label="已告警" value="1" />
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
        <el-table-column prop="serverName" label="服务器" min-width="150" show-overflow-tooltip />
        <el-table-column prop="peakTime" label="峰值时间" width="160">
          <template #default="{ row }">{{ formatDT(row.peakTime) }}</template>
        </el-table-column>
        <el-table-column prop="cpuUsage" label="CPU(%)" width="90">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.cpuUsage)" :color="cpuColor(row.cpuUsage)" :stroke-width="8"
              style="width:80px" />
          </template>
        </el-table-column>
        <el-table-column prop="memoryUsage" label="内存(%)" width="90">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.memoryUsage)" :color="memColor(row.memoryUsage)" :stroke-width="8"
              style="width:80px" />
          </template>
        </el-table-column>
        <el-table-column prop="diskUsage" label="磁盘(%)" width="90">
          <template #default="{ row }">{{ row.diskUsage }}%</template>
        </el-table-column>
        <el-table-column prop="alertFlag" label="告警" width="80">
          <template #default="{ row }">
            <el-tag :type="row.alertFlag === 1 ? 'danger' : 'success'" size="small">
              {{ row.alertFlag === 1 ? '告警' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertLevel" label="级别" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.alertLevel" :type="row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">
              {{ row.alertLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="threshold" label="阈值" width="70" />
        <el-table-column prop="description" label="备注" min-width="120" show-overflow-tooltip />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="服务器名称" prop="serverName">
          <el-input v-model="form.serverName" placeholder="请输入服务器名称" />
        </el-form-item>
        <el-form-item label="峰值时间">
          <el-date-picker v-model="form.peakTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="CPU使用率(%)" prop="cpuUsage">
          <el-input-number v-model="form.cpuUsage" :min="0" :max="100" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="内存使用率(%)" prop="memoryUsage">
          <el-input-number v-model="form.memoryUsage" :min="0" :max="100" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="磁盘使用率(%)">
          <el-input-number v-model="form.diskUsage" :min="0" :max="100" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="入流量(MB)">
          <el-input-number v-model="form.networkIn" :min="0" :precision="4" style="width:100%" />
        </el-form-item>
        <el-form-item label="出流量(MB)">
          <el-input-number v-model="form.networkOut" :min="0" :precision="4" style="width:100%" />
        </el-form-item>
        <el-form-item label="告警阈值(%)">
          <el-input-number v-model="form.threshold" :min="0" :max="100" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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
const searchForm = reactive({ serverName: '', alertFlag: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('新增记录')
const editId = ref(null)
const formRef = ref(null)
const form = reactive({
  serverName: '', peakTime: null, cpuUsage: null, memoryUsage: null,
  diskUsage: null, networkIn: null, networkOut: null, threshold: 80, description: ''
})
const rules = {
  serverName: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  cpuUsage: [{ required: true, message: '请输入CPU使用率', trigger: 'blur' }],
  memoryUsage: [{ required: true, message: '请输入内存使用率', trigger: 'blur' }]
}

function formatDT(dt) { return dt ? dayjs(dt).format('YYYY-MM-DD HH:mm') : '-' }
function cpuColor(v) {
  if (v >= 90) return '#F56C6C'; if (v >= 70) return '#E6A23C'; return '#67C23A'
}
function memColor(v) {
  if (v >= 90) return '#F56C6C'; if (v >= 70) return '#E6A23C'; return '#67C23A'
}

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    if (searchForm.serverName) params.serverName = searchForm.serverName
    if (searchForm.alertFlag !== '' && searchForm.alertFlag !== null) params.alertFlag = searchForm.alertFlag
    if (dateRange.value) { params.startTime = dateRange.value[0]; params.endTime = dateRange.value[1] }
    const res = await apiClient.get('/server-peak/list', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {}
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await apiClient.get('/server-peak/statistics')
    stats.value = res.data || {}
  } catch {}
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() {
  searchForm.serverName = ''; searchForm.alertFlag = ''; dateRange.value = null
  pagination.pageNum = 1; loadData()
}

function handleSelectionChange(rows) { selectedRows.value = rows }

function handleAdd() {
  editId.value = null; dialogTitle.value = '新增记录'
  Object.assign(form, { serverName: '', peakTime: null, cpuUsage: null, memoryUsage: null,
    diskUsage: null, networkIn: null, networkOut: null, threshold: 80, description: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  editId.value = row.id; dialogTitle.value = '编辑记录'
  Object.assign(form, {
    serverName: row.serverName, peakTime: row.peakTime, cpuUsage: row.cpuUsage,
    memoryUsage: row.memoryUsage, diskUsage: row.diskUsage, networkIn: row.networkIn,
    networkOut: row.networkOut, threshold: row.threshold, description: row.description
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await apiClient.put(`/server-peak/${editId.value}`, form)
    else await apiClient.post('/server-peak', form)
    ElMessage.success(dialogTitle.value + '成功')
    dialogVisible.value = false; loadData(); loadStats()
  } catch {}
  finally { submitting.value = false }
}

async function handleDelete(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  await ElMessageBox.confirm(`确定删除该记录（ID: ${row.id}）？`, '确认删除', { type: 'warning' })
  await apiClient.delete(`/server-peak/${row.id}`)
  ElMessage.success('删除成功'); loadData(); loadStats()
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 条记录？`, '批量删除', { type: 'warning' })
  await apiClient.delete('/server-peak/batch', { data: selectedRows.value.map(r => r.id) })
  ElMessage.success('批量删除成功'); selectedRows.value = []; loadData(); loadStats()
}

const importUrl = `${BASE_URL}/server-peak/import`

async function handleImportChange(file) {
  const isExcel = /\.(xlsx|xls)$/i.test(file.name)
  if (!isExcel) { ElMessage.error('仅支持 Excel 文件'); return }
  const fd = new FormData(); fd.append('file', file.raw)
  try {
    const res = await apiClient.post('/server-peak/import', fd,
      { headers: { 'Content-Type': 'multipart/form-data' } })
    const d = res.data || {}
    ElMessage.success(`导入完成：成功 ${d.success || 0} 条，失败 ${d.fail || 0} 条`)
    loadData(); loadStats()
  } catch {}
}

async function handleExport() {
  try {
    const token = localStorage.getItem('accessToken')
    const res = await fetch(`${BASE_URL}/server-peak/export`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = `服务器峰值_${dayjs().format('YYYYMMDD')}.xlsx`; a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('导出失败') }
}

async function handleDownloadTemplate() {
  try {
    const token = localStorage.getItem('accessToken')
    const res = await fetch(`${BASE_URL}/server-peak/template`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = '服务器峰值导入模板.xlsx'; a.click()
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

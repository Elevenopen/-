<template>
  <div class="page-container">
    <div class="page-title">公告管理</div>

    <!-- 搜索栏 -->
    <div class="card-container search-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索标题/发布人" clearable style="width:200px"
        @keyup.enter="handleSearch" />
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:150px">
        <el-option label="全部" value="" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已下线" value="ARCHIVED" />
      </el-select>
      <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
      <el-button @click="handleReset"><el-icon><RefreshLeft /></el-icon>重置</el-button>
    </div>

    <!-- 工具栏 -->
    <div class="card-container toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>发布公告</el-button>
        <el-button type="danger" plain :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" @selection-change="handleSelectionChange" size="default">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisher" label="发布人" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ formatDT(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期时间" width="160">
          <template #default="{ row }">{{ formatDT(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="可选，简短描述"
            maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width:100%">
            <el-option label="普通 NORMAL" value="NORMAL" />
            <el-option label="重要 IMPORTANT" value="IMPORTANT" />
            <el-option label="置顶 TOP" value="TOP" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布人" prop="publisher">
          <el-input v-model="form.publisher" placeholder="请输入发布人姓名" />
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker v-model="form.publishTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择发布时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker v-model="form.expireTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="可选，留空则永不过期" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="已发布 PUBLISHED" value="PUBLISHED" />
            <el-option label="草稿 DRAFT" value="DRAFT" />
            <el-option label="已下线 ARCHIVED" value="ARCHIVED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="公告详情" width="640px">
      <div class="ann-detail">
        <h2 class="ann-title">{{ viewRow.title }}</h2>
        <div class="ann-meta">
          <el-tag :type="priorityTagType(viewRow.priority)" size="small">{{ priorityLabel(viewRow.priority) }}</el-tag>
          <el-tag :type="statusTagType(viewRow.status)" size="small">{{ statusLabel(viewRow.status) }}</el-tag>
          <span class="meta-item">发布人：{{ viewRow.publisher }}</span>
          <span class="meta-item">发布时间：{{ formatDT(viewRow.publishTime) }}</span>
          <span v-if="viewRow.expireTime" class="meta-item">到期：{{ formatDT(viewRow.expireTime) }}</span>
        </div>
        <div class="ann-content" v-html="viewRow.content"></div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import apiClient from '@/stores/auth'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const selectedRows = ref([])

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const searchForm = reactive({ keyword: '', status: '' })

const dialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const editId = ref(null)
const formRef = ref(null)
const form = reactive({
  title: '', summary: '', content: '', priority: 'NORMAL',
  publisher: '', publishTime: null, expireTime: null, status: 'PUBLISHED'
})
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  publisher: [{ required: true, message: '请输入发布人', trigger: 'blur' }]
}

const viewDialogVisible = ref(false)
const viewRow = ref({})

function formatDT(dt) { return dt ? dayjs(dt).format('YYYY-MM-DD HH:mm') : '-' }
function priorityLabel(p) { return { NORMAL: '普通', IMPORTANT: '重要', TOP: '置顶' }[p] || p }
function priorityTagType(p) { return { NORMAL: 'info', IMPORTANT: 'warning', TOP: 'danger' }[p] || 'info' }
function statusLabel(s) { return { PUBLISHED: '已发布', DRAFT: '草稿', ARCHIVED: '已下线' }[s] || s }
function statusTagType(s) { return { PUBLISHED: 'success', DRAFT: 'info', ARCHIVED: 'warning' }[s] || 'info' }

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status) params.status = searchForm.status
    const res = await apiClient.get('/announcement/list', { params })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch {}
  finally { loading.value = false }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() {
  searchForm.keyword = ''; searchForm.status = ''
  pagination.pageNum = 1; loadData()
}

function handleSelectionChange(rows) { selectedRows.value = rows }

function handleAdd() {
  editId.value = null; dialogTitle.value = '发布公告'
  Object.assign(form, { title: '', summary: '', content: '', priority: 'NORMAL',
    publisher: authStore.nickname || '', publishTime: null, expireTime: null, status: 'PUBLISHED' })
  dialogVisible.value = true
}

function handleEdit(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  editId.value = row.id; dialogTitle.value = '编辑公告'
  Object.assign(form, {
    title: row.title, summary: row.summary, content: row.content,
    priority: row.priority, publisher: row.publisher,
    publishTime: row.publishTime, expireTime: row.expireTime, status: row.status
  })
  dialogVisible.value = true
}

function handleView(row) {
  viewRow.value = row; viewDialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await apiClient.put(`/announcement/${editId.value}`, form)
    else await apiClient.post('/announcement', form)
    ElMessage.success(dialogTitle.value + '成功')
    dialogVisible.value = false; loadData()
  } catch {}
  finally { submitting.value = false }
}

async function handleDelete(row) {
  if (!row?.id) { ElMessage.warning('数据加载中，请稍后重试'); return }
  await ElMessageBox.confirm(`确定删除公告「${row.title}」？`, '确认删除', { type: 'warning' })
  await apiClient.delete(`/announcement/${row.id}`)
  ElMessage.success('删除成功'); loadData()
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 条公告？`, '批量删除', { type: 'warning' })
  for (const row of selectedRows.value) {
    await apiClient.delete(`/announcement/${row.id}`)
  }
  ElMessage.success('批量删除成功'); selectedRows.value = []; loadData()
}

import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()

onMounted(loadData)
</script>

<style scoped lang="scss">
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
  .toolbar-left, .toolbar-right { display: flex; gap: 8px; flex-wrap: wrap; }
}
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }

.ann-detail {
  .ann-title { font-size: 20px; font-weight: 700; color: #303133; margin-bottom: 12px; }
  .ann-meta {
    display: flex; align-items: center; gap: 10px; margin-bottom: 16px;
    flex-wrap: wrap;
    .meta-item { font-size: 13px; color: #909399; }
  }
  .ann-content {
    font-size: 14px; color: #606266; line-height: 1.8;
    padding: 16px; background: #f9fafb; border-radius: 8px;
    white-space: pre-wrap; word-break: break-all;
  }
}
</style>

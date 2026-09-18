<template>
  <div class="page-container">
    <div class="page-title">📢 网站公告</div>

    <!-- 公告列表 -->
    <div v-if="announcements.length > 0" class="announcement-list">
      <div
        v-for="item in announcements"
        :key="item.id"
        class="announcement-card"
        @click="showDetail(item)"
      >
        <div class="ann-header">
          <el-tag v-if="item.priority === 'TOP'" type="danger" effect="dark" size="small">置顶</el-tag>
          <el-tag v-else type="info" size="small">普通</el-tag>
          <span class="ann-title">{{ item.title }}</span>
        </div>
        <div class="ann-summary" v-if="item.summary">{{ item.summary }}</div>
        <div class="ann-footer">
          <span>👤 {{ item.publisher }}</span>
          <span>📅 {{ formatTime(item.publishTime) }}</span>
          <span>👁️ {{ item.viewCount }} 浏览</span>
          <span v-if="item.expireTime" class="expire-tag">⏰ {{ formatTime(item.expireTime) }} 到期</span>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无公告" />

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import apiClient from '@/stores/auth'

const announcements = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

function formatTime(time) {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

function showDetail(item) {
  ElMessage.info(`公告「${item.title}」\n\n${item.content.replace(/<[^>]+>/g, '')}`)
}

async function loadData() {
  try {
    const res = await apiClient.get(
      `/announcement/public/active?pageNum=${pageNum.value}&pageSize=${pageSize.value}`
    )
    announcements.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {}
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announcement-card {
  background: #fff; border-radius: 8px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06); cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
}
.ann-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px;
  .ann-title { font-size: 16px; font-weight: 600; color: #303133; }
}
.ann-summary { font-size: 13px; color: #606266; margin-bottom: 12px; line-height: 1.6; }
.ann-footer { display: flex; gap: 16px; font-size: 12px; color: #909399;
  .expire-tag { color: #E6A23C; }
}
.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>

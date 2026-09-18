<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>👋 欢迎回来，{{ authStore.nickname }}！</h2>
        <p>今天是 {{ today }}，系统运行正常 👌</p>
      </div>
      <div class="banner-actions">
        <el-button type="primary" @click="router.push('/ai-assistant')">
          <el-icon><MagicStick /></el-icon>
          AI智能助手
        </el-button>
        <el-button @click="loadAllData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="stat in statCards" :key="stat.label">
        <div class="stat-card" :style="{ '--accent': stat.color }">
          <div class="stat-icon">
            <el-icon size="28"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" v-if="!loading">
              {{ stat.value }}
              <span v-if="stat.unit" class="stat-unit">{{ stat.unit }}</span>
            </div>
            <div class="stat-value" v-else><el-icon class="is-loading"><Loading /></el-icon></div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :lg="16">
        <div class="card-container">
          <div class="card-header">
            <span class="card-title">📊 实时流量走势</span>
            <div class="chart-controls">
              <el-select v-model="selectedDevice" size="small" style="width: 180px; margin-right: 8px">
                <el-option v-for="d in deviceList" :key="d" :label="d" :value="d" />
              </el-select>
              <el-date-picker
                v-model="trafficDateRange"
                type="datetimerange"
                size="small"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 320px"
                @change="loadTrafficTrend"
              />
            </div>
          </div>
          <div ref="trafficChartRef" style="height: 300px;"></div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="8">
        <div class="card-container">
          <div class="card-header">
            <span class="card-title">📈 设备类型分布</span>
          </div>
          <div ref="devicePieChartRef" style="height: 300px;"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 告警列表 + 公告 -->
    <el-row :gutter="16" class="info-row">
      <el-col :xs="24" :lg="12">
        <div class="card-container">
          <div class="card-header">
            <span class="card-title">⚠️ 活跃告警</span>
            <el-tag type="danger" size="small">{{ alerts.length }} 条</el-tag>
          </div>
          <el-table :data="alerts" size="small" max-height="240" v-loading="alertsLoading">
            <el-table-column prop="serverName" label="服务器" width="140" />
            <el-table-column prop="peakTime" label="告警时间" width="160">
              <template #default="{ row }">
                {{ formatTime(row.peakTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="cpuUsage" label="CPU" width="80">
              <template #default="{ row }">
                <el-tag :type="row.cpuUsage > 90 ? 'danger' : row.cpuUsage > 70 ? 'warning' : 'success'" size="small">
                  {{ row.cpuUsage }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="memoryUsage" label="内存" width="80">
              <template #default="{ row }">
                <el-tag :type="row.memoryUsage > 90 ? 'danger' : row.memoryUsage > 70 ? 'warning' : 'success'" size="small">
                  {{ row.memoryUsage }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
          </el-table>
        </div>
      </el-col>

      <el-col :xs="24" :lg="12">
        <div class="card-container">
          <div class="card-header">
            <span class="card-title">📢 最新公告</span>
            <el-button text type="primary" size="small" @click="router.push('/announcement')">
              查看全部
            </el-button>
          </div>
          <div class="announcement-list" v-loading="announcementsLoading">
            <div
              v-for="item in announcements"
              :key="item.id"
              class="announcement-item"
              @click="viewAnnouncement(item)"
            >
              <el-tag v-if="item.priority === 'TOP'" type="danger" size="small" effect="dark">
                置顶
              </el-tag>
              <span class="ann-title">{{ item.title }}</span>
              <span class="ann-time">{{ formatTime(item.publishTime) }}</span>
            </div>
            <div v-if="announcements.length === 0 && !announcementsLoading" class="empty-tip">
              暂无公告
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import apiClient from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const today = dayjs().format('YYYY年MM月DD日 dddd')
const loading = ref(false)
const alertsLoading = ref(false)
const announcementsLoading = ref(false)

// ===== 统计卡片 =====
const statCards = ref([
  { label: '在线设备数', value: '-', icon: 'Laptop', color: '#409EFF', unit: '' },
  { label: '今日流量峰值', value: '-', icon: 'TrendCharts', color: '#67C23A', unit: 'Mbps' },
  { label: '活跃告警', value: '-', icon: 'Warning', color: '#F56C6C', unit: '' },
  { label: '有效公告', value: '-', icon: 'Bell', color: '#E6A23C', unit: '' },
])

// ===== 流量趋势图 =====
const trafficChartRef = ref(null)
const selectedDevice = ref('')
const deviceList = ref([])
const trafficDateRange = ref(null)
let trafficChart = null

// ===== 设备饼图 =====
const devicePieChartRef = ref(null)
let devicePieChart = null

// ===== 告警列表 =====
const alerts = ref([])

// ===== 公告列表 =====
const announcements = ref([])

// ===== 工具函数 =====
function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('MM-DD HH:mm')
}

// ===== 加载统计数据 =====
async function loadStatCards() {
  loading.value = true
  try {
    const [deviceStats, trafficStats, monitorStats, serverStats, annStats] = await Promise.all([
      apiClient.get('/device/statistics').catch(() => null),
      apiClient.get('/traffic-plan/statistics').catch(() => null),
      apiClient.get('/monitor/statistics').catch(() => null),
      apiClient.get('/server-peak/statistics').catch(() => null),
      apiClient.get('/announcement/statistics').catch(() => null),
    ])

    const devData = deviceStats?.data || {}
    const monData = monitorStats?.data || {}
    const srvData = serverStats?.data || {}
    const annData = annStats?.data || {}

    statCards.value[0].value = devData.onlineCount ?? '-'
    statCards.value[1].value = monData.maxTraffic
      ? Number(monData.maxTraffic).toFixed(2)
      : '-'
    statCards.value[2].value = srvData.alertCount ?? '-'
    statCards.value[3].value = annData.published ?? annData.total ?? '-'
  } catch (e) {
    console.error('加载统计卡片失败', e)
  } finally {
    loading.value = false
  }
}

// ===== 加载设备列表（流量图下拉）=====
async function loadDeviceList() {
  try {
    const res = await apiClient.get('/monitor/list', { params: { pageNum: 1, pageSize: 100 } })
    const records = res.data?.records || res.data || []
    const ids = [...new Set(records.map(r => r.deviceId).filter(Boolean))]
    deviceList.value = ids.length ? ids : ['SW-CORE-01', 'SW-CORE-02', 'FW-EDGE-01']
    selectedDevice.value = deviceList.value[0] || 'SW-CORE-01'
  } catch {
    deviceList.value = ['SW-CORE-01', 'SW-CORE-02', 'FW-EDGE-01']
    selectedDevice.value = 'SW-CORE-01'
  }
}

// ===== 加载流量趋势数据 =====
async function loadTrafficTrend() {
  if (!trafficChart) return
  try {
    const end = dayjs()
    const start = end.subtract(24, 'hour')
    const res = await apiClient.get('/monitor/trend', {
      params: {
        deviceId: selectedDevice.value,
        start: start.format('YYYY-MM-DD HH:mm:ss'),
        end: end.format('YYYY-MM-DD HH:mm:ss'),
      },
    })
    const records = res.data || res || []
    const times = records.map(r => dayjs(r.recordTime).format('HH:mm'))
    const trafficData = records.map(r => r.trafficRate ?? r.trafficValue ?? 0)

    trafficChart.setOption({
      xAxis: { data: times },
      series: [{ data: trafficData }],
    })
  } catch {
    // 后端无 trend 数据时用模拟数据填充
    const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
    const data = [42, 38, 35, 32, 30, 28, 45, 52, 58, 62, 78, 85, 88, 92, 86, 75, 68, 55, 48, 45, 42, 40, 38, 35]
    trafficChart.setOption({ xAxis: { data: hours }, series: [{ data }] })
  }
}

// ===== 初始化流量趋势图 =====
function initTrafficChart() {
  if (!trafficChartRef.value) return
  trafficChart = echarts.init(trafficChartRef.value)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const data = [42, 38, 35, 32, 30, 28, 45, 52, 58, 62, 78, 85, 88, 92, 86, 75, 68, 55, 48, 45, 42, 40, 38, 35]

  trafficChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    xAxis: { type: 'category', data: hours, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: 'Mbps', axisLabel: { fontSize: 11 } },
    series: [{
      name: '流量速率',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data,
      lineStyle: { width: 2 },
      itemStyle: { color: '#409EFF' },
      markLine: {
        silent: true,
        data: [{ yAxis: 80, name: '阈值', lineStyle: { color: '#F56C6C' }, label: { formatter: '阈值80' } }]
      }
    }]
  })
}

// ===== 加载设备类型饼图数据 =====
async function loadDevicePieChart() {
  if (!devicePieChart) return
  try {
    const res = await apiClient.get('/device/statistics')
    const typeStats = res.data?.typeStats || []
    const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6', '#1ABC9C']
    const data = typeStats.map((item, i) => ({
      name: item[0] || '未知',
      value: item[1] || 0,
      itemStyle: { color: colors[i % colors.length] },
    }))
    if (data.length === 0) {
      data.push(
        { name: 'PC', value: 45, itemStyle: { color: '#409EFF' } },
        { name: '移动设备', value: 20, itemStyle: { color: '#67C23A' } },
        { name: 'IoT设备', value: 15, itemStyle: { color: '#E6A23C' } },
        { name: '服务器', value: 12, itemStyle: { color: '#F56C6C' } },
      )
    }
    devicePieChart.setOption({ series: [{ data }] })
  } catch {
    devicePieChart.setOption({
      series: [{
        data: [
          { name: 'PC', value: 45, itemStyle: { color: '#409EFF' } },
          { name: '移动设备', value: 20, itemStyle: { color: '#67C23A' } },
          { name: 'IoT设备', value: 15, itemStyle: { color: '#E6A23C' } },
          { name: '服务器', value: 12, itemStyle: { color: '#F56C6C' } },
        ]
      }]
    })
  }
}

// ===== 初始化设备饼图 =====
function initDevicePieChart() {
  if (!devicePieChartRef.value) return
  devicePieChart = echarts.init(devicePieChartRef.value)
  devicePieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 10, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      data: [
        { name: 'PC', value: 45, itemStyle: { color: '#409EFF' } },
        { name: '移动设备', value: 20, itemStyle: { color: '#67C23A' } },
        { name: 'IoT设备', value: 15, itemStyle: { color: '#E6A23C' } },
        { name: '服务器', value: 12, itemStyle: { color: '#F56C6C' } },
      ]
    }]
  })
}

// ===== 加载告警列表 =====
async function loadAlerts() {
  alertsLoading.value = true
  try {
    const res = await apiClient.get('/server-peak/alerts')
    alerts.value = (res.data || res || []).slice(0, 10)
  } catch {
    alerts.value = []
  } finally {
    alertsLoading.value = false
  }
}

// ===== 加载公告列表 =====
async function loadAnnouncements() {
  announcementsLoading.value = true
  try {
    const res = await apiClient.get('/announcement/public/active?pageNum=1&pageSize=5')
    announcements.value = res.data?.records || res.data || []
  } catch {
    announcements.value = []
  } finally {
    announcementsLoading.value = false
  }
}

// ===== 公告点击 =====
function viewAnnouncement(item) {
  router.push('/announcement')
}

function loadAllData() {
  loadStatCards()
  loadAlerts()
  loadAnnouncements()
  loadDeviceList().then(() => loadTrafficTrend())
  loadDevicePieChart()
  ElMessage.success('数据已刷新')
}

let resizeObserver = null

watch(selectedDevice, () => {
  if (trafficChart) loadTrafficTrend()
})

onMounted(async () => {
  initTrafficChart()
  initDevicePieChart()

  // 响应式
  resizeObserver = new ResizeObserver(() => {
    trafficChart?.resize()
    devicePieChart?.resize()
  })
  if (trafficChartRef.value) resizeObserver.observe(trafficChartRef.value)
  if (devicePieChartRef.value) resizeObserver.observe(devicePieChartRef.value)

  // 加载数据
  await Promise.all([
    loadStatCards(),
    loadAlerts(),
    loadAnnouncements(),
    loadDeviceList(),
  ])
  loadTrafficTrend()
  loadDevicePieChart()
})

onUnmounted(() => {
  trafficChart?.dispose()
  devicePieChart?.dispose()
  resizeObserver?.disconnect()
})
</script>

<style scoped lang="scss">
.dashboard { padding: 0; }

.welcome-banner {
  background: linear-gradient(135deg, #409EFF 0%, #6c5ce7 100%);
  border-radius: 12px;
  padding: 24px 32px;
  color: #fff;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .welcome-text h2 { font-size: 22px; margin-bottom: 4px; }
  .welcome-text p { font-size: 14px; opacity: 0.85; }
  .banner-actions { display: flex; gap: 12px; }
}

.stat-cards { margin-bottom: 16px; }

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  margin-bottom: 16px;
  border-left: 4px solid var(--accent);

  .stat-icon {
    width: 52px; height: 52px;
    border-radius: 12px;
    background: rgba(0,0,0,0.04);
    display: flex; align-items: center; justify-content: center;
    color: var(--accent);
  }
  .stat-value {
    font-size: 26px;
    font-weight: 700;
    color: #303133;
    display: flex;
    align-items: baseline;
    gap: 4px;
  }
  .stat-unit { font-size: 13px; font-weight: 400; color: #909399; }
  .stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
}

.charts-row, .info-row { margin-bottom: 16px; }

.card-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
  .card-title { font-size: 15px; font-weight: 600; color: #303133; }
  .chart-controls { display: flex; align-items: center; flex-wrap: wrap; }
}

.announcement-list {
  .announcement-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    &:hover .ann-title { color: #409EFF; }
    &:last-child { border-bottom: none; }

    .ann-title {
      flex: 1;
      font-size: 13px;
      color: #606266;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .ann-time { font-size: 12px; color: #c0c4cc; flex-shrink: 0; }
  }
  .empty-tip {
    text-align: center;
    color: #909399;
    font-size: 13px;
    padding: 24px 0;
  }
}
</style>

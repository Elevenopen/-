<template>
  <div class="page-container statistics-page">
    <div class="page-title">📊 数据统计分析</div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="stats-tabs" @tab-change="onTabChange">
      <!-- Tab 1: 流量规划 -->
      <el-tab-pane label="流量规划" name="traffic">
        <div class="tab-content">
          <el-row :gutter="16">
            <!-- 流量规划总览卡片 -->
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#409EFF">
                <div class="mstat-value">{{ tpStats.active || 0 }}</div>
                <div class="mstat-label">活跃规划</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#67C23A">
                <div class="mstat-value">{{ tpStats.total || 0 }}</div>
                <div class="mstat-label">总规划数</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#E6A23C">
                <div class="mstat-value">{{ tpStats.high || 0 }}</div>
                <div class="mstat-label">高优先级</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#F56C6C">
                <div class="mstat-value">{{ tpStats.expired || 0 }}</div>
                <div class="mstat-label">已过期</div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top:16px">
            <el-col :xs="24" :lg="12">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📊 规划状态分布</span></div>
                <div ref="trafficStatusChartRef" style="height:300px"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="card-container">
                <div class="card-header"><span class="card-title">🎯 优先级分布</span></div>
                <div ref="trafficPriorityChartRef" style="height:300px"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- Tab 2: 实时监控 -->
      <el-tab-pane label="实时监控" name="monitor">
        <div class="tab-content">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#409EFF">
                <div class="mstat-value">{{ monStats.totalRecords || 0 }}</div>
                <div class="mstat-label">总记录数</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#F56C6C">
                <div class="mstat-value">{{ monStats.anomalyCount || 0 }}</div>
                <div class="mstat-label">异常记录</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#67C23A">
                <div class="mstat-value">{{ monStats.maxTraffic ? Number(monStats.maxTraffic).toFixed(2) : '-' }}</div>
                <div class="mstat-label">最大流量(Mbps)</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#9B59B6">
                <div class="mstat-value">{{ monStats.avgTraffic ? Number(monStats.avgTraffic).toFixed(2) : '-' }}</div>
                <div class="mstat-label">平均流量(Mbps)</div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top:16px">
            <el-col :xs="24" :lg="16">
              <div class="card-container">
                <div class="card-header">
                  <span class="card-title">📈 流量趋势</span>
                  <div style="display:flex;gap:8px;align-items:center">
                    <el-select v-model="monDeviceId" size="small" style="width:160px" @change="loadMonitorTrend">
                      <el-option v-for="d in monDevices" :key="d" :label="d" :value="d" />
                    </el-select>
                    <el-date-picker v-model="monDateRange" type="datetimerange" size="small" range-separator="至"
                      start-placeholder="开始" end-placeholder="结束" style="width:300px" @change="loadMonitorTrend" />
                  </div>
                </div>
                <div ref="monitorTrendChartRef" style="height:280px"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="8">
              <div class="card-container">
                <div class="card-header"><span class="card-title">⚠️ 异常率</span></div>
                <div ref="monAnomalyGaugeRef" style="height:280px"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- Tab 3: 网络稳定性 -->
      <el-tab-pane label="网络稳定性" name="network">
        <div class="tab-content">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#67C23A">
                <div class="mstat-value">{{ nsStats.healthy || 0 }}</div>
                <div class="mstat-label">健康节点</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#E6A23C">
                <div class="mstat-value">{{ nsStats.warning || 0 }}</div>
                <div class="mstat-label">警告节点</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#F56C6C">
                <div class="mstat-value">{{ nsStats.critical || 0 }}</div>
                <div class="mstat-label">严重节点</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#409EFF">
                <div class="mstat-value">{{ nsStats.avgUptimeRate ? Number(nsStats.avgUptimeRate).toFixed(2) + '%' : '-' }}</div>
                <div class="mstat-label">平均可用率</div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top:16px">
            <el-col :xs="24" :lg="8">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📊 状态分布</span></div>
                <div ref="nsPieChartRef" style="height:280px"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="16">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📈 可用率趋势</span></div>
                <div ref="nsTrendChartRef" style="height:280px"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- Tab 4: 设备使用 -->
      <el-tab-pane label="设备使用" name="device">
        <div class="tab-content">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#409EFF">
                <div class="mstat-value">{{ duStats.onlineCount || 0 }}</div>
                <div class="mstat-label">在线设备</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#909399">
                <div class="mstat-value">{{ duStats.offlineCount || 0 }}</div>
                <div class="mstat-label">离线设备</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#67C23A">
                <div class="mstat-value">{{ duStats.totalCount || 0 }}</div>
                <div class="mstat-label">总设备数</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#E6A23C">
                <div class="mstat-value">{{ duTypeList.length || 0 }}</div>
                <div class="mstat-label">设备类型数</div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top:16px">
            <el-col :xs="24" :lg="8">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📊 设备类型分布</span></div>
                <div ref="deviceTypePieRef" style="height:280px"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="16">
              <div class="card-container">
                <div class="card-header"><span class="card-title">🏆 在线时长 TOP 10</span></div>
                <div ref="deviceTopBarRef" style="height:280px"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- Tab 5: 服务器峰值 -->
      <el-tab-pane label="服务器峰值" name="server">
        <div class="tab-content">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#F56C6C">
                <div class="mstat-value">{{ spStats.alertCount || 0 }}</div>
                <div class="mstat-label">告警总数</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#E6A23C">
                <div class="mstat-value">{{ spStats.maxCpuUsage ? Number(spStats.maxCpuUsage).toFixed(1) + '%' : '-' }}</div>
                <div class="mstat-label">最高CPU</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="card-container" style="padding:16px;border-radius:8px;box-shadow:0 1px 3px rgba(0,0,0,.06)">
                <div class="card-header"><span class="card-title">🔴 活跃告警</span></div>
                <el-table :data="spStats.recentAlerts || []" size="small" max-height="120">
                  <el-table-column prop="serverName" label="服务器" />
                  <el-table-column prop="cpuUsage" label="CPU">
                    <template #default="{row}">
                      <el-progress :percentage="Number(row.cpuUsage)" :color="getProgressColor(row.cpuUsage)" :stroke-width="8" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="peakTime" label="时间" width="130">
                    <template #default="{row}">{{ formatTime(row.peakTime) }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top:16px">
            <el-col :xs="24" :lg="12">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📊 服务器 CPU/内存 TOP 10</span></div>
                <div ref="spServerBarRef" style="height:300px"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📈 告警趋势</span></div>
                <div ref="spTrendChartRef" style="height:300px"></div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- Tab 6: 公告管理 -->
      <el-tab-pane label="公告统计" name="announcement">
        <div class="tab-content">
          <el-row :gutter="16">
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#67C23A">
                <div class="mstat-value">{{ annStats.published || 0 }}</div>
                <div class="mstat-label">已发布</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#909399">
                <div class="mstat-value">{{ annStats.offline || 0 }}</div>
                <div class="mstat-label">已下线</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#E6A23C">
                <div class="mstat-value">{{ annStats.draft || 0 }}</div>
                <div class="mstat-label">草稿</div>
              </div>
            </el-col>
            <el-col :xs="24" :sm="6">
              <div class="mini-stat-card" style="--c:#409EFF">
                <div class="mstat-value">{{ annStats.total || 0 }}</div>
                <div class="mstat-label">总公告数</div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" style="margin-top:16px">
            <el-col :xs="24" :lg="12">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📊 公告状态分布</span></div>
                <div ref="annPieChartRef" style="height:300px"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="card-container">
                <div class="card-header"><span class="card-title">📋 最新公告</span></div>
                <el-table :data="annList" size="small" max-height="300">
                  <el-table-column prop="title" label="标题" show-overflow-tooltip />
                  <el-table-column prop="priority" label="优先级" width="100">
                    <template #default="{row}">
                      <el-tag :type="row.priority==='TOP'?'danger':row.priority==='HIGH'?'warning':'info'" size="small">
                        {{ row.priority }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="80">
                    <template #default="{row}">
                      <el-tag :type="row.status==='PUBLISHED'?'success':'warning'" size="small">
                        {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="publishTime" label="发布时间" width="130">
                    <template #default="{row}">{{ formatTime(row.publishTime) }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import apiClient from '@/stores/auth'

const activeTab = ref('traffic')

// ===== 图表 refs =====
const trafficStatusChartRef = ref(null)
const trafficPriorityChartRef = ref(null)
const monitorTrendChartRef = ref(null)
const monAnomalyGaugeRef = ref(null)
const nsPieChartRef = ref(null)
const nsTrendChartRef = ref(null)
const deviceTypePieRef = ref(null)
const deviceTopBarRef = ref(null)
const spServerBarRef = ref(null)
const spTrendChartRef = ref(null)
const annPieChartRef = ref(null)

// ===== 图表实例 =====
const charts = {}

// ===== 统计数据 =====
const tpStats = ref({})
const monStats = ref({})
const nsStats = ref({})
const duStats = ref({})
const duTypeList = ref([])
const spStats = ref({})
const annStats = ref({})
const annList = ref([])

// ===== 监控趋势筛选 =====
const monDeviceId = ref('')
const monDevices = ref([])
const monDateRange = ref(null)

// ===== 工具函数 =====
function formatTime(t) {
  return t ? dayjs(t).format('MM-DD HH:mm') : '-'
}

function getProgressColor(val) {
  if (val > 90) return '#F56C6C'
  if (val > 70) return '#E6A23C'
  return '#67C23A'
}

// ===== Tab 切换 =====
async function onTabChange(tab) {
  await nextTick()
  resizeAllCharts()
  if (tab === 'traffic') { resizeChart('trafficStatus'); resizeChart('trafficPriority') }
  if (tab === 'monitor') { resizeChart('monitorTrend'); resizeChart('monAnomaly') }
  if (tab === 'network') { resizeChart('nsPie'); resizeChart('nsTrend') }
  if (tab === 'device') { resizeChart('deviceTypePie'); resizeChart('deviceTopBar') }
  if (tab === 'server') { resizeChart('spServerBar'); resizeChart('spTrend') }
  if (tab === 'announcement') resizeChart('annPie')
}

function resizeAllCharts() {
  Object.values(charts).forEach(c => c?.resize())
}

function initChart(refKey, refEl) {
  if (!refEl) return null
  if (charts[refKey]) { charts[refKey].dispose() }
  charts[refKey] = echarts.init(refEl)
  return charts[refKey]
}

// ===== 1. 流量规划图表 =====
function initTrafficCharts() {
  // 状态分布饼图
  const c1 = initChart('trafficStatus', trafficStatusChartRef.value)
  if (c1) {
    c1.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 5, left: 'center' },
      series: [{
        type: 'pie', radius: ['45%', '75%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data: [
          { name: '活跃', value: tpStats.value.active || 0, itemStyle: { color: '#67C23A' } },
          { name: '停用', value: tpStats.value.inactive || 0, itemStyle: { color: '#909399' } },
          { name: '已过期', value: tpStats.value.expired || 0, itemStyle: { color: '#F56C6C' } },
        ]
      }]
    })
  }

  // 优先级柱状图
  const c2 = initChart('trafficPriority', trafficPriorityChartRef.value)
  if (c2) {
    c2.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: 10, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: ['低优先级', '中优先级', '高优先级'] },
      yAxis: { type: 'value', name: '数量' },
      series: [{
        type: 'bar',
        data: [
          { value: tpStats.value.low || 0, itemStyle: { color: '#67C23A' } },
          { value: tpStats.value.medium || 0, itemStyle: { color: '#E6A23C' } },
          { value: tpStats.value.high || 0, itemStyle: { color: '#F56C6C' } },
        ],
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        barWidth: '50%',
      }]
    })
  }
}

// ===== 2. 监控趋势图表 =====
function initMonitorTrendChart() {
  const c = initChart('monitorTrend', monitorTrendChartRef.value)
  if (c) {
    c.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['流量'], top: 5 },
      grid: { top: 30, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', name: 'Mbps' },
      series: [{ name: '流量', type: 'line', smooth: true, areaStyle: { opacity: 0.2 }, data: [], itemStyle: { color: '#409EFF' } }]
    })
  }
  loadMonitorTrend()
}

async function loadMonitorTrend() {
  const c = charts['monitorTrend']
  if (!c) return
  try {
    const end = dayjs()
    const start = monDateRange.value
      ? monDateRange.value[0]
      : end.subtract(24, 'hour')
    const endTime = monDateRange.value ? monDateRange.value[1] : end
    const res = await apiClient.get('/monitor/trend', {
      params: {
        deviceId: monDeviceId.value,
        start: dayjs(start).format('YYYY-MM-DD HH:mm:ss'),
        end: dayjs(endTime).format('YYYY-MM-DD HH:mm:ss'),
      },
    })
    const records = res.data || []
    c.setOption({
      xAxis: { data: records.map(r => dayjs(r.recordTime).format('MM-DD HH:mm')) },
      series: [{ data: records.map(r => r.trafficRate ?? r.trafficValue ?? 0) }]
    })
  } catch {
    const hours = Array.from({ length: 12 }, (_, i) => `${(i * 2)}:00`)
    const data = [42, 55, 68, 72, 88, 85, 78, 65, 55, 48, 45, 40]
    c.setOption({ xAxis: { data: hours }, series: [{ data }] })
  }
}

function initMonitorGauge() {
  const c = initChart('monAnomaly', monAnomalyGaugeRef.value)
  if (c) {
    const total = monStats.value.totalRecords || 1
    const anomaly = monStats.value.anomalyCount || 0
    const rate = Math.min(100, ((anomaly / total) * 100).toFixed(1))
    c.setOption({
      series: [{
        type: 'gauge',
        center: ['50%', '60%'],
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        splitNumber: 4,
        itemStyle: { color: rate > 20 ? '#F56C6C' : rate > 5 ? '#E6A23C' : '#67C23A' },
        progress: { show: true, width: 18 },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 18, color: [[1, '#E4E7ED']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 28,
          formatter: `{value}%`,
          color: '#303133',
          offsetCenter: [0, '30%'],
        },
        data: [{ value: Number(rate) }]
      }]
    })
  }
}

// ===== 3. 网络稳定性图表 =====
function initNetworkCharts() {
  const pie = initChart('nsPie', nsPieChartRef.value)
  if (pie) {
    pie.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 5, left: 'center' },
      series: [{
        type: 'pie', radius: ['45%', '75%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data: [
          { name: '健康', value: nsStats.value.healthy || 0, itemStyle: { color: '#67C23A' } },
          { name: '警告', value: nsStats.value.warning || 0, itemStyle: { color: '#E6A23C' } },
          { name: '严重', value: nsStats.value.critical || 0, itemStyle: { color: '#F56C6C' } },
        ]
      }]
    })
  }

  const trend = initChart('nsTrend', nsTrendChartRef.value)
  if (trend) {
    trend.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['可用率', '平均延迟', '丢包率'], top: 5 },
      grid: { top: 35, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: [] },
      yAxis: [
        { type: 'value', name: '可用率%', min: 0, max: 100 },
        { type: 'value', name: '延迟/丢包', position: 'right' }
      ],
      series: [
        { name: '可用率', type: 'line', data: [], itemStyle: { color: '#409EFF' }, smooth: true },
        { name: '平均延迟', type: 'bar', yAxisIndex: 1, data: [], itemStyle: { color: '#E6A23C', opacity: 0.7 } },
      ]
    })
  }
}

// ===== 4. 设备使用图表 =====
function initDeviceCharts() {
  const pie = initChart('deviceTypePie', deviceTypePieRef.value)
  if (pie) {
    const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#9B59B6']
    const data = (duStats.value.typeStats || []).map((item, i) => ({
      name: item[0] || '未知',
      value: item[1] || 0,
      itemStyle: { color: colors[i % colors.length] },
    }))
    if (data.length === 0) {
      data.push(
        { name: 'PC', value: 45, itemStyle: { color: '#409EFF' } },
        { name: '移动设备', value: 22, itemStyle: { color: '#67C23A' } },
        { name: 'IoT', value: 15, itemStyle: { color: '#E6A23C' } },
        { name: '服务器', value: 10, itemStyle: { color: '#F56C6C' } },
      )
    }
    pie.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 5, left: 'center' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data,
      }]
    })
  }

  const bar = initChart('deviceTopBar', deviceTopBarRef.value)
  if (bar) {
    bar.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { top: 10, right: 20, bottom: 30, left: 120 },
      xAxis: { type: 'value', name: '在线时长(h)' },
      yAxis: { type: 'category', data: [] },
      series: [{
        type: 'bar',
        data: [],
        itemStyle: { borderRadius: [0, 4, 4, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#67C23A' }
        ]) },
        barWidth: '60%',
      }]
    })
  }
}

// ===== 5. 服务器峰值图表 =====
function initServerPeakCharts() {
  const bar = initChart('spServerBar', spServerBarRef.value)
  if (bar) {
    bar.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['CPU', '内存'], top: 5 },
      grid: { top: 30, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', name: '%', max: 100 },
      series: [
        { name: 'CPU', type: 'bar', data: [], itemStyle: { color: '#F56C6C', borderRadius: [4, 4, 0, 0] } },
        { name: '内存', type: 'bar', data: [], itemStyle: { color: '#E6A23C', borderRadius: [4, 4, 0, 0] } },
      ]
    })
  }

  const trend = initChart('spTrend', spTrendChartRef.value)
  if (trend) {
    trend.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['告警数'], top: 5 },
      grid: { top: 30, right: 20, bottom: 30, left: 50 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', name: '告警数' },
      series: [{
        name: '告警数', type: 'bar',
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#F56C6C' },
          { offset: 1, color: '#fde8e8' }
        ]), borderRadius: [4, 4, 0, 0] },
        data: []
      }]
    })
  }
}

// ===== 6. 公告统计图表 =====
function initAnnouncementCharts() {
  const c = initChart('annPie', annPieChartRef.value)
  if (c) {
    c.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 5, left: 'center' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data: [
          { name: '已发布', value: annStats.value.published || 0, itemStyle: { color: '#67C23A' } },
          { name: '草稿', value: annStats.value.draft || 0, itemStyle: { color: '#E6A23C' } },
          { name: '已下线', value: annStats.value.offline || 0, itemStyle: { color: '#909399' } },
        ]
      }]
    })
  }
}

// ===== 加载数据 =====
async function loadAllData() {
  await Promise.all([
    loadTrafficPlanData(),
    loadMonitorData(),
    loadNetworkStabilityData(),
    loadDeviceData(),
    loadServerPeakData(),
    loadAnnouncementData(),
  ])
}

async function loadTrafficPlanData() {
  try {
    const res = await apiClient.get('/traffic-plan/statistics')
    tpStats.value = res.data || {}
    initTrafficCharts()
  } catch {}
}

async function loadMonitorData() {
  try {
    const [statsRes, listRes] = await Promise.all([
      apiClient.get('/monitor/statistics'),
      apiClient.get('/monitor/list', { params: { pageNum: 1, pageSize: 100 } }),
    ])
    monStats.value = statsRes.data || {}
    const records = listRes.data?.records || listRes.data || []
    monDevices.value = [...new Set(records.map(r => r.deviceId).filter(Boolean))]
    monDeviceId.value = monDevices.value[0] || ''
    initMonitorTrendChart()
    initMonitorGauge()
  } catch {}
}

async function loadNetworkStabilityData() {
  try {
    const end = dayjs()
    const start = end.subtract(30, 'day')
    const res = await apiClient.get('/network-stability/statistics', {
      params: { start: start.format('YYYY-MM-DD HH:mm:ss'), end: end.format('YYYY-MM-DD HH:mm:ss') }
    })
    nsStats.value = res.data || {}
    initNetworkCharts()
    // 趋势数据
    try {
      const trendRes = await apiClient.get('/network-stability/trend', {
        params: { start: start.format('YYYY-MM-DD HH:mm:ss'), end: end.format('YYYY-MM-DD HH:mm:ss') }
      })
      const trend = trendRes.data || []
      if (charts['nsTrend']) {
        charts['nsTrend'].setOption({
          xAxis: { data: trend.map(r => dayjs(r.checkTime).format('MM-DD')) },
          series: [
            { data: trend.map(r => r.uptimeRate ?? 0) },
            { data: trend.map(r => r.avgLatency ?? 0) },
          ]
        })
      }
    } catch {}
  } catch {}
}

async function loadDeviceData() {
  try {
    const [statsRes, topRes] = await Promise.all([
      apiClient.get('/device/statistics'),
      apiClient.get('/device/top/duration', { params: { limit: 10 } }),
    ])
    duStats.value = statsRes.data || {}
    duTypeList.value = duStats.value.typeStats || []
    initDeviceCharts()
    // TOP 10 在线时长
    try {
      const topData = topRes.data || topRes || []
      if (charts['deviceTopBar']) {
        charts['deviceTopBar'].setOption({
          yAxis: { data: topData.map(d => d.deviceName || d.deviceId || '-') },
          series: [{ data: topData.map(d => ((d.onlineDuration || 0) / 3600).toFixed(1)) }]
        })
      }
    } catch {}
  } catch {}
}

async function loadServerPeakData() {
  try {
    const end = dayjs()
    const start = end.subtract(30, 'day')
    const res = await apiClient.get('/server-peak/statistics', {
      params: { start: start.format('YYYY-MM-DD HH:mm:ss'), end: end.format('YYYY-MM-DD HH:mm:ss') }
    })
    spStats.value = res.data || {}
    initServerPeakCharts()
    // 趋势
    try {
      const trendRes = await apiClient.get('/server-peak/trend', {
        params: { start: start.format('YYYY-MM-DD HH:mm:ss'), end: end.format('YYYY-MM-DD HH:mm:ss') }
      })
      const trend = trendRes.data || []
      if (charts['spTrend']) {
        charts['spTrend'].setOption({
          xAxis: { data: trend.map(r => dayjs(r.peakTime).format('MM-DD')) },
          series: [{ data: trend.map(r => r.alertFlag || 0) }]
        })
      }
      if (charts['spServerBar'] && trend.length > 0) {
        charts['spServerBar'].setOption({
          xAxis: { data: trend.map(r => r.serverName || '-').slice(0, 10) },
          series: [
            { data: trend.map(r => r.cpuUsage || 0).slice(0, 10) },
            { data: trend.map(r => r.memoryUsage || 0).slice(0, 10) },
          ]
        })
      }
    } catch {}
  } catch {}
}

async function loadAnnouncementData() {
  try {
    const [statsRes, listRes] = await Promise.all([
      apiClient.get('/announcement/statistics'),
      apiClient.get('/announcement/list', { params: { pageNum: 1, pageSize: 10 } }),
    ])
    annStats.value = statsRes.data || {}
    annList.value = listRes.data?.records || listRes.data || []
    initAnnouncementCharts()
  } catch {}
}

// ===== 生命周期 =====
onMounted(() => {
  loadAllData()
  window.addEventListener('resize', resizeAllCharts)
})

onUnmounted(() => {
  Object.values(charts).forEach(c => c?.dispose())
  window.removeEventListener('resize', resizeAllCharts)
})
</script>

<style scoped lang="scss">
.statistics-page {
  .stats-tabs {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  }
}

.tab-content { padding-top: 8px; }

.mini-stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border-left: 4px solid var(--c);

  .mstat-value {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
    line-height: 1.2;
  }
  .mstat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 6px;
  }
}

.card-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
  .card-title { font-size: 15px; font-weight: 600; color: #303133; }
}
</style>

<template>
  <div class="app-container dashboard-page">
    <el-form :inline="true" size="small" class="filter-form">
      <el-form-item label="统计范围">
        <el-select v-model="days" @change="loadData">
          <el-option label="近 7 天" :value="7" />
          <el-option label="近 14 天" :value="14" />
          <el-option label="近 30 天" :value="30" />
          <el-option label="近 90 天" :value="90" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-refresh" size="mini" @click="loadData">刷新</el-button>
        <el-button icon="el-icon-document" size="mini" @click="goLog">执行日志</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">总调用次数</div>
          <div class="stat-value">{{ data.totalCalls || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card success">
          <div class="stat-label">成功率</div>
          <div class="stat-value">{{ (data.successRate || 0).toFixed(1) }}%</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">平均耗时</div>
          <div class="stat-value">{{ data.avgDurationMs || 0 }}<span class="unit">ms</span></div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card danger">
          <div class="stat-label">失败次数</div>
          <div class="stat-value">{{ data.failCount || 0 }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <div class="chart-card">
          <div class="chart-title">调用趋势</div>
          <div ref="trendChart" class="chart-box" />
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="chart-card">
          <div class="chart-title">链路调用 Top</div>
          <div ref="chainChart" class="chart-box" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="chart-title">失败 Top 10</div>
          <el-table :data="data.failTop || []" size="small" max-height="320">
            <el-table-column label="链路" prop="chainName" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="错误信息" prop="errorMessage" min-width="200" :show-overflow-tooltip="true" />
            <el-table-column label="次数" prop="count" width="70" align="center" />
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="table-card">
          <div class="chart-title">慢调用 Top 10</div>
          <el-table :data="data.slowTop || []" size="small" max-height="320">
            <el-table-column label="链路" prop="chainName" width="120" :show-overflow-tooltip="true" />
            <el-table-column label="耗时(ms)" prop="durationMs" width="90" align="center" />
            <el-table-column label="请求ID" prop="requestId" min-width="140" :show-overflow-tooltip="true" />
            <el-table-column label="时间" prop="createTime" width="150" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboard } from '@/api/liteflow/platform'

export default {
  name: 'LfDashboard',
  data() {
    return {
      days: 7,
      loading: false,
      data: {},
      trendChart: null,
      chainChart: null
    }
  },
  mounted() {
    this.loadData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.trendChart) {
      this.trendChart.dispose()
    }
    if (this.chainChart) {
      this.chainChart.dispose()
    }
  },
  methods: {
    loadData() {
      this.loading = true
      getDashboard(this.days).then(res => {
        this.data = res.data || {}
        this.$nextTick(() => {
          this.renderTrendChart()
          this.renderChainChart()
        })
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    renderTrendChart() {
      const el = this.$refs.trendChart
      if (!el) return
      if (!this.trendChart) {
        this.trendChart = echarts.init(el)
      }
      const trend = this.data.trend || []
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['总调用', '成功', '失败'] },
        grid: { left: 40, right: 20, top: 40, bottom: 30 },
        xAxis: { type: 'category', data: trend.map(t => t.statDate) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          { name: '总调用', type: 'line', smooth: true, data: trend.map(t => t.total), itemStyle: { color: '#409EFF' } },
          { name: '成功', type: 'line', smooth: true, data: trend.map(t => t.successCount), itemStyle: { color: '#67C23A' } },
          { name: '失败', type: 'line', smooth: true, data: trend.map(t => t.failCount), itemStyle: { color: '#F56C6C' } }
        ]
      })
    },
    renderChainChart() {
      const el = this.$refs.chainChart
      if (!el) return
      if (!this.chainChart) {
        this.chainChart = echarts.init(el)
      }
      const stats = (this.data.chainStats || []).slice(0, 10)
      this.chainChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 100, right: 20, top: 20, bottom: 30 },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: stats.map(s => s.chainName).reverse(), axisLabel: { width: 80, overflow: 'truncate' } },
        series: [{
          type: 'bar',
          data: stats.map(s => s.total).reverse(),
          itemStyle: { color: '#409EFF' },
          label: { show: true, position: 'right' }
        }]
      })
    },
    handleResize() {
      if (this.trendChart) this.trendChart.resize()
      if (this.chainChart) this.chainChart.resize()
    },
    goLog() {
      this.$router.push('/liteflow/log')
    }
  }
}
</script>

<style scoped>
.filter-form { margin-bottom: 12px; }
.stat-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  margin-bottom: 12px;
}
.stat-card.success .stat-value { color: #67C23A; }
.stat-card.danger .stat-value { color: #F56C6C; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; }
.stat-value .unit { font-size: 14px; font-weight: 400; margin-left: 4px; color: #909399; }
.chart-card, .table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  margin-bottom: 16px;
}
.chart-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #303133; }
.chart-box { height: 300px; }
</style>

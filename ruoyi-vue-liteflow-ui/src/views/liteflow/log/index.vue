<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="请求ID" prop="requestId">
        <el-input v-model="queryParams.requestId" placeholder="requestId" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="链路ID" prop="chainName">
        <el-input v-model="queryParams.chainName" placeholder="chainName" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="结果" prop="success">
        <el-select v-model="queryParams.success" placeholder="全部" clearable>
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="执行时间">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="yyyy-MM-dd HH:mm:ss"
          size="small"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['liteflow:log:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete-solid" size="mini" @click="handleClean" v-hasPermi="['liteflow:log:remove']">清空</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="logList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="请求ID" prop="requestId" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="链路ID" prop="chainName" width="130" :show-overflow-tooltip="true" />
      <el-table-column label="结果" prop="success" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'" size="mini">{{ scope.row.success === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时(ms)" prop="durationMs" width="90" align="center" />
      <el-table-column label="执行人" prop="createBy" width="90" align="center" />
      <el-table-column label="执行时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="showDetail(scope.row)" v-hasPermi="['liteflow:log:query']">详情</el-button>
          <el-button size="mini" type="text" @click="goEditor(scope.row)" v-hasPermi="['liteflow:editor:view']">
            {{ scope.row.success === 0 ? '定位失败' : '定位链路' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="执行日志详情" :visible.sync="detailOpen" width="780px" append-to-body>
      <el-descriptions v-if="current" :column="2" border size="small">
        <el-descriptions-item label="请求ID" :span="2">{{ current.requestId }}</el-descriptions-item>
        <el-descriptions-item label="链路ID">{{ current.chainName }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="结果">{{ current.success === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="执行人">{{ current.createBy }}</el-descriptions-item>
        <el-descriptions-item label="消息" :span="2">{{ current.message || '-' }}</el-descriptions-item>
        <el-descriptions-item label="步骤" :span="2">{{ current.executeStepStr || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="current.failedNodeId" label="失败节点" :span="2">
          <el-tag type="danger" size="mini">{{ current.failedNodeId }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误" :span="2">{{ current.errorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="current && current.paramJson" style="margin-top:12px">
        <div class="detail-label">请求参数</div>
        <pre class="detail-pre">{{ formatJson(current.paramJson) }}</pre>
      </div>
      <div v-if="current && current.contextJson" style="margin-top:12px">
        <div class="detail-label">上下文结果</div>
        <pre class="detail-pre">{{ formatJson(current.contextJson) }}</pre>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button v-if="current && current.executeStepStr" type="primary" @click="goEditorFromDetail" v-hasPermi="['liteflow:editor:view']">定位到画布</el-button>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listExecLog, getExecLog, delExecLog, cleanExecLog } from '@/api/liteflow/platform'

export default {
  name: 'LfExecLog',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      logList: [],
      ids: [],
      multiple: true,
      detailOpen: false,
      current: null,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        requestId: undefined,
        chainName: undefined,
        success: undefined
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        params.params = {
          beginTime: this.dateRange[0],
          endTime: this.dateRange[1]
        }
      }
      listExecLog(params).then(res => {
        this.logList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    showDetail(row) {
      getExecLog(row.id).then(res => {
        this.current = res.data
        this.detailOpen = true
      })
    },
    goEditor(row) {
      const query = { chainId: row.chainName }
      if (row.executeStepStr) {
        query.highlightSteps = row.executeStepStr
        query.success = String(row.success)
        if (row.failedNodeId) {
          query.failedNode = row.failedNodeId
        }
      }
      this.$router.push({ path: '/liteflow/editor', query })
    },
    goEditorFromDetail() {
      if (!this.current) {
        return
      }
      this.detailOpen = false
      this.goEditor(this.current)
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('确认删除选中的执行日志？').then(() => delExecLog(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleClean() {
      this.$modal.confirm('确认清空全部执行日志？').then(() => cleanExecLog()).then(() => {
        this.getList()
        this.$modal.msgSuccess('清空成功')
      }).catch(() => {})
    },
    formatJson(str) {
      try {
        return JSON.stringify(JSON.parse(str), null, 2)
      } catch (e) {
        return str
      }
    }
  }
}
</script>

<style scoped>
.detail-label { font-weight: 600; margin-bottom: 6px; font-size: 13px; }
.detail-pre {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  margin: 0;
}
</style>

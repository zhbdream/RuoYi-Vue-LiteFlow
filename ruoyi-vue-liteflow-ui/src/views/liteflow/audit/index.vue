<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="链路ID" prop="chainName">
        <el-input v-model="queryParams.chainName" placeholder="chainName" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="操作类型" prop="actionType">
        <el-select v-model="queryParams.actionType" placeholder="全部" clearable>
          <el-option label="新建 CREATE" value="CREATE" />
          <el-option label="编辑 EDIT" value="EDIT" />
          <el-option label="发布 PUBLISH" value="PUBLISH" />
          <el-option label="导入 IMPORT" value="IMPORT" />
          <el-option label="删除 DELETE" value="DELETE" />
          <el-option label="回滚 ROLLBACK" value="ROLLBACK" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作人" prop="operateBy">
        <el-input v-model="queryParams.operateBy" placeholder="操作人" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="操作时间">
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
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['liteflow:audit:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="auditList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="链路ID" prop="chainName" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="操作" prop="actionType" width="100" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" :type="actionTagType(scope.row.actionType)">{{ actionLabel(scope.row.actionType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本" prop="version" width="70" align="center" />
      <el-table-column label="草稿" prop="draftFlag" width="80" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.draftFlag === '1'">草稿</span>
          <span v-else-if="scope.row.draftFlag === '0'">已发布</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作人" prop="operateBy" width="100" align="center" />
      <el-table-column label="备注" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作时间" prop="createTime" width="160" align="center" />
      <el-table-column label="详情" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="showDetail(scope.row)" v-hasPermi="['liteflow:audit:query']">EL对比</el-button>
          <el-button size="mini" type="text" @click="goEditor(scope.row)" v-hasPermi="['liteflow:editor:view']">编排</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="规则变更详情" :visible.sync="detailOpen" width="900px" append-to-body>
      <el-descriptions v-if="current" :column="2" border size="small">
        <el-descriptions-item label="链路ID">{{ current.chainName }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ actionLabel(current.actionType) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ current.operateBy }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-row :gutter="12" style="margin-top:12px">
        <el-col :span="12">
          <div class="detail-label">变更前 EL</div>
          <pre class="detail-pre">{{ current && current.elBefore ? current.elBefore : '（无）' }}</pre>
        </el-col>
        <el-col :span="12">
          <div class="detail-label">变更后 EL</div>
          <pre class="detail-pre">{{ current && current.elAfter ? current.elAfter : '（无）' }}</pre>
        </el-col>
      </el-row>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listChainAudit, getChainAudit, delChainAudit } from '@/api/liteflow/platform'

const ACTION_MAP = {
  CREATE: '新建',
  EDIT: '编辑',
  PUBLISH: '发布',
  IMPORT: '导入',
  DELETE: '删除',
  ROLLBACK: '回滚'
}

export default {
  name: 'LfChainAudit',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      auditList: [],
      ids: [],
      multiple: true,
      detailOpen: false,
      current: null,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        chainName: undefined,
        actionType: undefined,
        operateBy: undefined
      }
    }
  },
  created() {
    if (this.$route.query.chainName) {
      this.queryParams.chainName = this.$route.query.chainName
    }
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
      listChainAudit(params).then(res => {
        this.auditList = res.rows
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
      getChainAudit(row.id).then(res => {
        this.current = res.data
        this.detailOpen = true
      })
    },
    goEditor(row) {
      this.$router.push({ path: '/liteflow/editor', query: { chainId: row.chainName } })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('确认删除选中的审计记录？').then(() => delChainAudit(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    actionLabel(type) {
      return ACTION_MAP[type] || type
    },
    actionTagType(type) {
      if (type === 'PUBLISH') return 'success'
      if (type === 'DELETE') return 'danger'
      if (type === 'EDIT') return 'warning'
      if (type === 'ROLLBACK') return 'warning'
      return 'info'
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
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

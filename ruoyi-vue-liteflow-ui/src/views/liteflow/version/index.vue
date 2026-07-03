<template>
  <div class="app-container">
    <el-alert v-if="chainInfo" type="info" :closable="false" show-icon style="margin-bottom:12px">
      <template slot="title">
        链路 <strong>{{ chainInfo.chainName }}</strong>
        — 当前版本 v{{ chainInfo.version || 1 }}
        <el-tag size="mini" :type="chainInfo.draftFlag === '1' ? 'warning' : 'success'" style="margin-left:8px">
          {{ chainInfo.draftFlag === '1' ? '草稿' : '已发布' }}
        </el-tag>
      </template>
    </el-alert>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="链路ID" prop="chainName">
        <el-input v-model="queryParams.chainName" placeholder="chainName" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5" v-if="chainInfo">
        <el-button type="primary" plain icon="el-icon-s-operation" size="mini" @click="goEditor" v-hasPermi="['liteflow:editor:view']">打开编排</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="versionList">
      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="版本号" prop="version" width="90" align="center">
        <template slot-scope="scope">
          <el-tag type="primary" size="mini">v{{ scope.row.version }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="链路ID" prop="chainName" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="发布人" prop="publishBy" width="100" align="center" />
      <el-table-column label="发布时间" prop="createTime" width="160" align="center" />
      <el-table-column label="备注" prop="remark" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="showDetail(scope.row)" v-hasPermi="['liteflow:chain:query']">详情</el-button>
          <el-button size="mini" type="text" @click="showCompare(scope.row)" v-hasPermi="['liteflow:chain:query']">对比当前</el-button>
          <el-button size="mini" type="text" @click="handleRollback(scope.row)" v-hasPermi="['liteflow:chain:edit']">回滚</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="版本快照详情" :visible.sync="detailOpen" width="820px" append-to-body>
      <el-descriptions v-if="current" :column="2" border size="small">
        <el-descriptions-item label="版本">v{{ current.version }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ current.publishBy }}</el-descriptions-item>
        <el-descriptions-item label="发布时间" :span="2">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:12px">
        <div class="detail-label">EL 快照</div>
        <pre class="detail-pre">{{ current && current.elData ? current.elData : '（空）' }}</pre>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="版本对比（快照 vs 当前）" :visible.sync="compareOpen" width="960px" append-to-body>
      <el-alert v-if="compareHasDiff" type="warning" :closable="false" show-icon style="margin-bottom:12px">
        检测到 EL 差异，高亮行：绿色=新增、红色=删除、灰色=相同
      </el-alert>
      <el-row :gutter="12" v-if="compareVersion">
        <el-col :span="12">
          <div class="detail-label">快照 v{{ compareVersion.version }}（{{ compareVersion.createTime }}）</div>
          <div class="diff-pre">
            <div
              v-for="(row, idx) in compareDiff.left"
              :key="'l-' + idx"
              class="diff-line"
              :class="'diff-' + row.type"
            >
              <span class="diff-ln">{{ row.line != null ? row.line : ' ' }}</span>
              <span class="diff-text">{{ row.text || ' ' }}</span>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="detail-label">当前链路（v{{ chainInfo ? chainInfo.version : '-' }}）</div>
          <div class="diff-pre">
            <div
              v-for="(row, idx) in compareDiff.right"
              :key="'r-' + idx"
              class="diff-line"
              :class="'diff-' + row.type"
            >
              <span class="diff-ln">{{ row.line != null ? row.line : ' ' }}</span>
              <span class="diff-text">{{ row.text || ' ' }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
      <div slot="footer" class="dialog-footer">
        <el-button @click="compareOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getChain, listChainVersions, getChainVersion, rollbackChainVersion } from '@/api/liteflow/chain'
import { diffLines, hasDiff } from '@/utils/textDiff'

export default {
  name: 'LfChainVersion',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      versionList: [],
      chainInfo: null,
      detailOpen: false,
      compareOpen: false,
      current: null,
      compareVersion: null,
      compareDiff: { left: [], right: [] },
      compareHasDiff: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        chainId: undefined,
        chainName: undefined
      }
    }
  },
  created() {
    const chainId = this.$route.query.chainId
    const chainName = this.$route.query.chainName
    if (chainId) {
      this.queryParams.chainId = Number(chainId)
      this.loadChainInfo(chainId)
    }
    if (chainName) {
      this.queryParams.chainName = chainName
    }
    this.getList()
  },
  methods: {
    loadChainInfo(chainId) {
      getChain(chainId).then(res => {
        this.chainInfo = res.data
      })
    },
    getList() {
      this.loading = true
      listChainVersions(this.queryParams).then(res => {
        this.versionList = res.rows
        this.total = res.total
        this.loading = false
        if (!this.chainInfo && this.queryParams.chainId) {
          this.loadChainInfo(this.queryParams.chainId)
        }
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.queryParams.chainId = undefined
      this.chainInfo = null
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.queryParams.chainId = undefined
      this.chainInfo = null
      this.handleQuery()
    },
    showDetail(row) {
      getChainVersion(row.id).then(res => {
        this.current = res.data
        this.detailOpen = true
      })
    },
    showCompare(row) {
      getChainVersion(row.id).then(res => {
        this.compareVersion = res.data
        if (this.chainInfo && this.chainInfo.id !== row.chainId) {
          this.loadChainInfo(row.chainId)
        } else if (!this.chainInfo) {
          this.loadChainInfo(row.chainId)
        }
        this.$nextTick(() => this.buildCompareDiff())
        this.compareOpen = true
      })
    },
    buildCompareDiff() {
      const left = (this.compareVersion && this.compareVersion.elData) || ''
      const right = (this.chainInfo && this.chainInfo.elData) || ''
      this.compareHasDiff = hasDiff(left, right)
      this.compareDiff = diffLines(left, right)
    },
    handleRollback(row) {
      this.$modal.confirm('确认回滚至 v' + row.version + '？回滚后链路变为草稿，需再次发布才生效。').then(() => {
        return rollbackChainVersion(row.id)
      }).then(() => {
        this.$modal.msgSuccess('回滚成功，请检查后发布')
        if (row.chainId) {
          this.queryParams.chainId = row.chainId
          this.loadChainInfo(row.chainId)
        }
        this.getList()
      }).catch(() => {})
    },
    goEditor() {
      if (!this.chainInfo) return
      this.$router.push({ path: '/liteflow/editor', query: { chainId: this.chainInfo.chainName, id: this.chainInfo.id } })
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
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.diff-pre {
  background: #f5f7fa;
  border-radius: 4px;
  max-height: 360px;
  overflow: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
.diff-line {
  display: flex;
  line-height: 20px;
  padding: 0 8px;
  white-space: pre-wrap;
  word-break: break-all;
}
.diff-ln {
  width: 32px;
  flex-shrink: 0;
  color: #909399;
  user-select: none;
  text-align: right;
  padding-right: 8px;
}
.diff-text { flex: 1; }
.diff-same { background: transparent; }
.diff-remove { background: #fde2e2; }
.diff-add { background: #e1f3d8; }
.diff-empty { background: #fafafa; min-height: 20px; }
</style>

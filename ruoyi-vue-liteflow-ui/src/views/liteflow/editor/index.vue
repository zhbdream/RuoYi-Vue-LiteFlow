<template>
  <div class="app-container editor-page">
    <!-- 未指定链路：选择页 -->
    <div v-if="showPicker" class="chain-picker">
      <div class="picker-header">
        <h2>可视化编排</h2>
        <p>请选择一条链路进入编排器，或到链路管理新建 / 从模板创建。</p>
      </div>
      <el-form :inline="true" size="small" class="picker-search">
        <el-form-item label="链路ID">
          <el-input v-model="pickerKeyword" placeholder="搜索 chainName" clearable @keyup.enter.native="loadChainList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadChainList">搜索</el-button>
          <el-button icon="el-icon-plus" @click="goChainList">链路管理</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="pickerLoading"
        :data="chainList"
        highlight-current-row
        @row-click="openChain"
        style="width: 100%"
      >
        <el-table-column label="链路ID" prop="chainName" min-width="140">
          <template slot-scope="scope">
            <span class="chain-name-cell">{{ scope.row.chainName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" prop="chainDesc" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="发布" prop="draftFlag" width="80" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.draftFlag === '1' ? 'warning' : 'success'" size="mini">
              {{ scope.row.draftFlag === '1' ? '草稿' : '已发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click.stop="openChain(scope.row)">编排</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="picker-tip">提示：也可在「链路管理」列表中点击「编排」直接进入。</p>
    </div>

    <lite-flow-editor
      v-else-if="loaded"
      :chain-name="chain.chainName"
      :context-class="chain.contextClass"
      :el-data="chain.elData"
      :graph-json="chain.graphJson"
      :components="components"
      :chain-options="chainOptions"
      :component-loading="componentLoading"
      :log-highlight="logHighlight"
      @back="goChainList"
      @reload="handleReload"
      @save="handleSave"
    />
    <div v-else v-loading="loading" class="editor-loading" />
  </div>
</template>

<script>
import LiteFlowEditor from '@/components/LiteFlowEditor/index.vue'
import { listChain, getChain, getChainByName, updateChain, reloadChain, listComponent } from '@/api/liteflow/chain'

export default {
  name: 'LfEditor',
  components: { LiteFlowEditor },
  data() {
    return {
      loading: false,
      loaded: false,
      showPicker: false,
      pickerLoading: false,
      pickerKeyword: '',
      chainList: [],
      componentLoading: true,
      chain: {
        id: null,
        chainName: '',
        elData: '',
        graphJson: ''
      },
      components: [],
      chainOptions: []
    }
  },
  computed: {
    logHighlight() {
      const q = this.$route.query
      if (!q.highlightSteps) {
        return null
      }
      return {
        executeStepStr: q.highlightSteps,
        failedNodeId: q.failedNode || '',
        success: q.success
      }
    }
  },
  watch: {
    '$route.query': {
      handler() {
        this.initPage()
      },
      deep: true
    }
  },
  created() {
    this.initPage()
    this.loadChainOptions()
  },
  methods: {
    initPage() {
      const chainDbId = this.$route.query.id
      const chainId = this.$route.query.chainId
      if (!chainDbId && !chainId) {
        this.loaded = false
        this.loading = false
        this.showPicker = true
        this.loadChainList()
        return
      }
      this.showPicker = false
      this.loadPage(chainDbId, chainId)
    },

    loadChainList() {
      this.pickerLoading = true
      listChain({
        pageNum: 1,
        pageSize: 200,
        chainName: this.pickerKeyword || undefined
      }).then(res => {
        this.chainList = res.rows || []
      }).finally(() => {
        this.pickerLoading = false
      })
    },

    openChain(row) {
      if (!row || !row.chainName) {
        return
      }
      this.$router.replace({
        path: '/liteflow/editor',
        query: {
          ...this.$route.query,
          chainId: row.chainName,
          id: row.id
        }
      })
    },

    loadChainOptions() {
      listChain({ pageNum: 1, pageSize: 500, status: '0' }).then(res => {
        this.chainOptions = (res.rows || []).filter(row => row.draftFlag === '0')
      })
    },

    loadPage(chainDbId, chainId) {
      this.loading = true
      this.componentLoading = true
      this.loaded = false

      const chainPromise = chainDbId
        ? getChain(chainDbId)
        : getChainByName(chainId)

      Promise.all([chainPromise, listComponent()])
        .then(([chainRes, compRes]) => {
          this.chain = chainRes.data || {}
          this.components = compRes.data || []
          if (!this.chain.chainName) {
            throw new Error('empty chain')
          }
          this.loaded = true
        })
        .catch(() => {
          this.$modal.msgError('加载链路「' + (chainId || chainDbId) + '」失败，请重新选择')
          this.$router.replace({ path: '/liteflow/editor', query: {} })
        })
        .finally(() => {
          this.loading = false
          this.componentLoading = false
        })
    },

    goChainList() {
      this.$router.push({ path: '/liteflow/chain' })
    },

    handleReload() {
      if (!this.chain.chainName) {
        return
      }
      reloadChain(this.chain.chainName).then(() => {
        this.$modal.msgSuccess('热刷新成功')
      })
    },

    handleSave(payload) {
      const data = {
        ...this.chain,
        elData: payload.elData,
        graphJson: payload.graphJson,
        draftFlag: '1'
      }
      updateChain(data).then(() => {
        this.chain.elData = payload.elData
        this.chain.graphJson = payload.graphJson
        this.chain.draftFlag = '1'
        this.$modal.msgSuccess('已保存为草稿，请在链路管理中发布后生效')
      })
    }
  }
}
</script>

<style scoped>
.editor-page {
  padding-bottom: 0;
}

.editor-loading {
  min-height: 400px;
}

.chain-picker {
  max-width: 960px;
  margin: 0 auto;
}

.picker-header h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #303133;
}

.picker-header p {
  margin: 0 0 20px;
  color: #909399;
  font-size: 14px;
}

.picker-search {
  margin-bottom: 12px;
}

.chain-name-cell {
  font-family: Consolas, monospace;
  font-weight: 600;
  color: #409eff;
  cursor: pointer;
}

.picker-tip {
  margin-top: 16px;
  font-size: 13px;
  color: #909399;
}
</style>

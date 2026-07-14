<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="节点ID" prop="nodeId">
        <el-input v-model="queryParams.nodeId" placeholder="组件 nodeId" clearable />
      </el-form-item>
      <el-form-item label="类型" prop="nodeType">
        <el-select v-model="queryParams.nodeType" placeholder="组件类型" clearable>
          <el-option label="普通 common" value="common" />
          <el-option label="布尔 boolean" value="boolean" />
          <el-option label="选择 switch" value="switch" />
          <el-option label="循环 for" value="for" />
          <el-option label="Agent agent" value="agent" />
          <el-option label="声明式 declarative" value="declarative" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-refresh" size="mini" @click="getList">刷新扫描</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-document-add" size="mini" @click="openScaffold">生成脚手架</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="filteredList">
      <el-table-column label="节点ID" prop="nodeId" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="名称" prop="name" min-width="120" />
      <el-table-column label="类型" prop="nodeType" width="110" align="center">
        <template slot-scope="scope">
          <el-tag size="mini">{{ scope.row.nodeType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="引用数" prop="refCount" width="80" align="center" />
      <el-table-column label="Java 类" prop="className" min-width="220" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="showDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="组件详情" :visible.sync="detailOpen" width="640px" append-to-body>
      <el-descriptions v-if="current" :column="1" border size="small">
        <el-descriptions-item label="节点ID">{{ current.nodeId }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.nodeType }}</el-descriptions-item>
        <el-descriptions-item label="Java 类">{{ current.className }}</el-descriptions-item>
        <el-descriptions-item label="引用链路">
          <span v-if="!current.refChains || !current.refChains.length">暂无引用</span>
          <el-tag v-for="c in current.refChains" :key="c" size="mini" style="margin:2px">{{ c }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div slot="footer" class="dialog-footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="生成组件脚手架" :visible.sync="scaffoldOpen" width="820px" append-to-body>
      <el-form ref="scaffoldForm" :model="scaffoldForm" :rules="scaffoldRules" label-width="100px" size="small">
        <el-form-item label="nodeId" prop="nodeId">
          <el-input v-model="scaffoldForm.nodeId" placeholder="如 myValidate" />
        </el-form-item>
        <el-form-item label="组件类型" prop="nodeType">
          <el-select v-model="scaffoldForm.nodeType" style="width:100%">
            <el-option label="普通 common" value="common" />
            <el-option label="布尔 boolean" value="boolean" />
            <el-option label="选择 switch" value="switch" />
            <el-option label="循环 for" value="for" />
          </el-select>
        </el-form-item>
        <el-form-item label="风格" prop="style">
          <el-radio-group v-model="scaffoldForm.style">
            <el-radio label="inherited">继承式</el-radio>
            <el-radio label="declarative">声明式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="包名" prop="packageName">
          <el-input v-model="scaffoldForm.packageName" placeholder="com.ruoyiliteflow.liteflow.component" />
        </el-form-item>
      </el-form>
      <el-input
        v-if="scaffoldSource"
        type="textarea"
        :rows="16"
        :value="scaffoldSource"
        readonly
        class="scaffold-source"
      />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="generateScaffold">生 成</el-button>
        <el-button v-if="scaffoldSource" @click="copyScaffold">复制源码</el-button>
        <el-button @click="scaffoldOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listComponentCenter, generateComponentScaffold } from '@/api/liteflow/platform'

export default {
  name: 'LfComponentCenter',
  data() {
    return {
      loading: false,
      showSearch: true,
      componentList: [],
      detailOpen: false,
      current: null,
      queryParams: {
        nodeId: undefined,
        nodeType: undefined
      },
      scaffoldOpen: false,
      scaffoldSource: '',
      scaffoldFileName: '',
      scaffoldForm: {
        nodeId: '',
        nodeType: 'common',
        style: 'inherited',
        packageName: 'com.ruoyiliteflow.liteflow.component'
      },
      scaffoldRules: {
        nodeId: [{ required: true, message: 'nodeId 不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    filteredList() {
      return this.componentList.filter(item => {
        const idOk = !this.queryParams.nodeId || (item.nodeId && item.nodeId.includes(this.queryParams.nodeId))
        const typeOk = !this.queryParams.nodeType || item.nodeType === this.queryParams.nodeType
        return idOk && typeOk
      })
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listComponentCenter().then(res => {
        this.componentList = res.data || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    handleQuery() {
      // 前端过滤
    },
    resetQuery() {
      this.queryParams = { nodeId: undefined, nodeType: undefined }
    },
    showDetail(row) {
      this.current = row
      this.detailOpen = true
    },
    openScaffold() {
      this.scaffoldSource = ''
      this.scaffoldFileName = ''
      this.scaffoldOpen = true
    },
    generateScaffold() {
      this.$refs.scaffoldForm.validate(valid => {
        if (!valid) return
        generateComponentScaffold(this.scaffoldForm).then(res => {
          const data = res.data || {}
          this.scaffoldSource = data.source || ''
          this.scaffoldFileName = data.fileName || 'Component.java'
          this.$modal.msgSuccess('已生成 ' + this.scaffoldFileName)
        })
      })
    },
    copyScaffold() {
      if (!this.scaffoldSource) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(this.scaffoldSource).then(() => {
          this.$modal.msgSuccess('已复制到剪贴板')
        }).catch(() => {
          this.fallbackCopy(this.scaffoldSource)
        })
      } else {
        this.fallbackCopy(this.scaffoldSource)
      }
    },
    fallbackCopy(text) {
      const el = document.createElement('textarea')
      el.value = text
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
      this.$modal.msgSuccess('已复制到剪贴板')
    }
  }
}
</script>

<style scoped>
.scaffold-source ::v-deep textarea {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  margin-top: 8px;
}
</style>

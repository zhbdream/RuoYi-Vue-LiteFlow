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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="filteredList">
      <el-table-column label="节点ID" prop="nodeId" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="名称" prop="name" min-width="120" />
      <el-table-column label="类型" prop="nodeType" width="100" align="center">
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
  </div>
</template>

<script>
import { listComponentCenter } from '@/api/liteflow/platform'

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
      // 前端过滤，无需请求
    },
    resetQuery() {
      this.queryParams = { nodeId: undefined, nodeType: undefined }
    },
    showDetail(row) {
      this.current = row
      this.detailOpen = true
    }
  }
}
</script>

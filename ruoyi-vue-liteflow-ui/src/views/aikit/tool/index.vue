<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="toolCode">
        <el-input v-model="queryParams.toolCode" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="toolType">
        <el-select v-model="queryParams.toolType" clearable placeholder="全部">
          <el-option label="local" value="local" />
          <el-option label="mcp" value="mcp" />
        </el-select>
      </el-form-item>
      <el-form-item label="启用" prop="enabled">
        <el-select v-model="queryParams.enabled" clearable placeholder="全部">
          <el-option label="启用" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:tool:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aikit:tool:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" prop="toolCode" min-width="140" />
      <el-table-column label="名称" prop="toolName" min-width="120" />
      <el-table-column label="类型" prop="toolType" width="90" />
      <el-table-column label="调用键" prop="invokeKey" min-width="140" />
      <el-table-column label="MCP Server" prop="mcpServerKey" width="120" />
      <el-table-column label="启用" prop="enabled" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled === '1' ? 'success' : 'info'" size="mini">{{ scope.row.enabled === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aikit:tool:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aikit:tool:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="工具编码" prop="toolCode">
          <el-input v-model="form.toolCode" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="名称" prop="toolName">
          <el-input v-model="form.toolName" />
        </el-form-item>
        <el-form-item label="类型" prop="toolType">
          <el-select v-model="form.toolType" style="width:100%">
            <el-option label="local" value="local" />
            <el-option label="mcp" value="mcp" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="调用键" prop="invokeKey">
          <el-input v-model="form.invokeKey" placeholder="local=bean键；mcp=tool名" />
        </el-form-item>
        <el-form-item label="MCP Server">
          <el-input v-model="form.mcpServerKey" placeholder="如 ai-core" />
        </el-form-item>
        <el-form-item label="启用">
          <el-radio-group v-model="form.enabled">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAiTool, getAiTool, addAiTool, updateAiTool, delAiTool } from '@/api/aikit/platform'

export default {
  name: 'Tool',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      ids: [],
      multiple: true,
      open: false,
      title: '',
      queryParams: { pageNum: 1, pageSize: 10, toolCode: undefined, toolType: undefined, enabled: undefined },
      form: {},
      rules: {
        toolCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
        toolType: [{ required: true, message: '类型不能为空', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAiTool(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(i => i.id)
      this.multiple = !selection.length
    },
    reset() {
      this.form = {
        id: undefined, toolCode: undefined, toolName: undefined, toolType: 'mcp',
        description: undefined, invokeKey: undefined, mcpServerKey: 'ai-core', enabled: '1', remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增工具'
    },
    handleUpdate(row) {
      this.reset()
      getAiTool(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改工具'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id != null ? updateAiTool(this.form) : addAiTool(this.form)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除？').then(() => delAiTool(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

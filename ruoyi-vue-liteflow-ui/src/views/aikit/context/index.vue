<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="policyCode">
        <el-input v-model="queryParams.policyCode" clearable @keyup.enter.native="handleQuery" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:context:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aikit:context:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" prop="policyCode" min-width="120" />
      <el-table-column label="名称" prop="policyName" min-width="120" />
      <el-table-column label="窗口" prop="windowSize" width="80" />
      <el-table-column label="摘要" prop="enableSummary" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enableSummary === '1' ? 'success' : 'info'" size="mini">{{ scope.row.enableSummary === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" prop="isDefault" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isDefault === '1' ? 'warning' : 'info'" size="mini">{{ scope.row.isDefault === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="变量模板" prop="variableTemplate" min-width="160" show-overflow-tooltip />
      <el-table-column label="启用" prop="enabled" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled === '1' ? 'success' : 'info'" size="mini">{{ scope.row.enabled === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aikit:context:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aikit:context:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="策略编码" prop="policyCode">
          <el-input v-model="form.policyCode" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="名称" prop="policyName">
          <el-input v-model="form.policyName" />
        </el-form-item>
        <el-form-item label="记忆窗口">
          <el-input-number v-model="form.windowSize" :min="1" :max="100" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="超窗摘要">
          <el-radio-group v-model="form.enableSummary">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="变量模板">
          <el-input v-model="form.variableTemplate" :placeholder="'如 调用方={{principal}}'" />
        </el-form-item>
        <el-form-item label="默认策略">
          <el-radio-group v-model="form.isDefault">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
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
import { listAiContext, getAiContext, addAiContext, updateAiContext, delAiContext } from '@/api/aikit/platform'

export default {
  name: 'Context',
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
      queryParams: { pageNum: 1, pageSize: 10, policyCode: undefined, enabled: undefined },
      form: {},
      rules: {
        policyCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAiContext(this.queryParams).then(res => {
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
        id: undefined, policyCode: undefined, policyName: undefined, windowSize: 8,
        enableSummary: '1', variableTemplate: '调用方={{principal}}', isDefault: '0', enabled: '1', remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增上下文策略'
    },
    handleUpdate(row) {
      this.reset()
      getAiContext(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改上下文策略'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id != null ? updateAiContext(this.form) : addAiContext(this.form)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除？').then(() => delAiContext(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

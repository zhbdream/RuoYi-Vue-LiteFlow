<template>
  <div class="app-container">
    <el-alert title="会话记忆由 AgentRuntime 按上下文策略自动写入；超窗后会用当前模型生成摘要。可按 Agent+会话清理，或按天数清理过期。" type="info" :closable="false" show-icon class="mb8" />
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="Agent" prop="agentCode">
        <el-input v-model="queryParams.agentCode" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="会话" prop="sessionId">
        <el-input v-model="queryParams.sessionId" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="memoryType">
        <el-select v-model="queryParams.memoryType" clearable placeholder="全部">
          <el-option label="turn" value="turn" />
          <el-option label="summary" value="summary" />
          <el-option label="fact" value="fact" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:memory:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aikit:memory:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-delete" size="mini" @click="handleClearSession" v-hasPermi="['aikit:memory:remove']">按会话清理</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button size="mini" @click="handlePurge" v-hasPermi="['aikit:memory:remove']">清理过期</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="Agent" prop="agentCode" width="110" />
      <el-table-column label="会话" prop="sessionId" width="120" />
      <el-table-column label="主体" prop="principal" width="100" />
      <el-table-column label="类型" prop="memoryType" width="90" />
      <el-table-column label="角色" prop="role" width="90" />
      <el-table-column label="内容" prop="content" min-width="220" show-overflow-tooltip />
      <el-table-column label="时间" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="100">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aikit:memory:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新增记忆" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="Agent" prop="agentCode">
          <el-input v-model="form.agentCode" placeholder="如 chat" />
        </el-form-item>
        <el-form-item label="会话ID">
          <el-input v-model="form.sessionId" placeholder="default" />
        </el-form-item>
        <el-form-item label="主体">
          <el-input v-model="form.principal" placeholder="anonymous" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.memoryType" style="width:100%">
            <el-option label="turn" value="turn" />
            <el-option label="summary" value="summary" />
            <el-option label="fact" value="fact" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="user" value="user" />
            <el-option label="assistant" value="assistant" />
            <el-option label="system" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" />
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
import { listAiMemory, addAiMemory, delAiMemory, clearAiMemory, purgeAiMemory } from '@/api/aikit/platform'

export default {
  name: 'Memory',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      ids: [],
      multiple: true,
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, agentCode: undefined, sessionId: undefined, memoryType: undefined },
      form: {},
      rules: {
        agentCode: [{ required: true, message: 'Agent 不能为空', trigger: 'blur' }],
        content: [{ required: true, message: '内容不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAiMemory(this.queryParams).then(res => {
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
        agentCode: 'chat', sessionId: 'default', principal: 'admin',
        memoryType: 'fact', role: 'system', content: ''
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        addAiMemory(this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除？').then(() => delAiMemory(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleClearSession() {
      const agentCode = this.queryParams.agentCode
      const sessionId = this.queryParams.sessionId
      if (!agentCode || !sessionId) {
        this.$modal.msgWarning('请先在筛选里填写 Agent 和会话')
        return
      }
      this.$modal.confirm('确认清理 ' + agentCode + ' / ' + sessionId + ' 的全部记忆？').then(() => {
        return clearAiMemory({ agentCode, sessionId })
      }).then(res => {
        this.$modal.msgSuccess(res.msg || '已清理')
        this.getList()
      }).catch(() => {})
    },
    handlePurge() {
      this.$prompt('清理多少天前的记忆？', '清理过期', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: '30',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '请输入正整数'
      }).then(({ value }) => purgeAiMemory(parseInt(value, 10))).then(res => {
        this.$modal.msgSuccess(res.msg || '已清理')
        this.getList()
      }).catch(() => {})
    }
  }
}
</script>

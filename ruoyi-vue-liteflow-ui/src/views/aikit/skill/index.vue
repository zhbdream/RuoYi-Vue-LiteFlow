<template>
  <div class="app-container">
    <el-alert :title="'prompt / http 均支持 {{principal}} {{agentCode}} {{sessionId}} {{message}}。HTTP 首行可写 GET/POST URL，其余为 POST body。'" type="info" :closable="false" show-icon class="mb8" />
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="skillCode">
        <el-input v-model="queryParams.skillCode" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="skillType">
        <el-select v-model="queryParams.skillType" clearable placeholder="全部">
          <el-option label="prompt" value="prompt" />
          <el-option label="http" value="http" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:skill:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aikit:skill:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" prop="skillCode" min-width="140" />
      <el-table-column label="名称" prop="skillName" min-width="120" />
      <el-table-column label="类型" prop="skillType" width="90" />
      <el-table-column label="描述" prop="description" min-width="160" show-overflow-tooltip />
      <el-table-column label="启用" prop="enabled" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled === '1' ? 'success' : 'info'" size="mini">{{ scope.row.enabled === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-video-play" @click="openRun(scope.row)" v-hasPermi="['aikit:skill:query']">试跑</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aikit:skill:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aikit:skill:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="技能编码" prop="skillCode">
          <el-input v-model="form.skillCode" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="名称" prop="skillName">
          <el-input v-model="form.skillName" />
        </el-form-item>
        <el-form-item label="类型" prop="skillType">
          <el-select v-model="form.skillType" style="width:100%">
            <el-option label="prompt" value="prompt" />
            <el-option label="http" value="http" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.skillType === 'http'" label="方法">
          <el-radio-group v-model="form.httpMethod">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.skillType === 'http'" label="URL" prop="httpUrl">
          <el-input v-model="form.httpUrl" :placeholder="'https://example.com/api?q={{message}}'" />
        </el-form-item>
        <el-form-item v-if="form.skillType === 'http' && form.httpMethod === 'POST'" label="Body">
          <el-input v-model="form.httpBody" type="textarea" :rows="4" :placeholder="'{\&quot;q\&quot;:\&quot;{{message}}\&quot;}'" />
        </el-form-item>
        <el-form-item v-if="form.skillType !== 'http'" label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" :placeholder="'提示词，可用 {{principal}} {{message}}'" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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

    <el-dialog title="试跑技能" :visible.sync="runOpen" width="640px" append-to-body>
      <el-form size="small" label-width="90px">
        <el-form-item label="技能">{{ runForm.skillCode }}</el-form-item>
        <el-form-item label="message">
          <el-input v-model="runForm.message" type="textarea" :rows="2" :placeholder="'会替换 {{message}}'" />
        </el-form-item>
        <el-form-item label="principal">
          <el-input v-model="runForm.principal" />
        </el-form-item>
        <el-form-item label="sessionId">
          <el-input v-model="runForm.sessionId" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="runLoading" @click="doRun">试 跑</el-button>
        </el-form-item>
        <el-form-item label="渲染结果">
          <el-input type="textarea" :rows="3" :value="runRendered" readonly />
        </el-form-item>
        <el-form-item label="输出">
          <el-input type="textarea" :rows="6" :value="runResult" readonly />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import { listAiSkill, getAiSkill, addAiSkill, updateAiSkill, delAiSkill, runAiSkill } from '@/api/aikit/platform'

export default {
  name: 'Skill',
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
      runOpen: false,
      runLoading: false,
      runRendered: '',
      runResult: '',
      runForm: { skillCode: '', message: '七天无理由怎么退？', principal: 'admin', sessionId: 'default' },
      queryParams: { pageNum: 1, pageSize: 10, skillCode: undefined, skillType: undefined, enabled: undefined },
      form: {},
      rules: {
        skillCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
        skillType: [{ required: true, message: '类型不能为空', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAiSkill(this.queryParams).then(res => {
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
        id: undefined, skillCode: undefined, skillName: undefined, skillType: 'prompt',
        content: '', httpMethod: 'GET', httpUrl: '', httpBody: '',
        description: undefined, enabled: '1', remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增技能'
    },
    handleUpdate(row) {
      this.reset()
      getAiSkill(row.id).then(res => {
        this.form = Object.assign({ httpMethod: 'GET', httpUrl: '', httpBody: '' }, res.data)
        if (this.form.skillType === 'http') {
          const parsed = this.parseHttpContent(this.form.content)
          this.form.httpMethod = parsed.httpMethod
          this.form.httpUrl = parsed.httpUrl
          this.form.httpBody = parsed.httpBody
        }
        this.open = true
        this.title = '修改技能'
      })
    },
    parseHttpContent(content) {
      const t = (content || '').trim()
      const lines = t.split(/\r?\n/)
      const first = lines[0] || ''
      if (/^POST\s+/i.test(first)) {
        return { httpMethod: 'POST', httpUrl: first.replace(/^POST\s+/i, '').trim(), httpBody: lines.slice(1).join('\n') }
      }
      if (/^GET\s+/i.test(first)) {
        return { httpMethod: 'GET', httpUrl: first.replace(/^GET\s+/i, '').trim(), httpBody: lines.slice(1).join('\n') }
      }
      return { httpMethod: 'GET', httpUrl: t, httpBody: '' }
    },
    composeContent() {
      if (this.form.skillType !== 'http') return this.form.content
      const url = this.form.httpUrl || ''
      const body = this.form.httpBody || ''
      if (this.form.httpMethod === 'POST') {
        return 'POST ' + url + (body ? '\n' + body : '')
      }
      return url
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = Object.assign({}, this.form, { content: this.composeContent() })
        const req = payload.id != null ? updateAiSkill(payload) : addAiSkill(payload)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    openRun(row) {
      this.runForm = { skillCode: row.skillCode, message: '七天无理由怎么退？', principal: 'admin', sessionId: 'default' }
      this.runRendered = ''
      this.runResult = ''
      this.runOpen = true
    },
    doRun() {
      this.runLoading = true
      runAiSkill(this.runForm.skillCode, {
        message: this.runForm.message,
        principal: this.runForm.principal,
        sessionId: this.runForm.sessionId
      }).then(res => {
        const d = res.data || {}
        this.runRendered = d.rendered || ''
        this.runResult = d.result || ''
        this.runLoading = false
      }).catch(() => { this.runLoading = false })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除？').then(() => delAiSkill(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

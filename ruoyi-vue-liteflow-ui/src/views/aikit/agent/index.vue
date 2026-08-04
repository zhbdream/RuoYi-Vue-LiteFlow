<template>
  <div class="app-container">
    <el-alert title="修改 system_prompt 后，配置驱动调用会立即生效，无需改代码。" type="info" :closable="false" show-icon class="mb8" />
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="agentCode">
        <el-input v-model="queryParams.agentCode" clearable @keyup.enter.native="handleQuery" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:agent:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aikit:agent:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" prop="agentCode" width="120" />
      <el-table-column label="名称" prop="agentName" min-width="120" />
      <el-table-column label="模型ID" prop="modelId" width="90" />
      <el-table-column label="温度" prop="temperature" width="80" />
      <el-table-column label="工具" min-width="120">
        <template slot-scope="scope">
          <span>{{ (scope.row.toolCodes || []).join(', ') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="知识库" min-width="120">
        <template slot-scope="scope">
          <span>{{ (scope.row.knowledgeCodes || []).join(', ') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="技能" min-width="120">
        <template slot-scope="scope">
          <span>{{ (scope.row.skillCodes || []).join(', ') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="上下文" prop="contextPolicyId" width="90" />
      <el-table-column label="启用" prop="enabled" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled === '1' ? 'success' : 'info'" size="mini">{{ scope.row.enabled === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aikit:agent:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-video-play" @click="openRun(scope.row)" v-hasPermi="['aikit:agent:run']">试跑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aikit:agent:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="720px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="编码" prop="agentCode">
          <el-input v-model="form.agentCode" :disabled="form.id != null" placeholder="如 chat / risk" />
        </el-form-item>
        <el-form-item label="名称" prop="agentName">
          <el-input v-model="form.agentName" />
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="绑定模型ID">
          <el-input-number v-model="form.modelId" :min="0" controls-position="right" style="width:100%" placeholder="空=默认模型" />
        </el-form-item>
        <el-form-item label="温度">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="绑定工具">
          <el-select v-model="form.toolIds" multiple filterable clearable style="width:100%" placeholder="选择工具">
            <el-option v-for="t in toolOptions" :key="t.id" :label="t.toolName + ' (' + t.toolCode + ')'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定知识库">
          <el-select v-model="form.kbIds" multiple filterable clearable style="width:100%" placeholder="选择知识库">
            <el-option v-for="k in kbOptions" :key="k.id" :label="k.kbName + ' (' + k.kbCode + ')'" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定技能">
          <el-select v-model="form.skillIds" multiple filterable clearable style="width:100%" placeholder="选择技能">
            <el-option v-for="s in skillOptions" :key="s.id" :label="s.skillName + ' (' + s.skillCode + ')'" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="上下文策略">
          <el-select v-model="form.contextPolicyId" clearable filterable style="width:100%" placeholder="默认策略">
            <el-option v-for="p in contextOptions" :key="p.id" :label="p.policyName + ' (' + p.policyCode + ')'" :value="p.id" />
          </el-select>
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

    <el-dialog title="试跑智能体" :visible.sync="runOpen" width="640px" append-to-body>
      <el-form label-width="80px" size="small">
        <el-form-item label="Agent">
          <el-input :value="runForm.agentCode" disabled />
        </el-form-item>
        <el-form-item label="输入">
          <el-input v-model="runForm.message" type="textarea" :rows="4" placeholder="输入试跑内容" />
        </el-form-item>
        <el-form-item label="会话ID">
          <el-input v-model="runForm.sessionId" placeholder="default" />
        </el-form-item>
        <el-form-item label="结果" v-if="runResult">
          <el-input :value="runResult" type="textarea" :rows="6" readonly />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="runLoading" @click="doRun">运 行</el-button>
        <el-button @click="runOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAiAgent, getAiAgent, addAiAgent, updateAiAgent, delAiAgent, runAiAgent, listAiTool, listAiKnowledge, listAiSkill, listAiContext } from '@/api/aikit/platform'

export default {
  name: 'Agent',
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
      toolOptions: [],
      kbOptions: [],
      skillOptions: [],
      contextOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, agentCode: undefined, enabled: undefined },
      form: {},
      rules: {
        agentCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }]
      },
      runOpen: false,
      runLoading: false,
      runResult: '',
      runForm: { agentCode: '', message: '', sessionId: 'default' }
    }
  },
  created() {
    this.getList()
    this.loadTools()
    this.loadKbs()
    this.loadSkills()
    this.loadContexts()
  },
  methods: {
    loadTools() {
      listAiTool({ pageNum: 1, pageSize: 200 }).then(res => {
        this.toolOptions = res.rows || []
      }).catch(() => {})
    },
    loadKbs() {
      listAiKnowledge({ pageNum: 1, pageSize: 200 }).then(res => {
        this.kbOptions = res.rows || []
      }).catch(() => {})
    },
    loadSkills() {
      listAiSkill({ pageNum: 1, pageSize: 200, enabled: '1' }).then(res => {
        this.skillOptions = res.rows || []
      }).catch(() => {})
    },
    loadContexts() {
      listAiContext({ pageNum: 1, pageSize: 200, enabled: '1' }).then(res => {
        this.contextOptions = res.rows || []
      }).catch(() => {})
    },
    getList() {
      this.loading = true
      listAiAgent(this.queryParams).then(res => {
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
        id: undefined, agentCode: undefined, agentName: undefined, systemPrompt: '',
        modelId: undefined, temperature: 0.3, contextPolicyId: undefined, enabled: '1',
        toolIds: [], kbIds: [], skillIds: [], remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增智能体'
    },
    handleUpdate(row) {
      this.reset()
      getAiAgent(row.id).then(res => {
        this.form = Object.assign({ toolIds: [], kbIds: [], skillIds: [] }, res.data)
        if (!this.form.toolIds) this.form.toolIds = []
        if (!this.form.kbIds) this.form.kbIds = []
        if (!this.form.skillIds) this.form.skillIds = []
        this.open = true
        this.title = '修改智能体'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const payload = Object.assign({}, this.form)
        if (payload.modelId === 0) payload.modelId = null
        const req = payload.id != null ? updateAiAgent(payload) : addAiAgent(payload)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除？').then(() => delAiAgent(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    openRun(row) {
      this.runForm = { agentCode: row.agentCode, message: '用一句话介绍你自己', sessionId: 'default' }
      this.runResult = ''
      this.runOpen = true
    },
    doRun() {
      if (!this.runForm.message) {
        this.$modal.msgWarning('请输入内容')
        return
      }
      this.runLoading = true
      runAiAgent(this.runForm.agentCode, {
        message: this.runForm.message,
        principal: 'admin',
        sessionId: this.runForm.sessionId || 'default'
      }).then(res => {
        const data = res.data || {}
        this.runResult = data.content || JSON.stringify(data, null, 2)
        this.runLoading = false
      }).catch(() => { this.runLoading = false })
    }
  }
}
</script>

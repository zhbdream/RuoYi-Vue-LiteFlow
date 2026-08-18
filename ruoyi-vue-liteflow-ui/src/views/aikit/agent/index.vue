<template>
  <div class="app-container">
    <el-alert title="改提示词立即生效。点「去助手聊」在 AI 助手中使用该智能体。" type="info" :closable="false" show-icon class="mb8" />
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

    <div v-loading="loading" class="agent-grid">
      <div
        v-for="row in list"
        :key="row.id"
        class="agent-card"
        :class="{ selected: ids.indexOf(row.id) !== -1 }"
      >
        <div class="agent-card__head">
          <el-checkbox
            class="agent-card__check"
            :value="ids.indexOf(row.id) !== -1"
            @change="val => toggleSelect(row, val)"
          />
          <div class="agent-card__title">
            <div class="agent-card__name">{{ row.agentName || row.agentCode }}</div>
            <div class="agent-card__code">{{ row.agentCode }}</div>
          </div>
          <el-tag size="mini" :type="row.enabled === '1' ? 'success' : 'info'">{{ row.enabled === '1' ? '启用' : '停用' }}</el-tag>
        </div>
        <div class="agent-card__binds">
          <el-tag size="mini" effect="plain">{{ modelLabel(row) }}</el-tag>
          <el-tag v-for="t in (row.toolCodes || []).slice(0, 3)" :key="'t-' + t" size="mini" type="warning" effect="plain">{{ t }}</el-tag>
          <el-tag v-for="k in (row.knowledgeCodes || []).slice(0, 2)" :key="'k-' + k" size="mini" type="success" effect="plain">{{ k }}</el-tag>
          <el-tag v-for="s in (row.skillCodes || []).slice(0, 2)" :key="'s-' + s" size="mini" effect="plain">{{ s }}</el-tag>
          <span v-if="bindOverflow(row)" class="agent-card__more">+{{ bindOverflow(row) }}</span>
        </div>
        <div class="agent-card__actions">
          <el-button type="primary" size="mini" icon="el-icon-chat-dot-round" @click="goChat(row)" v-hasPermi="['liteflow:chat:send']">去助手聊</el-button>
          <el-button size="mini" @click="handleUpdate(row)" v-hasPermi="['aikit:agent:edit']">编辑</el-button>
          <el-button size="mini" @click="openRun(row)" v-hasPermi="['aikit:agent:run']">试跑</el-button>
          <el-button size="mini" type="text" @click="openLogs(row)" v-hasPermi="['aikit:agent:query']">记录</el-button>
          <el-button size="mini" type="text" @click="handleDelete(row)" v-hasPermi="['aikit:agent:remove']">删除</el-button>
        </div>
      </div>
      <div v-if="!loading && !list.length" class="agent-empty">暂无智能体</div>
    </div>
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
        <el-form-item label="绑定模型">
          <el-select v-model="form.modelId" clearable filterable placeholder="默认模型" style="width:100%">
            <el-option
              v-for="m in modelOptions"
              :key="m.id"
              :label="(m.modelName || m.modelCode) + (m.isDefault === '1' ? '（默认）' : '')"
              :value="m.id"
            />
          </el-select>
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

    <el-dialog title="试跑智能体" :visible.sync="runOpen" width="760px" append-to-body class="aikit-run-dialog">
      <div class="run-meta">
        <span>Agent：{{ runForm.agentCode }}</span>
        <el-input v-model="runForm.sessionId" size="mini" style="width:220px;margin-left:12px" placeholder="sessionId" />
        <el-button size="mini" @click="newSession">新会话</el-button>
        <el-button size="mini" @click="loadHistory">加载历史</el-button>
      </div>
      <div class="run-chat" ref="runChat">
        <div v-for="(m, i) in runMessages" :key="i" :class="['run-bubble', m.role]">
          <div class="run-role">{{ m.role === 'user' ? '我' : 'Agent' }}</div>
          <div class="run-text">{{ m.content }}</div>
          <div v-if="m.tools && m.tools.length" class="run-tools">
            <el-tag v-for="(t, ti) in m.tools" :key="ti" size="mini" type="warning">{{ t.tool || t.skill }} {{ t.costMs != null ? t.costMs + 'ms' : '' }}</el-tag>
          </div>
        </div>
        <div v-if="runLoading && !runMessages.some(m => m.streaming)" class="run-bubble assistant">
          <div class="run-role">Agent</div>
          <div class="run-text">思考中…</div>
        </div>
      </div>
      <el-input v-model="runForm.message" type="textarea" :rows="3" placeholder="输入后回车发送，可多轮连续提问" @keydown.ctrl.enter.native="doRun" />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="runLoading" @click="doRun">发 送</el-button>
        <el-button v-if="runLoading" @click="stopRun">停 止</el-button>
        <el-button @click="runOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="'调用记录 · ' + logAgentCode" :visible.sync="logOpen" width="920px" append-to-body>
      <el-form size="small" :inline="true">
        <el-form-item label="会话">
          <el-input v-model="logQuery.sessionId" clearable style="width:180px" placeholder="全部" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="mini" @click="loadLogs">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="logLoading" :data="logList" size="small" max-height="420">
        <el-table-column label="时间" prop="createTime" width="160" />
        <el-table-column label="会话" prop="sessionId" width="140" show-overflow-tooltip />
        <el-table-column label="模型" prop="model" width="120" show-overflow-tooltip />
        <el-table-column label="耗时" prop="costMs" width="80" />
        <el-table-column label="KB" width="60" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.kbHit === '1' ? 'success' : 'info'">{{ scope.row.kbHit === '1' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工具" width="60" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.toolHit === '1' ? 'warning' : 'info'">{{ scope.row.toolHit === '1' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="输入" prop="userMessage" min-width="140" show-overflow-tooltip />
        <el-table-column label="轨迹" prop="toolTrace" min-width="140" show-overflow-tooltip />
        <el-table-column label="错误" prop="errorMsg" min-width="120" show-overflow-tooltip />
      </el-table>
      <pagination v-show="logTotal > 0" :total="logTotal" :page.sync="logQuery.pageNum" :limit.sync="logQuery.pageSize" @pagination="loadLogs" />
    </el-dialog>
  </div>
</template>

<script>
import { listAiAgent, getAiAgent, addAiAgent, updateAiAgent, delAiAgent, streamAiAgent, listAiAgentLogs, listAiTool, listAiKnowledge, listAiSkill, listAiContext, listAiMemory, listAiModel } from '@/api/aikit/platform'

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
      modelOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, agentCode: undefined, enabled: undefined },
      form: {},
      rules: {
        agentCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }]
      },
      runOpen: false,
      runLoading: false,
      runMessages: [],
      runStream: null,
      runForm: { agentCode: '', message: '', sessionId: 'default' },
      logOpen: false,
      logLoading: false,
      logAgentCode: '',
      logList: [],
      logTotal: 0,
      logQuery: { pageNum: 1, pageSize: 10, sessionId: undefined }
    }
  },
  created() {
    this.getList()
    this.loadTools()
    this.loadKbs()
    this.loadSkills()
    this.loadContexts()
    this.loadModels()
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
    loadModels() {
      listAiModel({ pageNum: 1, pageSize: 200, status: '0' }).then(res => {
        this.modelOptions = res.rows || []
      }).catch(() => {})
    },
    modelLabel(row) {
      if (!row || !row.modelId) return '默认模型'
      const m = this.modelOptions.find(x => x.id === row.modelId)
      return m ? (m.modelName || m.modelCode) : ('模型 #' + row.modelId)
    },
    bindOverflow(row) {
      const extra = Math.max(0, (row.toolCodes || []).length - 3)
        + Math.max(0, (row.knowledgeCodes || []).length - 2)
        + Math.max(0, (row.skillCodes || []).length - 2)
      return extra
    },
    toggleSelect(row, checked) {
      const on = checked === true || checked === false ? checked : this.ids.indexOf(row.id) === -1
      if (on) {
        if (this.ids.indexOf(row.id) === -1) {
          this.ids = this.ids.concat(row.id)
        }
      } else {
        this.ids = this.ids.filter(id => id !== row.id)
      }
      this.multiple = !this.ids.length
    },
    goChat(row) {
      this.$router.push({
        path: '/aikit/chat',
        query: { agent: row.agentCode, t: String(Date.now()) }
      })
    },
    getList() {
      this.loading = true
      listAiAgent(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.ids = []
        this.multiple = true
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
    openLogs(row) {
      this.logAgentCode = row.agentCode
      this.logQuery = { pageNum: 1, pageSize: 10, sessionId: undefined }
      this.logOpen = true
      this.loadLogs()
    },
    loadLogs() {
      this.logLoading = true
      listAiAgentLogs(this.logAgentCode, this.logQuery).then(res => {
        this.logList = res.rows || []
        this.logTotal = res.total || 0
        this.logLoading = false
      }).catch(() => { this.logLoading = false })
    },
    openRun(row) {
      this.runForm = { agentCode: row.agentCode, message: '', sessionId: 'try-' + Date.now() }
      this.runMessages = []
      this.runOpen = true
    },
    newSession() {
      this.runForm.sessionId = 'try-' + Date.now()
      this.runMessages = []
    },
    loadHistory() {
      listAiMemory({ pageNum: 1, pageSize: 20, agentCode: this.runForm.agentCode, sessionId: this.runForm.sessionId }).then(res => {
        const rows = (res.rows || []).slice().reverse()
        this.runMessages = rows.filter(r => r.role === 'user' || r.role === 'assistant').map(r => ({
          role: r.role, content: r.content, tools: []
        }))
        this.scrollRun()
      }).catch(() => {})
    },
    stopRun() {
      if (this.runStream && this.runStream.abort) this.runStream.abort()
      this.runLoading = false
    },
    scrollRun() {
      this.$nextTick(() => {
        const el = this.$refs.runChat
        if (el) el.scrollTop = el.scrollHeight
      })
    },
    doRun() {
      if (!this.runForm.message) {
        this.$modal.msgWarning('请输入内容')
        return
      }
      const text = this.runForm.message
      this.runMessages.push({ role: 'user', content: text })
      const assistant = { role: 'assistant', content: '', tools: [], streaming: true }
      this.runMessages.push(assistant)
      this.runForm.message = ''
      this.runLoading = true
      this.scrollRun()
      this.runStream = streamAiAgent(this.runForm.agentCode, {
        message: text,
        principal: 'admin',
        sessionId: this.runForm.sessionId || 'default'
      }, {
        onEvent: ({ type, payload }) => {
          if (type === 'delta') {
            assistant.content += (payload && payload.text) ? payload.text : (typeof payload === 'string' ? payload : '')
            this.scrollRun()
          } else if (type === 'tool') {
            assistant.tools.push(payload || {})
          }
        },
        onDone: (payload) => {
          assistant.streaming = false
          if ((!assistant.content) && payload && payload.content) assistant.content = payload.content
          if (payload && payload.toolTrace && payload.toolTrace.length && !assistant.tools.length) {
            assistant.tools = payload.toolTrace
          }
          this.runLoading = false
          this.scrollRun()
        },
        onError: (err) => {
          assistant.streaming = false
          assistant.content = assistant.content || ('出错：' + ((err && err.message) || '未知错误'))
          this.runLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}
.agent-card {
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  padding: 14px 16px 12px;
  background: #fff;
}
.agent-card.selected {
  border-color: #b3d8ff;
  box-shadow: 0 0 0 1px #d9ecff;
}
.agent-card__head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.agent-card__check {
  margin-top: 2px;
}
.agent-card__title {
  flex: 1;
  min-width: 0;
}
.agent-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2a37;
  line-height: 1.3;
}
.agent-card__code {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}
.agent-card__binds {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 24px;
}
.agent-card__more {
  font-size: 12px;
  color: #909399;
  line-height: 22px;
}
.agent-card__actions {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.agent-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #909399;
  padding: 36px 0;
}
.run-meta { display: flex; align-items: center; margin-bottom: 8px; font-size: 13px; color: #606266; }
.run-chat { height: 360px; overflow-y: auto; background: #f7f8fa; padding: 12px; border-radius: 4px; margin-bottom: 10px; }
.run-bubble { margin-bottom: 10px; max-width: 86%; }
.run-bubble.user { margin-left: auto; }
.run-role { font-size: 12px; color: #909399; margin-bottom: 4px; }
.run-text { white-space: pre-wrap; word-break: break-word; padding: 8px 12px; border-radius: 6px; background: #fff; line-height: 1.5; }
.run-bubble.user .run-text { background: #409EFF; color: #fff; }
.run-tools { margin-top: 4px; }
.run-tools .el-tag { margin-right: 4px; }
</style>

<template>
  <div class="app-container">
    <div class="model-status">
      <div class="status-block">
        <div class="status-label">默认模型</div>
        <div class="status-value">{{ statusDefault }}</div>
        <div class="status-hint">助手与智能体未指定时使用</div>
      </div>
      <div class="status-block">
        <div class="status-label">API Key</div>
        <div class="status-value" :class="{ warn: statusKeyMissing }">{{ statusKey }}</div>
        <div class="status-hint">{{ lastTest || '保存后同步给助手、智能体与链路' }}</div>
      </div>
      <div class="status-block">
        <div class="status-label">日配额</div>
        <div class="status-value">{{ statusQuota }}</div>
        <div class="status-hint">默认模型当日调用上限</div>
      </div>
    </div>
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="modelCode">
        <el-input v-model="queryParams.modelCode" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="全部">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:model:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['aikit:model:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" prop="modelCode" min-width="120" />
      <el-table-column label="名称" prop="modelName" min-width="120" />
      <el-table-column label="供应商" prop="provider" width="120" />
      <el-table-column label="模型" prop="model" min-width="120" />
      <el-table-column label="API Key" prop="apiKeyMasked" width="130" />
      <el-table-column label="日调用" prop="dailyCallLimit" width="90" align="center">
        <template slot-scope="scope">{{ scope.row.dailyCallLimit != null ? scope.row.dailyCallLimit : '不限' }}</template>
      </el-table-column>
      <el-table-column label="默认" prop="isDefault" width="70" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isDefault === '1'" type="success" size="mini">是</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['aikit:model:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-connection" @click="handleTest(scope.row)" v-hasPermi="['aikit:model:test']">测试</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['aikit:model:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" size="small">
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="form.modelCode" :disabled="form.id != null" placeholder="如 deepseek-default" />
        </el-form-item>
        <el-form-item label="显示名称" prop="modelName">
          <el-input v-model="form.modelName" />
        </el-form-item>
        <el-form-item label="供应商" prop="provider">
          <el-select v-model="form.provider" style="width:100%">
            <el-option label="deepseek" value="deepseek" />
            <el-option label="openai-compatible" value="openai-compatible" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名" prop="model">
          <el-input v-model="form.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password :placeholder="form.id ? '留空表示不修改' : '必填'" />
        </el-form-item>
        <el-form-item label="日调用上限">
          <el-input-number v-model="form.dailyCallLimit" :min="0" :max="999999" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="日 Token 上限">
          <el-input-number v-model="form.dailyTokenLimit" :min="0" :max="99999999" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-radio-group v-model="form.isDefault">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
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
import { listAiModel, getAiModel, addAiModel, updateAiModel, delAiModel, testAiModel, getAiModelSources } from '@/api/aikit/platform'

export default {
  name: 'Model',
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
      sourceInfo: {},
      lastTest: '',
      queryParams: { pageNum: 1, pageSize: 10, modelCode: undefined, status: undefined },
      form: {},
      rules: {
        modelCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
        provider: [{ required: true, message: '供应商不能为空', trigger: 'change' }],
        model: [{ required: true, message: '模型名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    this.loadSources()
  },
  computed: {
    defaultModel() {
      return this.list.find(m => m.isDefault === '1') || null
    },
    statusDefault() {
      const m = this.defaultModel
      if (m) return m.modelName || m.modelCode
      const ai = this.sourceInfo.aiModel || {}
      return ai.modelCode || ai.model || '未设置'
    },
    statusKeyMissing() {
      const m = this.defaultModel
      if (m) return m.hasApiKey === false || m.apiKeyMasked === '（未配置）'
      const ai = this.sourceInfo.aiModel || {}
      return !ai.configured
    },
    statusKey() {
      const m = this.defaultModel
      if (m && m.apiKeyMasked) return m.apiKeyMasked
      const ai = this.sourceInfo.aiModel || {}
      return ai.configured ? '已配置' : '未配置'
    },
    statusQuota() {
      const m = this.defaultModel
      if (!m || m.dailyCallLimit == null || m.dailyCallLimit === 0) return '不限'
      return m.dailyCallLimit + ' 次/日'
    }
  },
  methods: {
    loadSources() {
      getAiModelSources().then(res => {
        this.sourceInfo = res.data || {}
      }).catch(() => {})
    },
    getList() {
      this.loading = true
      listAiModel(this.queryParams).then(res => {
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
        id: undefined, modelCode: undefined, modelName: undefined, provider: 'deepseek',
        baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat', apiKey: undefined,
        dailyCallLimit: undefined, dailyTokenLimit: undefined,
        status: '0', isDefault: '1', remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增模型'
    },
    handleUpdate(row) {
      this.reset()
      getAiModel(row.id).then(res => {
        this.form = Object.assign({}, res.data, { apiKey: undefined })
        this.open = true
        this.title = '修改模型'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id != null ? updateAiModel(this.form) : addAiModel(this.form)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
          this.loadSources()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除？').then(() => delAiModel(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleTest(row) {
      this.$modal.loading('连通测试中...')
      testAiModel({ id: row.id }).then(res => {
        this.$modal.closeLoading()
        const reply = (res.data && res.data.reply) || 'ok'
        this.lastTest = '连通成功：' + reply
        this.$modal.msgSuccess(this.lastTest)
      }).catch(() => {
        this.$modal.closeLoading()
        this.lastTest = '连通失败，请检查 Key 与 Base URL'
      })
    }
  }
}
</script>

<style scoped>
.model-status {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.status-block {
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fafbfc;
}
.status-label {
  font-size: 12px;
  color: #8a94a6;
}
.status-value {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2a37;
  line-height: 1.3;
  word-break: break-all;
}
.status-value.warn {
  color: #e6a23c;
}
.status-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
@media (max-width: 900px) {
  .model-status {
    grid-template-columns: 1fr;
  }
}
</style>

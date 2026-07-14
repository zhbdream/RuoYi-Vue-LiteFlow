<template>
  <div class="app-container">
    <el-alert
      title="API Key 仅写入时提交，入库 AES 加密；列表不回传明文。设为默认后，riskAgent 优先使用该配置（覆盖空的 yml）。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="modelCode">
        <el-input v-model="queryParams.modelCode" placeholder="model_code" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="provider">
        <el-select v-model="queryParams.provider" clearable placeholder="全部">
          <el-option label="deepseek" value="deepseek" />
          <el-option label="openai-compatible" value="openai-compatible" />
        </el-select>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['liteflow:agent:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['liteflow:agent:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编码" prop="modelCode" min-width="130" :show-overflow-tooltip="true" />
      <el-table-column label="名称" prop="modelName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="供应商" prop="provider" width="120" align="center" />
      <el-table-column label="configKey" prop="configKey" width="100" align="center" />
      <el-table-column label="模型" prop="model" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="API Key" prop="apiKeyMasked" width="130" align="center" />
      <el-table-column label="默认" prop="isDefault" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isDefault === '1' ? 'success' : 'info'" size="mini">{{ scope.row.isDefault === '1' ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'" size="mini">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="日调用上限" prop="dailyCallLimit" width="100" align="center" />
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['liteflow:agent:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['liteflow:agent:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="form.modelCode" placeholder="如 deepseek-default" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="显示名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 DeepSeek 默认" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="供应商" prop="provider">
              <el-select v-model="form.provider" style="width:100%">
                <el-option label="deepseek" value="deepseek" />
                <el-option label="openai-compatible" value="openai-compatible" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="configKey" prop="configKey">
              <el-input v-model="form.configKey" placeholder="deepseek" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模型名" prop="model">
          <el-input v-model="form.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="form.id ? '留空表示不修改已有 Key' : '必填，如 sk-xxx'"
          />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="是否默认">
              <el-radio-group v-model="form.isDefault">
                <el-radio label="1">是</el-radio>
                <el-radio label="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio label="0">正常</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="日调用上限">
              <el-input-number v-model="form.dailyCallLimit" :min="0" :max="999999" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日Token上限">
              <el-input-number v-model="form.dailyTokenLimit" :min="0" :max="99999999" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { listAgentModel, getAgentModel, addAgentModel, updateAgentModel, delAgentModel } from '@/api/liteflow/agent'

export default {
  name: 'LfAgentModel',
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
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        modelCode: undefined,
        provider: undefined,
        status: undefined
      },
      form: {},
      rules: {
        modelCode: [{ required: true, message: '模型编码不能为空', trigger: 'blur' }],
        provider: [{ required: true, message: '供应商不能为空', trigger: 'change' }],
        configKey: [{ required: true, message: 'configKey 不能为空', trigger: 'blur' }],
        model: [{ required: true, message: '模型名不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAgentModel(this.queryParams).then(res => {
        this.list = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
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
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    reset() {
      this.form = {
        id: undefined,
        modelCode: undefined,
        modelName: undefined,
        provider: 'deepseek',
        configKey: 'deepseek',
        baseUrl: 'https://api.deepseek.com/v1',
        model: 'deepseek-chat',
        apiKey: undefined,
        status: '0',
        isDefault: '1',
        dailyCallLimit: undefined,
        dailyTokenLimit: undefined,
        remark: undefined
      }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增 Agent 模型'
    },
    handleUpdate(row) {
      this.reset()
      getAgentModel(row.id).then(res => {
        this.form = Object.assign({}, res.data, { apiKey: undefined })
        this.open = true
        this.title = '修改 Agent 模型'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (!this.form.id && !this.form.apiKey) {
          this.$modal.msgError('新增时必须填写 API Key')
          return
        }
        const payload = { ...this.form }
        if (!payload.apiKey) {
          delete payload.apiKey
        }
        const req = payload.id != null ? updateAgentModel(payload) : addAgentModel(payload)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('确认删除选中的模型配置？').then(() => {
        return delAgentModel(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

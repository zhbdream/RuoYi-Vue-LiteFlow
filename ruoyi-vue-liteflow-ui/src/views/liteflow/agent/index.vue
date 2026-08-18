<template>
  <div class="app-container">
    <el-alert
      title="本页已合并到「AI能力 → 模型管理」，请到新入口维护 Key 与日配额。此处仅兼容旧地址。"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
    />
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

    <div v-loading="loading" class="model-card-grid">
      <div
        v-for="item in list"
        :key="item.id"
        class="model-card"
        :class="{
          'is-selected': ids.includes(item.id),
          'is-default': item.isDefault === '1',
          'is-disabled': item.status !== '0'
        }"
      >
        <div class="model-card__header">
          <el-checkbox
            :value="ids.includes(item.id)"
            @change="checked => toggleSelect(item, checked)"
          />
          <div class="model-card__brand">
            <div class="model-card__avatar" :class="'provider-' + (item.provider || 'default')">
              {{ providerInitial(item.provider) }}
            </div>
            <div class="model-card__titles">
              <div class="model-card__name" :title="item.modelName || item.modelCode">
                {{ item.modelName || item.modelCode || '未命名模型' }}
              </div>
              <div class="model-card__code">{{ item.modelCode }}</div>
            </div>
          </div>
          <div class="model-card__tags">
            <el-tag v-if="item.isDefault === '1'" type="success" size="mini" effect="dark">默认</el-tag>
            <el-tag :type="item.status === '0' ? 'success' : 'info'" size="mini">
              {{ item.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </div>
        </div>

        <div class="model-card__body">
          <div class="model-card__row">
            <span class="label">供应商</span>
            <span class="value">{{ item.provider || '-' }}</span>
          </div>
          <div class="model-card__row">
            <span class="label">模型</span>
            <span class="value mono">{{ item.model || '-' }}</span>
          </div>
          <div class="model-card__row">
            <span class="label">configKey</span>
            <span class="value mono">{{ item.configKey || '-' }}</span>
          </div>
          <div class="model-card__row">
            <span class="label">API Key</span>
            <span class="value" :class="{ ok: !!item.apiKeyMasked }">
              <i :class="item.apiKeyMasked ? 'el-icon-success' : 'el-icon-warning-outline'" />
              {{ item.apiKeyMasked || '未配置' }}
            </span>
          </div>
          <div class="model-card__row">
            <span class="label">日调用上限</span>
            <span class="value">{{ item.dailyCallLimit != null ? item.dailyCallLimit : '不限' }}</span>
          </div>
        </div>

        <div class="model-card__footer">
          <el-button
            type="text"
            icon="el-icon-edit"
            size="mini"
            @click="handleUpdate(item)"
            v-hasPermi="['liteflow:agent:edit']"
          >编辑</el-button>
          <el-button
            type="text"
            icon="el-icon-delete"
            size="mini"
            class="danger-text"
            @click="handleDelete(item)"
            v-hasPermi="['liteflow:agent:remove']"
          >删除</el-button>
        </div>
      </div>

      <div v-if="!loading && list.length === 0" class="model-card-empty">
        <i class="el-icon-cpu" />
        <p>暂无模型配置</p>
        <el-button type="primary" size="mini" icon="el-icon-plus" @click="handleAdd" v-hasPermi="['liteflow:agent:add']">新增模型</el-button>
      </div>
    </div>

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
        pageSize: 12,
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
    providerInitial(provider) {
      if (!provider) return 'M'
      if (provider === 'deepseek') return 'DS'
      if (provider === 'openai-compatible') return 'AI'
      return provider.slice(0, 2).toUpperCase()
    },
    toggleSelect(item, checked) {
      if (checked) {
        if (!this.ids.includes(item.id)) {
          this.ids = this.ids.concat(item.id)
        }
      } else {
        this.ids = this.ids.filter(id => id !== item.id)
      }
      this.multiple = !this.ids.length
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
      const ids = row && row.id ? row.id : this.ids
      this.$modal.confirm('确认删除选中的模型配置？').then(() => {
        return delAgentModel(ids)
      }).then(() => {
        this.ids = []
        this.multiple = true
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.model-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 160px;
}

.model-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.model-card:hover {
  border-color: #c6e2ff;
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.1);
  transform: translateY(-2px);
}

.model-card.is-selected {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.25);
}

.model-card.is-default {
  border-color: #67c23a;
}

.model-card.is-default.is-selected {
  border-color: #409eff;
}

.model-card.is-disabled {
  opacity: 0.72;
}

.model-card__header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 14px 10px;
  border-bottom: 1px solid #f2f3f5;
  background: linear-gradient(180deg, #f8fbff 0%, #fff 100%);
}

.model-card__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.model-card__avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  background: #909399;
}

.model-card__avatar.provider-deepseek {
  background: linear-gradient(135deg, #4f6ef7, #2b5cff);
}

.model-card__avatar.provider-openai-compatible {
  background: linear-gradient(135deg, #10a37f, #0d8a6a);
}

.model-card__titles {
  min-width: 0;
}

.model-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-card__code {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-card__tags {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.model-card__body {
  flex: 1;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.model-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  line-height: 1.4;
}

.model-card__row .label {
  color: #909399;
  flex-shrink: 0;
}

.model-card__row .value {
  color: #606266;
  text-align: right;
  word-break: break-all;
}

.model-card__row .value.mono {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
}

.model-card__row .value.ok {
  color: #67c23a;
}

.model-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 6px 10px;
  border-top: 1px solid #f2f3f5;
  background: #fafbfc;
}

.model-card__footer .danger-text {
  color: #f56c6c;
}

.model-card-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: #909399;
  background: #fafbfc;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
}

.model-card-empty i {
  font-size: 36px;
  margin-bottom: 8px;
  color: #c0c4cc;
}

.model-card-empty p {
  margin: 0 0 12px;
  font-size: 14px;
}
</style>

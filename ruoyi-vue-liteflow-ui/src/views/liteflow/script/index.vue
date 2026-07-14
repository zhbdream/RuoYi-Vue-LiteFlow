<template>
  <div class="app-container">
    <el-alert
      v-if="liteflowReadonly"
      :title="liteflowReadonlyMessage || '当前环境为只读模式，禁止修改脚本'"
      type="info"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="72px">
      <el-form-item label="脚本ID" prop="scriptId">
        <el-input v-model="queryParams.scriptId" placeholder="脚本ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="scriptType">
        <el-select v-model="queryParams.scriptType" placeholder="脚本类型" clearable>
          <el-option label="普通 script" value="script" />
          <el-option label="布尔 boolean_script" value="boolean_script" />
          <el-option label="选择 switch_script" value="switch_script" />
          <el-option label="循环 for_script" value="for_script" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" :disabled="liteflowReadonly" @click="handleAdd" v-hasPermi="['liteflow:script:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple || liteflowReadonly" @click="handleDelete" v-hasPermi="['liteflow:script:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="scriptList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="70" align="center" />
      <el-table-column label="脚本ID" prop="scriptId" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="名称" prop="scriptName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="类型" prop="scriptType" width="120" align="center" />
      <el-table-column label="语言" prop="scriptLanguage" width="90" align="center" />
      <el-table-column label="版本" prop="version" width="70" align="center" />
      <el-table-column label="生效" prop="enable" width="70" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enable === 1 ? 'success' : 'info'" size="mini">{{ scope.row.enable === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" :disabled="liteflowReadonly" @click="handleUpdate(scope.row)" v-hasPermi="['liteflow:script:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-time" @click="showVersions(scope.row)" v-hasPermi="['liteflow:script:query']">版本</el-button>
          <el-button size="mini" type="text" icon="el-icon-view" @click="showRefs(scope.row)" v-hasPermi="['liteflow:script:query']">引用</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" :disabled="liteflowReadonly" @click="handleDelete(scope.row)" v-hasPermi="['liteflow:script:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="820px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="脚本ID" prop="scriptId">
          <el-input v-model="form.scriptId" placeholder="如 scriptPriceAdjust" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="脚本名称" prop="scriptName">
          <el-input v-model="form.scriptName" placeholder="显示名称" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="脚本类型" prop="scriptType">
              <el-select v-model="form.scriptType" style="width:100%">
                <el-option label="script" value="script" />
                <el-option label="boolean_script" value="boolean_script" />
                <el-option label="switch_script" value="switch_script" />
                <el-option label="for_script" value="for_script" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="语言" prop="scriptLanguage">
              <el-select v-model="form.scriptLanguage" style="width:100%">
                <el-option label="groovy" value="groovy" />
                <el-option label="qlexpress" value="qlexpress" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="是否生效">
          <el-radio-group v-model="form.enable">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="脚本内容" prop="scriptData">
          <el-input v-model="form.scriptData" type="textarea" :rows="14" placeholder="Groovy 脚本内容" class="script-editor" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleValidate" v-hasPermi="['liteflow:script:edit']">校验语法</el-button>
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="脚本引用分析" :visible.sync="refsOpen" width="480px" append-to-body>
      <p v-if="refChains.length === 0">暂无链路引用此脚本</p>
      <el-tag v-for="name in refChains" :key="name" style="margin:4px">{{ name }}</el-tag>
    </el-dialog>

    <el-dialog :title="'脚本版本 — ' + (versionScriptId || '')" :visible.sync="versionsOpen" width="720px" append-to-body>
      <el-table :data="versionList" size="small" max-height="360">
        <el-table-column label="版本" prop="version" width="80" align="center" />
        <el-table-column label="保存人" prop="publishBy" width="100" />
        <el-table-column label="时间" prop="createTime" width="160" />
        <el-table-column label="备注" prop="remark" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="showVersionDetail(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!versionList.length" description="暂无历史版本（修改脚本内容后才会产生快照）" :image-size="60" />
    </el-dialog>

    <el-dialog title="版本快照内容" :visible.sync="versionDetailOpen" width="820px" append-to-body>
      <el-descriptions v-if="versionDetail" :column="2" border size="small">
        <el-descriptions-item label="版本">v{{ versionDetail.version }}</el-descriptions-item>
        <el-descriptions-item label="保存人">{{ versionDetail.publishBy }}</el-descriptions-item>
        <el-descriptions-item label="语言">{{ versionDetail.scriptLanguage }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ versionDetail.scriptType }}</el-descriptions-item>
      </el-descriptions>
      <el-input
        v-if="versionDetail"
        type="textarea"
        :rows="14"
        :value="versionDetail.scriptData"
        readonly
        class="script-editor"
        style="margin-top:12px"
      />
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listScript, getScript, addScript, updateScript, delScript, validateScript, getScriptRefs, listScriptVersions, getScriptVersion } from '@/api/liteflow/platform'

export default {
  name: 'LfScript',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      scriptList: [],
      ids: [],
      multiple: true,
      title: '',
      open: false,
      refsOpen: false,
      refChains: [],
      versionsOpen: false,
      versionList: [],
      versionScriptId: '',
      versionDetailOpen: false,
      versionDetail: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        scriptId: undefined,
        scriptType: undefined
      },
      form: {},
      rules: {
        scriptId: [{ required: true, message: '脚本ID不能为空', trigger: 'blur' }],
        scriptData: [{ required: true, message: '脚本内容不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    ...mapGetters(['liteflowReadonly', 'liteflowReadonlyMessage'])
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listScript(this.queryParams).then(res => {
        this.scriptList = res.rows
        this.total = res.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        applicationName: 'ruoyi-liteflow',
        scriptId: undefined,
        scriptName: undefined,
        scriptData: '',
        scriptType: 'script',
        scriptLanguage: 'groovy',
        enable: 1,
        remark: undefined
      }
      this.resetForm('form')
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
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增脚本'
    },
    handleUpdate(row) {
      this.reset()
      getScript(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '编辑脚本'
      })
    },
    handleValidate() {
      if (!this.form.scriptData) {
        this.$modal.msgError('请先填写脚本内容')
        return
      }
      validateScript(this.form).then(() => {
        this.$modal.msgSuccess('脚本语法校验通过')
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id != null ? updateScript(this.form) : addScript(this.form)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('确认删除选中的脚本？').then(() => delScript(ids)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    showRefs(row) {
      getScriptRefs(row.scriptId).then(res => {
        this.refChains = res.data || []
        this.refsOpen = true
      })
    },
    showVersions(row) {
      this.versionScriptId = row.scriptId
      listScriptVersions(row.id).then(res => {
        this.versionList = res.data || []
        this.versionsOpen = true
      })
    },
    showVersionDetail(row) {
      getScriptVersion(row.id).then(res => {
        this.versionDetail = res.data
        this.versionDetailOpen = true
      })
    }
  }
}
</script>

<style scoped>
.script-editor ::v-deep textarea {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}
</style>

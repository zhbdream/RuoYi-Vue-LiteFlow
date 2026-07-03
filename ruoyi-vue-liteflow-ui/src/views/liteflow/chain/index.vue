<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="链路ID" prop="chainName">
        <el-input v-model="queryParams.chainName" placeholder="请输入链路ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['liteflow:chain:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-document-copy" size="mini" @click="openTemplateDialog" v-hasPermi="['liteflow:chain:add']">从模板创建</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-upload2" size="mini" @click="openImportDialog" v-hasPermi="['liteflow:chain:add']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['liteflow:chain:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['liteflow:chain:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-guide" size="mini" @click="openRouteExecute" v-hasPermi="['liteflow:execute']">决策路由试跑</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="chainList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="链路ID" align="center" prop="chainName" :show-overflow-tooltip="true" />
      <el-table-column label="描述" align="center" prop="chainDesc" :show-overflow-tooltip="true" />
      <el-table-column label="版本" align="center" prop="version" width="70" />
      <el-table-column label="发布" align="center" prop="draftFlag" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.draftFlag === '1' ? 'warning' : 'success'" size="mini">{{ scope.row.draftFlag === '1' ? '草稿' : '已发布' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-s-operation" @click="goEditor(scope.row)" v-hasPermi="['liteflow:editor:view']">编排</el-button>
          <el-button size="mini" type="text" icon="el-icon-video-play" @click="handleExecute(scope.row)" v-hasPermi="['liteflow:execute']">试跑</el-button>
          <el-button
            v-if="scope.row.draftFlag === '1'"
            size="mini"
            type="text"
            icon="el-icon-upload"
            @click="handlePublish(scope.row)"
            v-hasPermi="['liteflow:chain:edit']"
          >发布</el-button>
          <el-dropdown size="mini" @command="(command) => handleCommand(command, scope.row)">
            <el-button size="mini" type="text" icon="el-icon-d-arrow-right">更多</el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="edit" icon="el-icon-edit" v-hasPermi="['liteflow:chain:edit']">编辑</el-dropdown-item>
              <el-dropdown-item command="audit" icon="el-icon-document" v-hasPermi="['liteflow:audit:list']">审计</el-dropdown-item>
              <el-dropdown-item command="version" icon="el-icon-time" v-hasPermi="['liteflow:chain:query']">版本</el-dropdown-item>
              <el-dropdown-item command="permission" icon="el-icon-user" v-hasPermi="['liteflow:chain:permission']">执行权限</el-dropdown-item>
              <el-dropdown-item command="clone" icon="el-icon-copy-document" v-hasPermi="['liteflow:chain:add']">克隆</el-dropdown-item>
              <el-dropdown-item command="export" icon="el-icon-download" v-hasPermi="['liteflow:chain:query']">导出</el-dropdown-item>
              <el-dropdown-item command="reload" icon="el-icon-refresh" v-hasPermi="['liteflow:chain:reload']">热刷新</el-dropdown-item>
              <el-dropdown-item command="delete" icon="el-icon-delete" divided v-hasPermi="['liteflow:chain:remove']">删除</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="链路ID" prop="chainName">
          <el-input v-model="form.chainName" placeholder="如 helloChain" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="链路描述" prop="chainDesc">
          <el-input v-model="form.chainDesc" placeholder="链路说明" />
        </el-form-item>
        <el-form-item label="上下文Class" prop="contextClass">
          <el-input v-model="form.contextClass" placeholder="可选，如 OrderContext 全限定名" />
        </el-form-item>
        <el-form-item label="决策路由 EL" prop="routeEl">
          <el-input v-model="form.routeEl" placeholder="可选，如 isNewCustomer（布尔组件）" />
        </el-form-item>
        <el-form-item label="路由 namespace" prop="namespace">
          <el-input v-model="form.namespace" placeholder="可选，如 routeDemo" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="EL 表达式" prop="elData">
          <el-input v-model="form.elData" type="textarea" :rows="8" placeholder="THEN(a, b, c);" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="链路试跑" :visible.sync="executeOpen" width="700px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="链路ID">
          <el-input v-model="executeChainName" disabled />
        </el-form-item>
        <el-form-item label="请求 JSON">
          <el-input v-model="executeParamJson" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <el-alert v-if="executeResult" :title="executeResult.success ? '执行成功' : '执行失败'" :type="executeResult.success ? 'success' : 'error'" show-icon :closable="false" />
      <pre v-if="executeResult" class="execute-result">{{ formatResult(executeResult) }}</pre>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitExecute">执 行</el-button>
        <el-button @click="executeOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="决策路由试跑" :visible.sync="routeExecuteOpen" width="760px" append-to-body>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        按 namespace 遍历带决策路由的链路，命中规则并行执行。Demo5 请使用 namespace=<strong>routeDemo</strong>，userType=NEW 或 RETURNING。
      </el-alert>
      <el-form label-width="110px">
        <el-form-item label="namespace">
          <el-input v-model="routeExecuteForm.namespace" placeholder="routeDemo" />
        </el-form-item>
        <el-form-item label="上下文 Class">
          <el-input v-model="routeExecuteForm.contextClass" placeholder="com.ruoyiliteflow.liteflow.domain.context.RouteUserContext" />
        </el-form-item>
        <el-form-item label="请求 JSON">
          <el-input v-model="routeExecuteForm.paramJson" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <el-alert v-if="routeExecuteResult" :title="'命中 ' + (routeExecuteResult.hitCount || 0) + ' 条规则'" type="success" show-icon :closable="false" />
      <pre v-if="routeExecuteResult" class="execute-result">{{ formatResult(routeExecuteResult) }}</pre>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitRouteExecute">执 行</el-button>
        <el-button @click="routeExecuteOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="从模板创建链路" :visible.sync="templateOpen" width="560px" append-to-body>
      <el-form ref="templateForm" :model="templateForm" :rules="templateRules" label-width="100px">
        <el-form-item label="选择模板" prop="templateKey">
          <el-select v-model="templateForm.templateKey" placeholder="请选择模板" @change="onTemplateChange">
            <el-option v-for="t in chainTemplates" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="链路ID" prop="chainName">
          <el-input v-model="templateForm.chainName" placeholder="如 myOrderChain" />
        </el-form-item>
        <el-form-item label="链路描述">
          <el-input v-model="templateForm.chainDesc" />
        </el-form-item>
        <el-form-item label="创建后">
          <el-checkbox v-model="templateForm.openEditor">打开可视化编排</el-checkbox>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitTemplate">创 建</el-button>
        <el-button @click="templateOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="克隆链路" :visible.sync="cloneOpen" width="520px" append-to-body>
      <el-form ref="cloneForm" :model="cloneForm" :rules="cloneRules" label-width="100px">
        <el-form-item label="源链路">
          <el-input :value="cloneForm.sourceName" disabled />
        </el-form-item>
        <el-form-item label="新链路ID" prop="chainName">
          <el-input v-model="cloneForm.chainName" placeholder="如 orderProcessCopy" />
        </el-form-item>
        <el-form-item label="链路描述">
          <el-input v-model="cloneForm.chainDesc" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitClone">确 定</el-button>
        <el-button @click="cloneOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="导入链路 JSON" :visible.sync="importOpen" width="680px" append-to-body>
      <el-input v-model="importJson" type="textarea" :rows="16" placeholder="粘贴 export 导出的 JSON" />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitImport">导 入</el-button>
        <el-button @click="importOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="链路执行权限" :visible.sync="permissionOpen" width="640px" append-to-body>
      <p class="perm-chain-name">链路：<strong>{{ permissionChainName }}</strong></p>
      <el-table v-loading="permissionLoading" :data="permissionRows" border size="small" max-height="360">
        <el-table-column label="角色" prop="roleName" min-width="140" />
        <el-table-column label="可执行" width="90" align="center">
          <template slot-scope="scope">
            <el-checkbox v-model="scope.row.canExecute" true-label="1" false-label="0" />
          </template>
        </el-table-column>
        <el-table-column label="可编排" width="90" align="center">
          <template slot-scope="scope">
            <el-checkbox v-model="scope.row.canEdit" true-label="1" false-label="0" />
          </template>
        </el-table-column>
      </el-table>
      <p class="perm-tip">未勾选任何角色并保存 = 清除限制，恢复仅菜单权限控制。</p>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitPermission">保 存</el-button>
        <el-button @click="permissionOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listChain, getChain, delChain, addChain, updateChain, reloadChain, executeChain, executeRoute, publishChain, cloneChain, exportChain, importChain, listChainPermission, saveChainPermission } from '@/api/liteflow/chain'
import { listRole } from '@/api/system/role'
import { CHAIN_TEMPLATES, getDefaultExecuteParam } from '@/utils/liteflow/chainTemplates'

export default {
  name: 'LfChain',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      chainList: [],
      title: '',
      open: false,
      executeOpen: false,
      executeChainName: '',
      executeParamJson: '{}',
      executeResult: null,
      routeExecuteOpen: false,
      routeExecuteForm: {
        namespace: 'routeDemo',
        contextClass: 'com.ruoyiliteflow.liteflow.domain.context.RouteUserContext',
        paramJson: '{\n  "userType": "NEW"\n}'
      },
      routeExecuteResult: null,
      templateOpen: false,
      cloneOpen: false,
      importOpen: false,
      importJson: '',
      permissionOpen: false,
      permissionLoading: false,
      permissionChainName: '',
      permissionRows: [],
      allRoles: [],
      cloneForm: {
        id: null,
        sourceName: '',
        chainName: '',
        chainDesc: ''
      },
      cloneRules: {
        chainName: [{ required: true, message: '新链路ID不能为空', trigger: 'blur' }]
      },
      chainTemplates: CHAIN_TEMPLATES,
      templateForm: {
        templateKey: 'helloChain',
        chainName: '',
        chainDesc: '',
        openEditor: true
      },
      templateRules: {
        templateKey: [{ required: true, message: '请选择模板', trigger: 'change' }],
        chainName: [{ required: true, message: '链路ID不能为空', trigger: 'blur' }]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        chainName: undefined,
        status: undefined
      },
      form: {},
      rules: {
        chainName: [{ required: true, message: '链路ID不能为空', trigger: 'blur' }],
        elData: [{ required: true, message: 'EL 表达式不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    listRole({ pageNum: 1, pageSize: 200, status: '0' }).then(res => {
      this.allRoles = (res.rows || []).map(r => ({
        roleId: r.roleId,
        roleName: r.roleName,
        canExecute: '0',
        canEdit: '0'
      }))
    })
  },
  methods: {
    getList() {
      this.loading = true
      listChain(this.queryParams).then(response => {
        this.chainList = response.rows
        this.total = response.total
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
        chainName: undefined,
        chainDesc: undefined,
        elData: undefined,
        graphJson: undefined,
        enable: 1,
        status: '0',
        draftFlag: '0',
        version: 1,
        contextClass: undefined,
        routeEl: undefined,
        namespace: undefined,
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
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增链路'
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids[0]
      getChain(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '编辑链路'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.form.enable = this.form.status === '0' ? 1 : 0
          if (this.form.id != null) {
            updateChain(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addChain(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除链路编号为"' + ids + '"的数据项？').then(() => {
        return delChain(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleReload(row) {
      reloadChain(row.chainName).then(() => {
        this.$modal.msgSuccess('热刷新成功')
      })
    },
    goEditor(row) {
      this.$router.push({ path: '/liteflow/editor', query: { chainId: row.chainName, id: row.id } })
    },
    goAudit(row) {
      this.$router.push({ path: '/liteflow/audit', query: { chainName: row.chainName } })
    },
    goVersion(row) {
      this.$router.push({ path: '/liteflow/version', query: { chainId: row.id, chainName: row.chainName } })
    },
    handleCommand(command, row) {
      const handlers = {
        edit: () => this.handleUpdate(row),
        audit: () => this.goAudit(row),
        version: () => this.goVersion(row),
        permission: () => this.openPermissionDialog(row),
        clone: () => this.openCloneDialog(row),
        export: () => this.handleExport(row),
        reload: () => this.handleReload(row),
        delete: () => this.handleDelete(row)
      }
      const handler = handlers[command]
      if (handler) {
        handler()
      }
    },
    handleExecute(row) {
      this.executeChainName = row.chainName
      this.executeParamJson = JSON.stringify(getDefaultExecuteParam(row.chainName), null, 2)
      this.executeResult = null
      this.executeOpen = true
    },
    openTemplateDialog() {
      this.templateForm = {
        templateKey: 'helloChain',
        chainName: '',
        chainDesc: CHAIN_TEMPLATES[1].chainDesc,
        openEditor: true
      }
      this.templateOpen = true
    },
    onTemplateChange(key) {
      const t = CHAIN_TEMPLATES.find(item => item.key === key)
      if (t) {
        this.templateForm.chainDesc = t.chainDesc
        if (!this.templateForm.chainName) {
          this.templateForm.chainName = key === 'blank' ? '' : key + 'Copy'
        }
      }
    },
    submitTemplate() {
      this.$refs['templateForm'].validate(valid => {
        if (!valid) {
          return
        }
        const t = CHAIN_TEMPLATES.find(item => item.key === this.templateForm.templateKey)
        if (!t) {
          return
        }
        const data = {
          applicationName: 'ruoyi-liteflow',
          chainName: this.templateForm.chainName,
          chainDesc: this.templateForm.chainDesc || t.chainDesc,
          elData: t.elData || 'THEN(helloA);',
          contextClass: t.contextClass || undefined,
          enable: 1,
          status: '0',
          draftFlag: '0',
          version: 1
        }
        addChain(data).then(() => {
          this.$modal.msgSuccess('创建成功')
          this.templateOpen = false
          this.getList()
          if (this.templateForm.openEditor) {
            this.$router.push({ path: '/liteflow/editor', query: { chainId: data.chainName } })
          }
        })
      })
    },
    submitExecute() {
      let param = {}
      try {
        param = JSON.parse(this.executeParamJson || '{}')
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      executeChain(this.executeChainName, param).then(response => {
        this.executeResult = response.data
      })
    },
    openRouteExecute() {
      this.routeExecuteResult = null
      this.routeExecuteOpen = true
    },
    submitRouteExecute() {
      let param = {}
      try {
        param = JSON.parse(this.routeExecuteForm.paramJson || '{}')
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      executeRoute({
        namespace: this.routeExecuteForm.namespace,
        contextClass: this.routeExecuteForm.contextClass,
        param: param
      }).then(response => {
        this.routeExecuteResult = response.data
      })
    },
    formatResult(result) {
      return JSON.stringify(result, null, 2)
    },
    handlePublish(row) {
      this.$modal.confirm('确认发布链路 "' + row.chainName + '" ？发布后将热刷新生效。').then(() => {
        return publishChain(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('发布成功')
      }).catch(() => {})
    },
    openCloneDialog(row) {
      this.cloneForm = {
        id: row.id,
        sourceName: row.chainName,
        chainName: row.chainName + 'Copy',
        chainDesc: (row.chainDesc || '') + ' (克隆)'
      }
      this.cloneOpen = true
    },
    submitClone() {
      this.$refs.cloneForm.validate(valid => {
        if (!valid) return
        cloneChain({
          id: this.cloneForm.id,
          chainName: this.cloneForm.chainName,
          chainDesc: this.cloneForm.chainDesc
        }).then(() => {
          this.$modal.msgSuccess('克隆成功（草稿状态）')
          this.cloneOpen = false
          this.getList()
        })
      })
    },
    handleExport(row) {
      exportChain(row.id).then(res => {
        const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = row.chainName + '.json'
        link.click()
        window.URL.revokeObjectURL(url)
      })
    },
    openImportDialog() {
      this.importJson = ''
      this.importOpen = true
    },
    submitImport() {
      let data
      try {
        data = JSON.parse(this.importJson || '{}')
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      importChain(data).then(() => {
        this.$modal.msgSuccess('导入成功（草稿状态，请发布后生效）')
        this.importOpen = false
        this.getList()
      })
    },
    openPermissionDialog(row) {
      this.permissionChainName = row.chainName
      this.permissionOpen = true
      this.permissionLoading = true
      this.permissionRows = this.allRoles.map(r => ({ ...r, canExecute: '0', canEdit: '0' }))
      listChainPermission(row.chainName).then(res => {
        const map = {}
        ;(res.data || []).forEach(item => {
          map[item.roleId] = item
        })
        this.permissionRows = this.allRoles.map(r => {
          const hit = map[r.roleId]
          return {
            ...r,
            canExecute: hit && hit.canExecute === '1' ? '1' : '0',
            canEdit: hit && hit.canEdit === '1' ? '1' : '0'
          }
        })
      }).finally(() => {
        this.permissionLoading = false
      })
    },
    submitPermission() {
      const permissions = this.permissionRows
        .filter(row => row.canExecute === '1' || row.canEdit === '1')
        .map(row => ({
          roleId: row.roleId,
          canExecute: row.canExecute,
          canEdit: row.canEdit
        }))
      saveChainPermission({
        chainName: this.permissionChainName,
        permissions
      }).then(() => {
        this.$modal.msgSuccess('权限已保存')
        this.permissionOpen = false
      })
    }
  }
}
</script>

<style scoped>
.execute-result {
  margin-top: 12px;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 260px;
  overflow: auto;
  font-size: 12px;
}
.perm-chain-name {
  margin: 0 0 12px;
  font-size: 14px;
}
.perm-tip {
  margin: 12px 0 0;
  color: #909399;
  font-size: 12px;
}
</style>

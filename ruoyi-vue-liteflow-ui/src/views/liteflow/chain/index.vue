<template>
  <div class="app-container">
    <el-alert
      v-if="liteflowReadonly"
      :title="liteflowReadonlyMessage || '当前环境为只读模式，禁止修改规则'"
      type="info"
      show-icon
      :closable="false"
      class="mb8"
    />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" :disabled="liteflowReadonly" @click="handleAdd" v-hasPermi="['liteflow:chain:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-document-copy" size="mini" :disabled="liteflowReadonly" @click="openTemplateDialog" v-hasPermi="['liteflow:chain:add']">从模板创建</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-upload2" size="mini" :disabled="liteflowReadonly" @click="openImportDialog" v-hasPermi="['liteflow:chain:add']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single || liteflowReadonly" @click="handleUpdate" v-hasPermi="['liteflow:chain:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple || liteflowReadonly" @click="handleDelete" v-hasPermi="['liteflow:chain:remove']">删除</el-button>
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="268" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-s-operation" @click="goEditor(scope.row)" v-hasPermi="['liteflow:editor:view']">编排</el-button>
          <el-button size="mini" type="text" icon="el-icon-video-play" @click="handleExecute(scope.row)" v-hasPermi="['liteflow:execute']">试跑</el-button>
          <el-button size="mini" type="text" icon="el-icon-finished" @click="openCaseDialog(scope.row)" v-hasPermi="['liteflow:chain:query']">用例</el-button>
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
              <el-dropdown-item command="asTool" icon="el-icon-connection" v-hasPermi="['liteflow:execute']">设为工具</el-dropdown-item>
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
        <el-form-item label="Webhook" prop="webhookUrl">
          <el-input v-model="form.webhookUrl" placeholder="可选，执行完成回调 URL（优先于全局配置）" />
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

    <el-dialog title="链路试跑" :visible.sync="executeOpen" width="880px" append-to-body custom-class="chain-execute-dialog" @close="onExecuteDialogClose">
      <el-form label-width="100px">
        <el-form-item label="链路ID">
          <el-input v-model="executeChainName" disabled />
        </el-form-item>
        <el-form-item label="请求 JSON">
          <el-input v-model="executeParamJson" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="流式输出">
          <el-switch v-model="executeStreamMode" active-text="SSE（Agent 推理过程）" inactive-text="同步" />
        </el-form-item>
      </el-form>
      <div v-if="executeStreamLog" class="stream-log">{{ executeStreamLog }}</div>
      <el-alert
        v-if="executeResult"
        :title="executeResult.success ? '执行成功' : '执行失败'"
        :type="executeResult.success ? 'success' : 'error'"
        show-icon
        :closable="false"
      />

      <div v-if="executeInsight" class="exec-insight">
        <div v-if="executeInsight.steps.length" class="exec-steps">
          <span
            v-for="(step, idx) in executeInsight.steps"
            :key="'step-' + idx"
            class="exec-step"
          >
            <span class="exec-step__label">{{ step }}</span>
            <i v-if="idx < executeInsight.steps.length - 1" class="el-icon-right exec-step__arrow" />
          </span>
        </div>

        <div v-if="executeInsight.question" class="insight-card insight-card--question">
          <div class="insight-card__title">
            <i class="el-icon-chat-dot-round" /> 用户问题
          </div>
          <div class="insight-card__body">{{ executeInsight.question }}</div>
        </div>

        <div v-if="executeInsight.riskLevel" class="insight-card insight-card--risk">
          <div class="insight-card__title">
            <i class="el-icon-warning-outline" /> 风险结论
            <el-tag size="mini" :type="riskLevelType(executeInsight.riskLevel)" effect="dark" class="insight-card__tag">
              {{ executeInsight.riskLevel }}
            </el-tag>
          </div>
          <div v-if="executeInsight.agentReply" class="insight-card__body insight-card__body--pre">{{ executeInsight.agentReply }}</div>
        </div>

        <div v-if="executeInsight.answer" class="insight-card insight-card--answer">
          <div class="insight-card__title">
            <i class="el-icon-s-opportunity" /> 生成回答
            <el-tag v-if="executeInsight.hitCount != null" size="mini" type="success" class="insight-card__tag">
              命中 {{ executeInsight.hitCount }} 段
            </el-tag>
          </div>
          <div class="insight-card__body insight-card__body--pre">{{ executeInsight.answer }}</div>
        </div>

        <div v-if="executeInsight.graphSteps.length" class="insight-card insight-card--graph">
          <div class="insight-card__title">
            <i class="el-icon-share" /> LangGraph 轨迹
          </div>
          <div class="graph-trace">
            <div
              v-for="(g, idx) in executeInsight.graphSteps"
              :key="'g-' + idx"
              class="graph-trace__item"
            >
              <div class="graph-trace__node">{{ g.node }}</div>
              <div v-if="g.detail" class="graph-trace__detail">{{ g.detail }}</div>
            </div>
          </div>
        </div>

        <div v-if="executeInsight.ragHits.length" class="insight-card insight-card--rag">
          <div class="insight-card__title">
            <i class="el-icon-document" /> 检索片段 retrievedContext
          </div>
          <div
            v-for="(hit, idx) in executeInsight.ragHits"
            :key="'hit-' + idx"
            class="rag-hit"
          >
            <div class="rag-hit__head">
              <el-tag size="mini" effect="plain">{{ hit.source }}</el-tag>
              <span class="rag-hit__score">score {{ hit.score.toFixed(3) }}</span>
              <el-progress
                :percentage="Math.round(Math.min(hit.score, 1) * 100)"
                :stroke-width="8"
                :show-text="false"
                class="rag-hit__bar"
              />
            </div>
            <pre class="rag-hit__text">{{ hit.text }}</pre>
          </div>
        </div>
      </div>

      <el-collapse v-if="executeResult" v-model="executeJsonActive" class="exec-json-collapse">
        <el-collapse-item title="完整 JSON 结果" name="json">
          <pre class="execute-result">{{ formatResult(executeResult) }}</pre>
        </el-collapse-item>
      </el-collapse>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="executeLoading" @click="submitExecute">执 行</el-button>
        <el-button :disabled="liteflowReadonly" v-hasPermi="['liteflow:chain:edit']" @click="saveExecuteAsCase">保存为用例</el-button>
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

    <el-dialog :title="'试跑用例 — ' + caseChainName" :visible.sync="caseOpen" width="980px" append-to-body>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        回归使用当前库中的 EL（含草稿），不要求已发布。发布时可选择先跑启用中的用例。
      </el-alert>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" :disabled="liteflowReadonly" @click="openCaseForm()" v-hasPermi="['liteflow:chain:edit']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="el-icon-video-play" size="mini" :loading="caseRunLoading" @click="handleRunAllCases" v-hasPermi="['liteflow:execute']">全部回归</el-button>
        </el-col>
      </el-row>
      <el-table v-loading="caseLoading" :data="caseList" size="small" border>
        <el-table-column label="用例" prop="caseName" min-width="120" :show-overflow-tooltip="true" />
        <el-table-column label="期望" width="70" align="center">
          <template slot-scope="scope">
            {{ scope.row.expectSuccess === '0' ? '失败' : '成功' }}
          </template>
        </el-table-column>
        <el-table-column label="步骤包含" prop="expectStepContains" min-width="110" :show-overflow-tooltip="true" />
        <el-table-column label="状态" width="70" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近回归" width="80" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.lastRunSuccess === '1'" type="success" size="mini">通过</el-tag>
            <el-tag v-else-if="scope.row.lastRunSuccess === '0'" type="danger" size="mini">失败</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="最近时间" prop="lastRunTime" width="160" :show-overflow-tooltip="true" />
        <el-table-column label="说明" prop="lastRunMessage" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="text" @click="handleRunCase(scope.row)" v-hasPermi="['liteflow:execute']">跑</el-button>
            <el-button size="mini" type="text" :disabled="liteflowReadonly" @click="openCaseForm(scope.row)" v-hasPermi="['liteflow:chain:edit']">改</el-button>
            <el-button size="mini" type="text" :disabled="liteflowReadonly" @click="handleDeleteCase(scope.row)" v-hasPermi="['liteflow:chain:edit']">删</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="caseFormTitle" :visible.sync="caseFormOpen" width="640px" append-to-body>
      <el-form ref="caseForm" :model="caseForm" :rules="caseFormRules" label-width="110px">
        <el-form-item label="用例名称" prop="caseName">
          <el-input v-model="caseForm.caseName" placeholder="如 入门问候" />
        </el-form-item>
        <el-form-item label="请求 JSON" prop="paramJson">
          <el-input v-model="caseForm.paramJson" type="textarea" :rows="8" placeholder="{}" />
        </el-form-item>
        <el-form-item label="期望结果" prop="expectSuccess">
          <el-radio-group v-model="caseForm.expectSuccess">
            <el-radio label="1">成功</el-radio>
            <el-radio label="0">失败</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="步骤应包含" prop="expectStepContains">
          <el-input v-model="caseForm.expectStepContains" placeholder="可选，如 helloA" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="caseForm.sortOrder" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="caseForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="caseForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCaseForm">确 定</el-button>
        <el-button @click="caseFormOpen = false">取 消</el-button>
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

    <el-dialog title="设为智能体工具" :visible.sync="asToolOpen" width="560px" append-to-body>
      <el-form label-width="110px" size="small">
        <el-form-item label="链路">
          <span>{{ asToolForm.chainName }}</span>
          <el-tag v-if="asToolForm.published === false" type="warning" size="mini" style="margin-left:8px">未发布</el-tag>
          <el-tag v-else-if="asToolForm.exposed" type="success" size="mini" style="margin-left:8px">已暴露</el-tag>
        </el-form-item>
        <el-form-item label="工具编码">
          <el-input :value="asToolForm.toolCode" disabled />
        </el-form-item>
        <el-form-item label="同步开放 MCP">
          <el-checkbox v-model="asToolForm.exposeMcp" :disabled="asToolForm.agentChain">MCP :8090 可调用</el-checkbox>
          <p class="perm-tip">含 Agent 的链路默认不进开放 MCP。未启动 MCP 时，后台助手仍走本地执行。</p>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :disabled="asToolForm.published === false" @click="submitAsTool" v-hasPermi="['liteflow:execute']">暴露</el-button>
        <el-button type="danger" plain :disabled="!asToolForm.exposed" @click="removeAsTool" v-hasPermi="['liteflow:execute']">取消</el-button>
        <el-button @click="asToolOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { listChain, getChain, delChain, addChain, updateChain, reloadChain, executeChain, executeChainStream, executeRoute, publishChain, cloneChain, exportChain, importChain, listChainPermission, saveChainPermission, listChainCase, addChainCase, updateChainCase, delChainCase, runChainCase, runAllChainCases, getChainAsTool, exposeChainAsTool, unexposeChainAsTool } from '@/api/liteflow/chain'
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
      executeStreamMode: false,
      executeStreamLog: '',
      executeLoading: false,
      executeJsonActive: [],
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
      asToolOpen: false,
      asToolChainId: null,
      asToolForm: {
        chainName: '',
        toolCode: '',
        published: true,
        exposed: false,
        agentChain: false,
        exposeMcp: false
      },
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
      },
      caseOpen: false,
      caseLoading: false,
      caseRunLoading: false,
      caseChainName: '',
      caseChainRow: null,
      caseList: [],
      caseFormOpen: false,
      caseFormTitle: '新增用例',
      caseForm: {},
      caseFormRules: {
        caseName: [{ required: true, message: '用例名称不能为空', trigger: 'blur' }],
        paramJson: [{ required: true, message: '请求 JSON 不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    ...mapGetters(['liteflowReadonly', 'liteflowReadonlyMessage']),
    executeInsight() {
      return this.buildExecuteInsight(this.executeResult)
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
        webhookUrl: undefined,
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
        asTool: () => this.openAsToolDialog(row),
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
      this.executeStreamLog = ''
      this.executeJsonActive = []
      this.executeStreamMode = row.chainName === 'agentRiskDemo' || (row.elData || '').toLowerCase().includes('agent')
      this.executeOpen = true
    },
    buildExecuteInsight(result) {
      if (!result || !result.success) {
        return null
      }
      const ctx = result.contextData || {}
      const steps = this.parseExecuteSteps(result.executeStepStrWithTime || result.executeStepStr)
      const graphSteps = this.parseGraphTrace(ctx.graphTrace)
      const ragHits = this.parseRetrievedContext(ctx.retrievedContext)
      const insight = {
        steps,
        question: ctx.question || '',
        answer: ctx.answer || '',
        riskLevel: ctx.riskLevel || '',
        agentReply: ctx.agentReply || '',
        hitCount: ctx.hitCount != null ? ctx.hitCount : null,
        graphSteps,
        ragHits
      }
      const hasHighlight = !!(insight.question || insight.answer || insight.riskLevel
        || insight.graphSteps.length || insight.ragHits.length || insight.steps.length)
      return hasHighlight ? insight : null
    },
    parseExecuteSteps(stepStr) {
      if (!stepStr) {
        return []
      }
      return String(stepStr).split('==>').map(s => s.trim()).filter(Boolean)
    },
    parseGraphTrace(trace) {
      if (!trace) {
        return []
      }
      return String(trace).split('|').map(s => s.trim()).filter(Boolean).map(s => {
        const i = s.indexOf(':')
        if (i < 0) {
          return { node: s, detail: '' }
        }
        return { node: s.slice(0, i).trim(), detail: s.slice(i + 1).trim() }
      })
    },
    parseRetrievedContext(text) {
      if (!text) {
        return []
      }
      const blocks = String(text).split(/\n(?=- source=)/)
      return blocks.map(block => {
        const m = block.match(/^- source=([^,\n]+),\s*score=([0-9.]+)\s*\n?([\s\S]*)$/)
        if (!m) {
          return null
        }
        return {
          source: m[1].trim(),
          score: Number(m[2]),
          text: (m[3] || '').trim()
        }
      }).filter(Boolean)
    },
    riskLevelType(level) {
      const u = String(level || '').toUpperCase()
      if (u === 'HIGH') return 'danger'
      if (u === 'MEDIUM') return 'warning'
      if (u === 'LOW') return 'success'
      return 'info'
    },
    onExecuteDialogClose() {
      this.executeLoading = false
    },
    appendStreamLine(line) {
      this.executeStreamLog = (this.executeStreamLog || '') + line + '\n'
    },
    submitExecute() {
      let param = {}
      try {
        param = JSON.parse(this.executeParamJson || '{}')
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      this.executeResult = null
      this.executeStreamLog = ''
      this.executeJsonActive = []
      this.executeLoading = true
      if (!this.executeStreamMode) {
        executeChain(this.executeChainName, param).then(response => {
          this.executeResult = response.data
          // AI / RAG / Graph 试跑默认收起 JSON，优先看洞察卡片
          this.executeJsonActive = []
        }).finally(() => {
          this.executeLoading = false
        })
        return
      }
      executeChainStream(this.executeChainName, param, {
        onEvent: ({ type, payload }) => {
          const text = payload && payload.text ? payload.text : ''
          const node = payload && payload.nodeId ? '[' + payload.nodeId + '] ' : ''
          if (type === 'agent.reasoning' && text) {
            this.appendStreamLine(node + text)
          } else if (type === 'agent.tool_result') {
            this.appendStreamLine(node + '[tool] ' + (text || JSON.stringify(payload.data || {})))
          } else if (type === 'agent.result' && text) {
            this.appendStreamLine(node + '[result] ' + text)
          } else if (text) {
            this.appendStreamLine('[' + type + '] ' + text)
          }
        },
        onDone: (result) => {
          this.executeResult = result
          this.executeJsonActive = []
          this.executeLoading = false
        },
        onError: (err) => {
          this.executeLoading = false
          this.appendStreamLine('[error] ' + (err && err.message ? err.message : JSON.stringify(err)))
          this.$modal.msgError((err && err.message) || '流式执行失败')
        }
      }).catch(() => {
        this.executeLoading = false
      })
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
      this.$confirm('发布后将热刷新生效。是否先跑启用中的用例？失败则中止发布。', '发布链路', {
        distinguishCancelAndClose: true,
        confirmButtonText: '先跑用例再发布',
        cancelButtonText: '直接发布',
        type: 'warning'
      }).then(() => {
        this.publishWithCases(row)
      }).catch(action => {
        if (action === 'cancel') {
          this.doPublish(row)
        }
      })
    },
    publishWithCases(row) {
      this.$modal.loading('正在回归用例...')
      runAllChainCases(row.chainName).then(res => {
        this.$modal.closeLoading()
        const data = res.data || {}
        if (!data.total) {
          this.$modal.msg('没有启用中的用例，直接发布')
          return this.doPublish(row)
        }
        if (data.failed > 0) {
          this.$modal.msgError('回归未通过 ' + data.failed + '/' + data.total + '，已中止发布')
          this.openCaseDialog(row)
          return
        }
        this.$modal.msgSuccess('用例全部通过（' + data.passed + '）')
        return this.doPublish(row)
      }).catch(() => {
        this.$modal.closeLoading()
      })
    },
    doPublish(row) {
      return publishChain(row.id).then(() => {
        this.getList()
        this.$modal.msgSuccess('发布成功')
      })
    },
    openCaseDialog(row) {
      this.caseChainRow = row
      this.caseChainName = row.chainName
      this.caseOpen = true
      this.loadCaseList()
    },
    loadCaseList() {
      this.caseLoading = true
      listChainCase({ chainName: this.caseChainName, pageNum: 1, pageSize: 50 }).then(res => {
        this.caseList = res.rows || []
      }).finally(() => {
        this.caseLoading = false
      })
    },
    resetCaseForm() {
      this.caseForm = {
        id: undefined,
        chainName: this.caseChainName,
        caseName: undefined,
        paramJson: JSON.stringify(getDefaultExecuteParam(this.caseChainName), null, 2),
        expectSuccess: '1',
        expectStepContains: undefined,
        sortOrder: 0,
        status: '0',
        remark: undefined
      }
    },
    openCaseForm(row) {
      if (row && row.id) {
        this.caseFormTitle = '修改用例'
        this.caseForm = {
          id: row.id,
          chainName: row.chainName,
          caseName: row.caseName,
          paramJson: this.prettyJson(row.paramJson),
          expectSuccess: row.expectSuccess || '1',
          expectStepContains: row.expectStepContains,
          sortOrder: row.sortOrder == null ? 0 : row.sortOrder,
          status: row.status || '0',
          remark: row.remark
        }
      } else {
        this.caseFormTitle = '新增用例'
        this.resetCaseForm()
      }
      this.caseFormOpen = true
      this.$nextTick(() => this.resetForm('caseForm'))
    },
    prettyJson(text) {
      try {
        return JSON.stringify(JSON.parse(text || '{}'), null, 2)
      } catch (e) {
        return text || '{}'
      }
    },
    submitCaseForm() {
      this.$refs.caseForm.validate(valid => {
        if (!valid) {
          return
        }
        try {
          JSON.parse(this.caseForm.paramJson || '{}')
        } catch (e) {
          this.$modal.msgError('请求 JSON 格式不正确')
          return
        }
        const req = this.caseForm.id ? updateChainCase : addChainCase
        req(this.caseForm).then(() => {
          this.$modal.msgSuccess(this.caseForm.id ? '修改成功' : '新增成功')
          this.caseFormOpen = false
          this.loadCaseList()
        })
      })
    },
    handleRunCase(row) {
      this.caseRunLoading = true
      runChainCase(row.id).then(res => {
        const data = res.data || {}
        if (data.passed) {
          this.$modal.msgSuccess((data.caseName || '用例') + ' 通过')
        } else {
          this.$modal.msgError((data.caseName || '用例') + ' 未通过：' + (data.message || ''))
        }
        this.loadCaseList()
      }).finally(() => {
        this.caseRunLoading = false
      })
    },
    handleRunAllCases() {
      this.caseRunLoading = true
      runAllChainCases(this.caseChainName).then(res => {
        const data = res.data || {}
        if (!data.total) {
          this.$modal.msg('没有启用中的用例')
        } else if (data.failed > 0) {
          this.$modal.msgError('回归未通过 ' + data.failed + '/' + data.total)
        } else {
          this.$modal.msgSuccess('全部通过（' + data.passed + '）')
        }
        this.loadCaseList()
      }).finally(() => {
        this.caseRunLoading = false
      })
    },
    handleDeleteCase(row) {
      this.$modal.confirm('是否删除用例「' + row.caseName + '」？').then(() => {
        return delChainCase(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadCaseList()
      }).catch(() => {})
    },
    saveExecuteAsCase() {
      let paramJson = this.executeParamJson || '{}'
      try {
        JSON.parse(paramJson)
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      this.$prompt('用例名称', '保存为用例', {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: this.executeChainName + ' 试跑',
        inputPattern: /\S+/,
        inputErrorMessage: '用例名称不能为空'
      }).then(({ value }) => {
        return addChainCase({
          chainName: this.executeChainName,
          caseName: value,
          paramJson: paramJson,
          expectSuccess: this.executeResult && this.executeResult.success === false ? '0' : '1',
          status: '0',
          sortOrder: 0
        })
      }).then(() => {
        this.$modal.msgSuccess('已保存为用例')
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
    },
    openAsToolDialog(row) {
      this.asToolChainId = row.id
      this.asToolForm = {
        chainName: row.chainName,
        toolCode: 'lf_' + row.chainName,
        published: row.draftFlag !== '1' && row.status !== '1',
        exposed: false,
        agentChain: false,
        exposeMcp: false
      }
      this.asToolOpen = true
      getChainAsTool(row.id).then(res => {
        const data = res.data || {}
        this.asToolForm = {
          chainName: data.chainName || row.chainName,
          toolCode: data.toolCode || ('lf_' + row.chainName),
          published: data.published !== false,
          exposed: !!data.exposed,
          agentChain: !!data.agentChain,
          exposeMcp: !!data.exposeMcp && !data.agentChain
        }
      })
    },
    submitAsTool() {
      exposeChainAsTool(this.asToolChainId, { exposeMcp: !!this.asToolForm.exposeMcp }).then(() => {
        this.$modal.msgSuccess('已设为智能体工具，可在「AI能力 → 工具 / 智能体」绑定')
        getChainAsTool(this.asToolChainId).then(res => {
          const data = res.data || {}
          this.asToolForm = {
            chainName: data.chainName || this.asToolForm.chainName,
            toolCode: data.toolCode || this.asToolForm.toolCode,
            published: data.published !== false,
            exposed: !!data.exposed,
            agentChain: !!data.agentChain,
            exposeMcp: !!data.exposeMcp && !data.agentChain
          }
        })
      })
    },
    removeAsTool() {
      this.$modal.confirm('取消后智能体将无法再调用该链路工具，是否继续？').then(() => {
        return unexposeChainAsTool(this.asToolChainId)
      }).then(() => {
        this.$modal.msgSuccess('已取消')
        this.asToolForm.exposed = false
        this.asToolForm.exposeMcp = false
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.execute-result {
  margin: 0;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 280px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
}
.stream-log {
  margin: 8px 0 12px;
  max-height: 180px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  background: #1e293b;
  color: #e2e8f0;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  font-family: Consolas, Monaco, monospace;
}
.exec-insight {
  margin-top: 14px;
}
.exec-steps {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 2px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.exec-step {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
}
.exec-step__label {
  display: inline-block;
  padding: 3px 10px;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  color: #334155;
  font-size: 12px;
  line-height: 1.4;
  white-space: nowrap;
}
.exec-step__arrow {
  margin: 0 4px;
  color: #94a3b8;
  font-size: 12px;
}
.insight-card {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-left: 4px solid #64748b;
}
.insight-card--question {
  border-left-color: #0ea5e9;
  background: #f0f9ff;
}
.insight-card--answer {
  border-left-color: #059669;
  background: #f0fdf4;
}
.insight-card--risk {
  border-left-color: #d97706;
  background: #fffbeb;
}
.insight-card--graph {
  border-left-color: #2563eb;
  background: #eff6ff;
}
.insight-card--rag {
  border-left-color: #0d9488;
  background: #f0fdfa;
}
.insight-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}
.insight-card__tag {
  margin-left: 2px;
}
.insight-card__body {
  font-size: 13px;
  color: #334155;
  line-height: 1.65;
}
.insight-card__body--pre {
  white-space: pre-wrap;
  word-break: break-word;
}
.graph-trace {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.graph-trace__item {
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #bfdbfe;
  border-radius: 6px;
}
.graph-trace__node {
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
  letter-spacing: 0.02em;
}
.graph-trace__detail {
  margin-top: 4px;
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
  word-break: break-word;
}
.rag-hit {
  margin-top: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #99f6e4;
  border-radius: 6px;
}
.rag-hit:first-of-type {
  margin-top: 4px;
}
.rag-hit__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.rag-hit__score {
  flex: 0 0 auto;
  font-size: 12px;
  color: #0f766e;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
.rag-hit__bar {
  flex: 1;
  max-width: 140px;
}
.rag-hit__text {
  margin: 0;
  max-height: 140px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.55;
  color: #334155;
  font-family: Consolas, Monaco, "PingFang SC", "Microsoft YaHei", monospace;
}
.exec-json-collapse {
  margin-top: 8px;
  border: none;
}
.exec-json-collapse ::v-deep .el-collapse-item__header {
  height: 40px;
  line-height: 40px;
  font-size: 13px;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 0 12px;
}
.exec-json-collapse ::v-deep .el-collapse-item__wrap {
  border: none;
  background: transparent;
}
.exec-json-collapse ::v-deep .el-collapse-item__content {
  padding: 10px 0 0;
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

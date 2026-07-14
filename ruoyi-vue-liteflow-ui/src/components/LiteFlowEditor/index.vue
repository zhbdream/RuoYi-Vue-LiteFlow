<template>
  <div class="lf-editor">
    <div class="lf-editor-toolbar">
      <span class="chain-title">{{ chainName || '未选择链路' }}</span>
      <div class="toolbar-center">
        <el-radio-group v-model="editMode" size="mini" @change="onEditModeChange">
          <el-radio-button label="visual">可视化</el-radio-button>
          <el-radio-button label="el">EL 文本</el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-actions">
        <el-button size="mini" icon="el-icon-back" @click="$emit('back')">返回</el-button>
        <el-button size="mini" icon="el-icon-video-play" @click="openTestRun" v-hasPermi="['liteflow:execute']">试跑</el-button>
        <el-button size="mini" icon="el-icon-cpu" @click="openElDebug" v-hasPermi="['liteflow:execute']">EL试跑</el-button>
        <el-button size="mini" :disabled="!canUndo || readonly" @click="undo">撤销</el-button>
        <el-button size="mini" :disabled="!canRedo || readonly" @click="redo">重做</el-button>
        <el-button size="mini" icon="el-icon-rank" :disabled="!flowModel" @click="autoLayout">自动布局</el-button>
        <el-button size="mini" icon="el-icon-document-copy" @click="copyEl">复制 EL</el-button>
        <el-button size="mini" @click="handleValidate">校验 EL</el-button>
        <el-button size="mini" icon="el-icon-delete" :disabled="readonly" @click="clearCanvas">清空</el-button>
        <el-button size="mini" icon="el-icon-refresh" :disabled="readonly" @click="$emit('reload')" v-hasPermi="['liteflow:chain:reload']">热刷新</el-button>
        <el-button type="primary" size="mini" icon="el-icon-check" :disabled="readonly" @click="handleSave" v-hasPermi="['liteflow:editor:save', 'liteflow:chain:edit']">保存</el-button>
      </div>
    </div>

    <el-alert v-if="readonly" :title="readonlyMessage || '当前环境为只读模式，禁止保存与热刷新'" type="info" show-icon :closable="false" class="lf-editor-alert" />
    <el-alert v-if="elWarning" :title="elWarning" type="warning" show-icon :closable="false" class="lf-editor-alert" />

    <el-alert v-if="validateResult" :title="validateResult" :type="validateResultType" show-icon :closable="true" @close="validateResult=''" class="lf-editor-alert" />

    <div v-show="editMode === 'visual'" class="lf-editor-body">
      <div class="panel-left">
        <el-tabs v-model="leftPanelTab" class="left-panel-tabs">
          <el-tab-pane label="组件" name="component">
            <div class="panel-search">
              <el-input
                v-model="componentKeyword"
                size="mini"
                clearable
                prefix-icon="el-icon-search"
                placeholder="搜索 nodeId / 名称"
              />
            </div>
            <div v-loading="componentLoading" class="component-groups">
              <el-empty v-if="!filteredComponentGroups.length" description="无匹配组件" :image-size="48" />
              <el-collapse v-else v-model="expandedGroups">
                <el-collapse-item
                  v-for="group in filteredComponentGroups"
                  :key="group.type"
                  :name="group.type"
                >
                  <template slot="title">
                    <span class="group-title">
                      <i :class="groupIcon(group.type)" :style="{ color: groupColor(group.type) }"></i>
                      {{ group.label }}
                      <el-badge :value="group.items.length" type="info" class="group-badge" />
                    </span>
                  </template>
                  <div
                    v-for="item in group.items"
                    :key="item.nodeId"
                    class="component-item"
                    draggable="true"
                    @dragstart="onDragStart($event, item)"
                    @click="appendComponent(item)"
                  >
                    <div class="component-item-head">
                      <span class="node-id">{{ item.nodeId }}</span>
                      <el-tag size="mini" :type="groupTagType(group.type)" effect="plain">{{ groupShortLabel(group.type) }}</el-tag>
                    </div>
                    <span class="node-name">{{ item.name }}</span>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-tab-pane>
          <el-tab-pane label="子链路" name="subchain">
            <div class="panel-search">
              <el-input
                v-model="chainKeyword"
                size="mini"
                clearable
                prefix-icon="el-icon-search"
                placeholder="搜索链路 ID / 描述"
              />
            </div>
            <div class="component-groups subchain-list">
              <el-empty v-if="!filteredChainOptions.length" description="无可用子链路" :image-size="48" />
              <div
                v-for="item in filteredChainOptions"
                :key="item.chainName"
                class="component-item subchain-item"
                draggable="true"
                @dragstart="onSubChainDragStart($event, item)"
                @click="appendSubChain(item)"
              >
                <div class="component-item-head">
                  <span class="node-id">{{ item.chainName }}</span>
                  <el-tag size="mini" type="info" effect="plain">子链路</el-tag>
                </div>
                <span class="node-name">{{ item.chainDesc || item.chainName }}</span>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="逻辑" name="logic">
            <div class="logic-panel">
              <p class="logic-desc">点击添加到链路末尾，复杂分支在右侧属性面板配置。</p>
              <div class="logic-grid">
                <div
                  v-for="item in logicNodeDefs"
                  :key="item.key"
                  class="logic-card"
                  :style="{ borderColor: item.color }"
                  @click="item.handler()"
                >
                  <i :class="item.icon" :style="{ color: item.color }"></i>
                  <span class="logic-label">{{ item.label }}</span>
                  <span class="logic-tip">{{ item.tip }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
        <p class="panel-tip">拖拽/点击追加；右键更多操作；Ctrl+C/X/V 复制剪切粘贴；Delete 删除；Ctrl+D 复制到末尾。</p>
      </div>

      <div class="graph-wrap" ref="graphWrap" @click="hideContextMenu">
        <div ref="graphContainer" class="graph-container"></div>
        <div ref="minimapContainer" class="lf-minimap"></div>
        <ul
          v-show="contextMenu.visible"
          class="graph-context-menu"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
          @click.stop
        >
          <li
            v-for="item in contextMenuItems"
            :key="item.key"
            :class="{ disabled: item.disabled, divider: item.divider, danger: item.danger }"
            @click="handleContextAction(item)"
          >
            <template v-if="!item.divider">
              <i :class="item.icon"></i> {{ item.label }}
            </template>
          </li>
        </ul>
      </div>

      <div class="panel-right">
        <div class="panel-title">EL 预览</div>
        <el-input :value="elPreview" type="textarea" :rows="8" readonly />
        <div class="panel-title" style="margin-top: 8px">节点属性</div>
        <div v-if="selectedModelNode" class="prop-form">
          <el-form label-width="72px" size="mini">
            <el-form-item label="类型">
              <el-tag size="mini">{{ selectedModelNode.lfType }}</el-tag>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'subchain'" label="链路ID">
              <el-input v-model="selectedModelNode.chainName" disabled />
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'component'" label="节点ID">
              <el-input v-model="selectedModelNode.nodeId" disabled />
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'if'" label="条件">
              <el-select v-model="selectedModelNode.condition" filterable placeholder="布尔组件" @change="onPropChange">
                <el-option v-for="c in booleanComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'if'" label="真分支">
              <el-select v-model="selectedModelNode.trueNodeId" filterable placeholder="组件" @change="onIfBranchChange('true')">
                <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'if'" label="假分支">
              <el-select v-model="selectedModelNode.falseNodeId" filterable placeholder="组件" @change="onIfBranchChange('false')">
                <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'switch'" label="路由">
              <el-select v-model="selectedModelNode.condition" filterable placeholder="选择组件" @change="onPropChange">
                <el-option v-for="c in switchComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'switch'" label="分支">
              <div v-for="(bid, idx) in selectedModelNode.branchNodeIds" :key="idx" class="branch-row">
                <el-select v-model="selectedModelNode.branchNodeIds[idx]" filterable size="mini" @change="onSwitchBranchChange">
                  <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
                </el-select>
              </div>
              <el-button type="text" size="mini" @click="addSwitchBranch">+ 添加分支</el-button>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'component'" label="tag">
              <el-input v-model="selectedModelNode.tag" placeholder="可选" @change="onComponentMetaChange" />
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'component'" label="data">
              <el-input v-model="selectedModelNode.dataKey" placeholder="key" style="width:45%;margin-right:4%" @change="onComponentMetaChange" />
              <el-input v-model="selectedModelNode.dataValue" placeholder="value" style="width:50%" @change="onComponentMetaChange" />
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'component'" label="bind">
              <el-input v-model="selectedModelNode.bind" placeholder="可选" @change="onComponentMetaChange" />
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'when'" label="超时(s)">
              <el-input-number v-model="selectedModelNode.maxWaitSeconds" :min="0" :max="300" @change="onWhenChange" />
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'when'" label="并行项">
              <div v-for="(cid, idx) in selectedModelNode.childNodeIds" :key="idx" class="branch-row">
                <el-select v-model="selectedModelNode.childNodeIds[idx]" filterable size="mini" @change="onWhenChange">
                  <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
                </el-select>
              </div>
              <el-button type="text" size="mini" @click="addWhenChild">+ 添加并行项</el-button>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'for'" label="循环组件">
              <el-select v-model="selectedModelNode.loopComponent" filterable @change="onForChange">
                <el-option v-for="c in forComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'for'" label="循环体">
              <el-select v-model="selectedModelNode.bodyNodeId" filterable @change="onForChange">
                <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'catch'" label="try">
              <el-select v-model="selectedModelNode.tryNodeId" filterable @change="onCatchChange">
                <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedModelNode.lfType === 'catch'" label="fallback">
              <el-select v-model="selectedModelNode.fallbackNodeId" filterable @change="onCatchChange">
                <el-option v-for="c in commonComponents" :key="c.nodeId" :label="formatComponentLabel(c)" :value="c.nodeId" />
              </el-select>
            </el-form-item>
            <el-form-item label="名称">
              <el-input v-model="selectedModelNode.name" @change="onPropChange" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="selectedModelNode.remark" type="textarea" :rows="2" @change="onPropChange" />
            </el-form-item>
          </el-form>
        </div>
        <el-empty v-else description="点击画布节点查看属性" :image-size="48" />
        <div v-if="executeResult" class="exec-result-box">
          <div class="panel-title">试跑结果</div>
          <el-tag :type="executeResult.success ? 'success' : 'danger'" size="mini">{{ executeResult.success ? '成功' : '失败' }}</el-tag>
          <p class="exec-steps">{{ executeResult.executeStepStr }}</p>
        </div>
      </div>
    </div>

    <div v-show="editMode === 'el'" class="el-text-mode">
      <el-input v-model="elTextDraft" type="textarea" :rows="22" :readonly="readonly" placeholder="直接编辑 EL，切换回可视化时将尝试解析" />
      <p class="panel-tip">高级模式：编辑完成后切回「可视化」或点保存（会先解析 EL）。</p>
    </div>

    <el-dialog :title="pickerDialogTitle" :visible.sync="pickerOpen" width="440px" append-to-body>
      <el-select v-model="pickerComponentId" filterable placeholder="搜索 nodeId / 名称" style="width:100%">
        <el-option
          v-for="c in pickerComponentOptions"
          :key="c.nodeId"
          :label="formatComponentLabel(c)"
          :value="c.nodeId"
        />
      </el-select>
      <p class="insert-dialog-tip">{{ pickerDialogTip }}</p>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmComponentPicker">确 定</el-button>
        <el-button @click="pickerOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="编排器试跑" :visible.sync="executeOpen" width="680px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="链路ID">
          <el-input :value="chainName" disabled />
        </el-form-item>
        <el-form-item label="请求 JSON">
          <el-input v-model="executeParamJson" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <el-alert v-if="executeResult" :title="executeResult.success ? '执行成功' : '执行失败'" :type="executeResult.success ? 'success' : 'error'" show-icon :closable="false" />
      <pre v-if="executeResult" class="execute-result-pre">{{ formatExecuteResult(executeResult) }}</pre>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitTestRun">执 行</el-button>
        <el-button @click="executeOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
    <el-dialog title="EL 在线调试" :visible.sync="elDebugOpen" width="760px" append-to-body>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px">
        使用 execute2RespWithEL 直接执行当前 EL，不入库、不依赖已发布链路。
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="EL 表达式">
          <el-input v-model="elDebugEl" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="上下文Class">
          <el-input v-model="elDebugContextClass" placeholder="可选" />
        </el-form-item>
        <el-form-item label="请求 JSON">
          <el-input v-model="elDebugParamJson" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <el-alert v-if="elDebugResult" :title="elDebugResult.success ? '执行成功' : '执行失败'" :type="elDebugResult.success ? 'success' : 'error'" show-icon :closable="false" />
      <pre v-if="elDebugResult" class="execute-result-pre">{{ formatExecuteResult(elDebugResult) }}</pre>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitElDebug">执 行</el-button>
        <el-button @click="elDebugOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Graph } from '@antv/x6'
import { MiniMap } from '@antv/x6-plugin-minimap'
import { NODE_TYPE_LABELS } from '@/utils/liteflow/elThen'
import {
  parseEl,
  buildEl,
  buildExpr,
  createComponentNode,
  createIfNode,
  createSwitchNode,
  createWhenNode,
  createForNode,
  createCatchNode,
  createSubChainNode
} from '@/utils/liteflow/elModel'
import { layoutFlowModel, enrichModelFromComponents } from '@/utils/liteflow/elLayout'
import { validateFlowModel } from '@/utils/liteflow/elValidate'
import { validateEl, executeChain, executeEl } from '@/api/liteflow/chain'
import { getDefaultExecuteParam } from '@/utils/liteflow/chainTemplates'

const GROUP_ORDER = ['common', 'agent', 'boolean', 'switch', 'for', 'iterator', 'declarative', 'unknown']

export default {
  name: 'LiteFlowEditor',
  props: {
    chainName: { type: String, default: '' },
    contextClass: { type: String, default: '' },
    chainOptions: { type: Array, default: () => [] },
    elData: { type: String, default: '' },
    graphJson: { type: String, default: '' },
    components: { type: Array, default: () => [] },
    componentLoading: { type: Boolean, default: false },
    logHighlight: { type: Object, default: null },
    readonly: { type: Boolean, default: false },
    readonlyMessage: { type: String, default: '' }
  },
  data() {
    return {
      graph: null,
      flowModel: null,
      elPreview: '',
      elWarning: '',
      dragNodeId: null,
      selectedModelKey: null,
      selectedModelNode: null,
      historyStack: [],
      historyIndex: -1,
      validateResult: '',
      validateResultType: 'success',
      editMode: 'visual',
      elTextDraft: '',
      executeOpen: false,
      executeParamJson: '{}',
      executeResult: null,
      leftPanelTab: 'component',
      componentKeyword: '',
      chainKeyword: '',
      expandedGroups: ['common', 'agent', 'boolean', 'switch'],
      contextMenu: { visible: false, x: 0, y: 0 },
      contextMenuType: 'node',
      nodeClipboard: null,
      pickerOpen: false,
      pickerComponentId: '',
      pickerMode: 'insertAfter',
      elDebugOpen: false,
      elDebugEl: '',
      elDebugContextClass: '',
      elDebugParamJson: '{}',
      elDebugResult: null
    }
  },
  computed: {
    filteredChainOptions() {
      const kw = (this.chainKeyword || '').trim().toLowerCase()
      return (this.chainOptions || []).filter(item => {
        if (item.chainName === this.chainName) {
          return false
        }
        if (!kw) {
          return true
        }
        return (item.chainName && item.chainName.toLowerCase().includes(kw))
          || (item.chainDesc && item.chainDesc.toLowerCase().includes(kw))
      })
    },
    logicNodeDefs() {
      return [
        { key: 'if', label: 'IF', tip: '条件分支', icon: 'el-icon-share', color: '#E6A23C', handler: () => this.addIfNode() },
        { key: 'switch', label: 'SWITCH', tip: '多路选择', icon: 'el-icon-s-operation', color: '#67C23A', handler: () => this.addSwitchNode() },
        { key: 'when', label: 'WHEN', tip: '并行执行', icon: 'el-icon-connection', color: '#909399', handler: () => this.addWhenNode() },
        { key: 'for', label: 'FOR', tip: '次数循环', icon: 'el-icon-refresh-right', color: '#9C27B0', handler: () => this.addForNode() },
        { key: 'catch', label: 'CATCH', tip: '异常捕获', icon: 'el-icon-warning-outline', color: '#F56C6C', handler: () => this.addCatchNode() }
      ]
    },
    componentGroups() {
      const map = {}
      this.components.forEach(item => {
        const type = item.nodeType || 'unknown'
        if (!map[type]) {
          map[type] = { type, label: NODE_TYPE_LABELS[type] || NODE_TYPE_LABELS.unknown, items: [] }
        }
        map[type].items.push(item)
      })
      return Object.values(map).sort((a, b) => {
        const ia = GROUP_ORDER.indexOf(a.type)
        const ib = GROUP_ORDER.indexOf(b.type)
        return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib)
      })
    },
    filteredComponentGroups() {
      const kw = (this.componentKeyword || '').trim().toLowerCase()
      if (!kw) {
        return this.componentGroups
      }
      return this.componentGroups
        .map(group => ({
          ...group,
          items: group.items.filter(item => {
            const id = (item.nodeId || '').toLowerCase()
            const name = (item.name || '').toLowerCase()
            return id.includes(kw) || name.includes(kw)
          })
        }))
        .filter(group => group.items.length > 0)
    },
    booleanComponents() {
      return this.components.filter(c => c.nodeType === 'boolean')
    },
    switchComponents() {
      return this.components.filter(c => c.nodeType === 'switch')
    },
    commonComponents() {
      return this.components.filter(c => c.nodeType === 'common')
    },
    forComponents() {
      return this.components.filter(c => c.nodeType === 'for')
    },
    canUndo() {
      return this.historyIndex > 0
    },
    canRedo() {
      return this.historyIndex >= 0 && this.historyIndex < this.historyStack.length - 1
    },
    selectedThenIndex() {
      if (!this.selectedModelKey || !this.flowModel || this.flowModel.type !== 'then') {
        return -1
      }
      return (this.flowModel.children || []).findIndex(c => c._key === this.selectedModelKey)
    },
    isTopLevelThenNode() {
      return this.selectedThenIndex >= 0
    },
    hasNodeClipboard() {
      return !!(this.nodeClipboard && this.nodeClipboard.node)
    },
    isSelectedComponent() {
      const node = this.getSelectedModelNode()
      return !!(node && node.type === 'component')
    },
    pickerDialogTitle() {
      if (this.pickerMode === 'replace') {
        return '替换组件'
      }
      return '插入组件'
    },
    pickerDialogTip() {
      if (this.pickerMode === 'replace') {
        return '将选中组件节点替换为其他组件（保留 tag/data 等属性）'
      }
      if (this.pickerMode === 'insertBefore') {
        return '在选中节点前插入（仅 THEN 顶层顺序有效）'
      }
      return '在选中节点后插入（仅 THEN 顶层顺序有效）'
    },
    pickerComponentOptions() {
      return this.commonComponents.length ? this.commonComponents : this.components
    },
    contextMenuItems() {
      if (this.contextMenuType === 'blank') {
        return [
          { key: 'pasteEnd', label: '粘贴到链路末尾', icon: 'el-icon-document', action: 'pasteAtEnd', disabled: !this.hasNodeClipboard }
        ]
      }
      const idx = this.selectedThenIndex
      const top = this.isTopLevelThenNode
      const len = this.flowModel && this.flowModel.children ? this.flowModel.children.length : 0
      const clip = this.hasNodeClipboard
      const isComp = this.isSelectedComponent
      return [
        { key: 'copyId', label: '复制 nodeId', icon: 'el-icon-document-copy', action: 'copyNodeId' },
        { key: 'copyEl', label: '复制 EL 片段', icon: 'el-icon-tickets', action: 'copyElSnippet' },
        { key: 'copyNode', label: '复制节点', icon: 'el-icon-copy-document', action: 'copyNode' },
        { key: 'cut', label: '剪切节点', icon: 'el-icon-scissors', action: 'cutNode' },
        { key: 'pasteAfter', label: '粘贴到后方', icon: 'el-icon-bottom', action: 'pasteAfter', disabled: !clip || !top },
        { key: 'pasteReplace', label: '粘贴替换', icon: 'el-icon-refresh-left', action: 'pasteReplace', disabled: !clip || !isComp },
        { key: 'pasteEnd', label: '粘贴到链路末尾', icon: 'el-icon-document', action: 'pasteAtEnd', disabled: !clip },
        { key: 'div0', divider: true },
        { key: 'dup', label: '复制到链路末尾', icon: 'el-icon-document-add', action: 'duplicateToEnd' },
        { key: 'div1', divider: true },
        { key: 'insertBefore', label: '在前插入组件…', icon: 'el-icon-top', action: 'insertBefore', disabled: !top },
        { key: 'insertAfter', label: '在后插入组件…', icon: 'el-icon-bottom', action: 'insertAfter', disabled: !top },
        { key: 'replace', label: '替换为组件…', icon: 'el-icon-refresh', action: 'replaceComponent', disabled: !isComp },
        { key: 'moveUp', label: '上移', icon: 'el-icon-arrow-up', action: 'moveUp', disabled: !top || idx <= 0 },
        { key: 'moveDown', label: '下移', icon: 'el-icon-arrow-down', action: 'moveDown', disabled: !top || idx < 0 || idx >= len - 1 },
        { key: 'div2', divider: true },
        { key: 'delete', label: '删除节点', icon: 'el-icon-delete', action: 'delete', danger: true }
      ]
    }
  },
  watch: {
    elData(val) {
      if (this.graph && val) {
        this.loadFromEl(val)
      }
    },
    componentKeyword(val) {
      if (val && val.trim()) {
        this.expandedGroups = this.filteredComponentGroups.map(g => g.type)
      }
    },
    logHighlight(val) {
      if (val && val.executeStepStr && this.graph) {
        this.$nextTick(() => this.applyLogHighlight())
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initGraph()
      this.loadFromData(this.elData, this.graphJson)
      this.bindDropZone()
    })
    document.addEventListener('keydown', this.onKeyDown)
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.onKeyDown)
    this.unbindDropZone()
    if (this.graph) {
      this.graph.dispose()
      this.graph = null
    }
  },
  methods: {
    groupIcon(type) {
      const icons = {
        common: 'el-icon-cpu',
        agent: 'el-icon-magic-stick',
        boolean: 'el-icon-question',
        switch: 'el-icon-s-operation',
        for: 'el-icon-refresh-right',
        iterator: 'el-icon-sort',
        declarative: 'el-icon-document'
      }
      return icons[type] || 'el-icon-menu'
    },
    groupColor(type) {
      const colors = {
        common: '#409EFF',
        agent: '#13C2C2',
        boolean: '#E6A23C',
        switch: '#67C23A',
        for: '#9C27B0',
        iterator: '#909399',
        declarative: '#722ED1'
      }
      return colors[type] || '#909399'
    },
    groupTagType(type) {
      const types = { common: '', agent: '', boolean: 'warning', switch: 'success', for: 'danger', iterator: 'info', declarative: 'info' }
      return types[type] || 'info'
    },
    groupShortLabel(type) {
      const labels = { common: '普通', agent: 'Agent', boolean: '布尔', switch: '选择', for: '循环', iterator: '迭代', declarative: '声明式' }
      return labels[type] || '其他'
    },
    formatComponentLabel(comp) {
      if (!comp) {
        return ''
      }
      const id = comp.nodeId || ''
      const name = (comp.name || '').trim()
      if (!name || name === id) {
        return id
      }
      return `${id} · ${name}`
    },
    initGraph() {
      const container = this.$refs.graphContainer
      if (!container) {
        return
      }
      if (this.graph) {
        this.graph.dispose()
        this.graph = null
      }
      const graph = new Graph({
        container,
        autoResize: true,
        grid: { visible: true, type: 'dot', args: { color: '#dcdfe6', thickness: 1 } },
        panning: { enabled: true, eventTypes: ['leftMouseDown', 'mouseWheel'] },
        mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'], minScale: 0.4, maxScale: 2 },
        connecting: {
          snap: true,
          allowBlank: false,
          allowLoop: false,
          allowMulti: false,
          connector: 'rounded',
          router: 'orth'
        }
      })
      const minimapEl = this.$refs.minimapContainer
      if (minimapEl) {
        minimapEl.innerHTML = ''
        graph.use(new MiniMap({
          container: minimapEl,
          width: 180,
          height: 120,
          padding: 8,
          scalable: false,
          minScale: 0.01,
          maxScale: 1
        }))
      }
      this.graph = graph
      graph.on('node:click', ({ node }) => {
        const key = node.getData() && node.getData().modelKey
        if (key) {
          this.hideContextMenu()
          this.selectModelNode(key)
          this.highlightSelectedGraphNode()
        }
      })
      graph.on('blank:click', () => {
        this.selectedModelKey = null
        this.selectedModelNode = null
        this.hideContextMenu()
        this.highlightSelectedGraphNode()
      })
      graph.on('node:contextmenu', ({ e, node }) => {
        e.preventDefault()
        const key = node.getData() && node.getData().modelKey
        if (key) {
          this.selectModelNode(key)
          this.highlightSelectedGraphNode()
          this.showContextMenu(e, 'node')
        }
      })
      graph.on('blank:contextmenu', ({ e }) => {
        e.preventDefault()
        this.selectedModelKey = null
        this.selectedModelNode = null
        this.highlightSelectedGraphNode()
        this.showContextMenu(e, 'blank')
      })
    },

    bindDropZone() {
      const container = this.$refs.graphContainer
      if (!container || this._dropBound) {
        return
      }
      this._dropBound = true
      this._onDragOver = (e) => {
        e.preventDefault()
        if (e.dataTransfer) {
          e.dataTransfer.dropEffect = 'copy'
        }
      }
      this._onDrop = (e) => {
        e.preventDefault()
        const nodeId = e.dataTransfer.getData('text/plain') || this.dragNodeId
        const item = this.components.find(c => c.nodeId === nodeId)
        if (item) {
          this.appendComponent(item)
        }
      }
      container.addEventListener('dragover', this._onDragOver)
      container.addEventListener('drop', this._onDrop)
    },

    unbindDropZone() {
      const container = this.$refs.graphContainer
      if (container && this._onDragOver) {
        container.removeEventListener('dragover', this._onDragOver)
        container.removeEventListener('drop', this._onDrop)
      }
      this._dropBound = false
    },

    showContextMenu(e, type = 'node') {
      const wrap = this.$refs.graphWrap
      if (!wrap) {
        return
      }
      const rect = wrap.getBoundingClientRect()
      this.contextMenuType = type
      this.contextMenu = {
        visible: true,
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
      }
    },

    hideContextMenu() {
      this.contextMenu.visible = false
    },

    handleContextAction(item) {
      if (item.divider || item.disabled) {
        return
      }
      const actions = {
        copyNodeId: () => this.copySelectedNodeId(),
        copyElSnippet: () => this.copySelectedElSnippet(),
        copyNode: () => this.copySelectedNode(),
        cutNode: () => this.cutSelectedNode(),
        pasteAfter: () => this.pasteNodeAfter(),
        pasteReplace: () => this.pasteReplace(),
        pasteAtEnd: () => this.pasteAtEnd(),
        duplicateToEnd: () => this.duplicateSelectedToEnd(),
        insertBefore: () => this.openComponentPicker('insertBefore'),
        insertAfter: () => this.openComponentPicker('insertAfter'),
        replaceComponent: () => this.openComponentPicker('replace'),
        moveUp: () => this.moveSelectedThenNode(-1),
        moveDown: () => this.moveSelectedThenNode(1),
        delete: () => this.deleteSelectedNode()
      }
      this.hideContextMenu()
      const fn = actions[item.action]
      if (fn) {
        fn()
      }
    },

    getSelectedModelNode() {
      return this.findModelByKey(this.flowModel, this.selectedModelKey)
    },

    copyText(text, successMsg) {
      if (!text) {
        this.$modal.msgWarning('无内容可复制')
        return
      }
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess(successMsg || '已复制')
        }).catch(() => {
          this.$modal.msgSuccess(text)
        })
      } else {
        this.$modal.msgSuccess(text)
      }
    },

    copySelectedNodeId() {
      const node = this.getSelectedModelNode()
      if (!node) {
        return
      }
      let id = ''
      if (node.type === 'component') {
        id = node.nodeId
      } else if (node.type === 'if' || node.type === 'switch') {
        id = node.condition || node.type
      } else if (node.type === 'for') {
        id = node.loopComponent || 'for'
      } else {
        id = node.type
      }
      this.copyText(id, 'nodeId 已复制')
    },

    copySelectedElSnippet() {
      const node = this.getSelectedModelNode()
      if (!node) {
        return
      }
      const snippet = buildExpr(node)
      this.copyText(snippet, 'EL 片段已复制')
    },

    stripModelKeysRecursive(node) {
      if (!node || typeof node !== 'object') {
        return
      }
      delete node._key
      if (node.type === 'then') {
        ;(node.children || []).forEach(c => this.stripModelKeysRecursive(c))
      } else if (node.type === 'if') {
        this.stripModelKeysRecursive(node.trueBranch)
        this.stripModelKeysRecursive(node.falseBranch)
      } else if (node.type === 'switch') {
        ;(node.branches || []).forEach(b => this.stripModelKeysRecursive(b))
      } else if (node.type === 'when') {
        ;(node.children || []).forEach(c => this.stripModelKeysRecursive(c))
      } else if (node.type === 'for') {
        this.stripModelKeysRecursive(node.body)
      } else if (node.type === 'catch') {
        this.stripModelKeysRecursive(node.tryNode)
        this.stripModelKeysRecursive(node.fallback)
      } else if (node.type === 'retry') {
        this.stripModelKeysRecursive(node.target)
      }
    },

    duplicateSelectedToEnd() {
      const node = this.getSelectedModelNode()
      if (!node) {
        return
      }
      this.mutateModel(() => {
        this.ensureFlowModel()
        const copy = JSON.parse(JSON.stringify(node))
        this.stripModelKeysRecursive(copy)
        this.flowModel.children.push(copy)
      })
      this.$modal.msgSuccess('已复制到链路末尾')
    },

    copySelectedNodeToClipboard(mode = 'copy') {
      const node = this.getSelectedModelNode()
      if (!node) {
        if (mode === 'copy') {
          this.$modal.msgWarning('请先选中节点')
        }
        return false
      }
      const copy = JSON.parse(JSON.stringify(node))
      this.stripModelKeysRecursive(copy)
      this.nodeClipboard = { node: copy, mode }
      return true
    },

    copySelectedNode() {
      if (this.copySelectedNodeToClipboard('copy')) {
        this.$modal.msgSuccess('节点已复制')
      }
    },

    cutSelectedNode() {
      if (!this.copySelectedNodeToClipboard('cut')) {
        return
      }
      this.deleteSelectedNode(true)
      this.$modal.msgSuccess('节点已剪切')
    },

    consumeClipboardIfCut() {
      if (this.nodeClipboard && this.nodeClipboard.mode === 'cut') {
        this.nodeClipboard = null
      }
    },

    pasteAtEnd() {
      if (!this.hasNodeClipboard) {
        this.$modal.msgWarning('剪贴板为空，请先复制节点')
        return
      }
      const copy = JSON.parse(JSON.stringify(this.nodeClipboard.node))
      this.stripModelKeysRecursive(copy)
      const insertIdx = (this.flowModel && this.flowModel.children) ? this.flowModel.children.length : 0
      this.mutateModel(() => {
        this.ensureFlowModel()
        this.flowModel.children.push(copy)
      })
      this.consumeClipboardIfCut()
      this.$nextTick(() => {
        this.selectModelNode(`root.c${insertIdx}`)
        this.highlightSelectedGraphNode()
      })
      this.$modal.msgSuccess('已粘贴到链路末尾')
    },

    pasteNodeAfter() {
      if (!this.hasNodeClipboard) {
        this.$modal.msgWarning('剪贴板为空，请先复制节点')
        return
      }
      if (!this.isTopLevelThenNode) {
        this.pasteAtEnd()
        return
      }
      const copy = JSON.parse(JSON.stringify(this.nodeClipboard.node))
      this.stripModelKeysRecursive(copy)
      const insertIdx = this.selectedThenIndex + 1
      this.mutateModel(() => {
        this.flowModel.children.splice(insertIdx, 0, copy)
      })
      this.consumeClipboardIfCut()
      this.$nextTick(() => {
        this.selectModelNode(`root.c${insertIdx}`)
        this.highlightSelectedGraphNode()
      })
      this.$modal.msgSuccess('已粘贴到后方')
    },

    pasteReplace() {
      if (!this.hasNodeClipboard) {
        this.$modal.msgWarning('剪贴板为空，请先复制节点')
        return
      }
      const node = this.getSelectedModelNode()
      if (!node || node.type !== 'component') {
        this.$modal.msgWarning('仅组件节点支持粘贴替换')
        return
      }
      const key = this.selectedModelKey
      const copy = JSON.parse(JSON.stringify(this.nodeClipboard.node))
      this.stripModelKeysRecursive(copy)
      this.mutateModel(() => {
        this.replaceModelNodeByKey(this.flowModel, key, copy)
      })
      this.consumeClipboardIfCut()
      this.$nextTick(() => {
        this.selectModelNode(key)
        this.highlightSelectedGraphNode()
      })
      this.$modal.msgSuccess('已粘贴替换')
    },

    replaceModelNodeByKey(root, key, newNode) {
      if (!root || !key) {
        return false
      }
      if (root.type === 'then') {
        const idx = (root.children || []).findIndex(c => c._key === key)
        if (idx >= 0) {
          root.children[idx] = newNode
          return true
        }
        for (const child of root.children || []) {
          if (this.replaceModelNodeByKey(child, key, newNode)) {
            return true
          }
        }
      }
      if (root.type === 'if') {
        if (root.trueBranch && root.trueBranch._key === key) {
          root.trueBranch = newNode
          return true
        }
        if (root.falseBranch && root.falseBranch._key === key) {
          root.falseBranch = newNode
          return true
        }
        if (root.trueBranch && this.replaceModelNodeByKey(root.trueBranch, key, newNode)) {
          return true
        }
        if (root.falseBranch && this.replaceModelNodeByKey(root.falseBranch, key, newNode)) {
          return true
        }
      }
      if (root.type === 'switch') {
        const bi = (root.branches || []).findIndex(b => b._key === key)
        if (bi >= 0) {
          root.branches[bi] = newNode
          return true
        }
      }
      if (root.type === 'when') {
        const wi = (root.children || []).findIndex(c => c._key === key)
        if (wi >= 0) {
          root.children[wi] = newNode
          return true
        }
      }
      if (root.type === 'for') {
        if (root.body && root.body._key === key) {
          root.body = newNode
          return true
        }
      }
      if (root.type === 'catch') {
        if (root.fallback && root.fallback._key === key) {
          root.fallback = newNode
          return true
        }
        if (root.tryNode && this.replaceModelNodeByKey(root.tryNode, key, newNode)) {
          return true
        }
      }
      if (root.type === 'retry') {
        if (root.target && root.target._key === key) {
          root.target = newNode
          return true
        }
      }
      return false
    },

    openComponentPicker(mode) {
      if (mode === 'replace') {
        const node = this.getSelectedModelNode()
        if (!node || node.type !== 'component') {
          this.$modal.msgWarning('仅组件节点可替换')
          return
        }
        this.pickerComponentId = node.nodeId
      } else {
        if (!this.isTopLevelThenNode) {
          this.$modal.msgWarning('仅 THEN 顶层节点支持前后插入')
          return
        }
        this.pickerComponentId = this.pickerComponentOptions[0]?.nodeId || ''
      }
      this.pickerMode = mode
      this.pickerOpen = true
    },

    confirmComponentPicker() {
      if (!this.pickerComponentId) {
        this.$modal.msgWarning('请选择组件')
        return
      }
      const comp = this.components.find(c => c.nodeId === this.pickerComponentId)
      if (!comp) {
        return
      }
      if (this.pickerMode === 'replace') {
        const node = this.getSelectedModelNode()
        if (!node || node.type !== 'component') {
          return
        }
        const key = this.selectedModelKey
        this.mutateModel(() => {
          node.nodeId = comp.nodeId
          node.name = comp.name
        })
        this.pickerOpen = false
        this.$nextTick(() => {
          this.selectModelNode(key)
          this.highlightSelectedGraphNode()
        })
        this.$modal.msgSuccess('组件已替换')
        return
      }
      const idx = this.selectedThenIndex
      if (idx < 0) {
        return
      }
      const insertIdx = this.pickerMode === 'insertBefore' ? idx : idx + 1
      this.mutateModel(() => {
        this.flowModel.children.splice(insertIdx, 0, createComponentNode(comp.nodeId, comp.name))
      })
      this.pickerOpen = false
      this.$nextTick(() => {
        this.selectModelNode(`root.c${insertIdx}`)
        this.highlightSelectedGraphNode()
      })
      this.$modal.msgSuccess('组件已插入')
    },

    moveSelectedThenNode(delta) {
      const idx = this.selectedThenIndex
      if (idx < 0) {
        return
      }
      const newIdx = idx + delta
      if (newIdx < 0 || newIdx >= this.flowModel.children.length) {
        return
      }
      const key = this.selectedModelKey
      this.mutateModel(() => {
        const [item] = this.flowModel.children.splice(idx, 1)
        this.flowModel.children.splice(newIdx, 0, item)
      })
      this.$nextTick(() => {
        this.selectModelNode(key)
        this.highlightSelectedGraphNode()
      })
    },

    onKeyDown(e) {
      if (this.editMode !== 'visual') {
        return
      }
      const tag = document.activeElement && document.activeElement.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA') {
        return
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'v') {
        e.preventDefault()
        if (this.isTopLevelThenNode) {
          this.pasteNodeAfter()
        } else {
          this.pasteAtEnd()
        }
        return
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'x') {
        e.preventDefault()
        this.cutSelectedNode()
        return
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'c' && !e.shiftKey) {
        e.preventDefault()
        this.copySelectedNode()
        return
      }
      if (!this.selectedModelKey) {
        return
      }
      if (e.key === 'Delete' || e.key === 'Backspace') {
        e.preventDefault()
        this.deleteSelectedNode()
      }
      if ((e.ctrlKey || e.metaKey) && e.key === 'd') {
        e.preventDefault()
        this.duplicateSelectedToEnd()
      }
    },

    deleteSelectedNode(silent = false) {
      this.hideContextMenu()
      if (!this.selectedModelKey) {
        if (!silent) {
          this.$modal.msgWarning('请先选中要删除的节点')
        }
        return
      }
      const doDelete = () => {
        const ok = this.removeModelNodeByKey(this.flowModel, this.selectedModelKey)
        if (!ok) {
          if (!silent) {
            this.$modal.msgWarning('该节点无法删除（如 SWITCH/WHEN 至少保留一项）')
          }
          return
        }
        this.selectedModelKey = null
        this.selectedModelNode = null
        enrichModelFromComponents(this.flowModel, this.components)
        this.pushHistory()
        this.renderFromModel(false)
        if (!silent) {
          this.$modal.msgSuccess('节点已删除')
        }
      }
      if (silent) {
        doDelete()
      } else {
        this.$modal.confirm('确认删除选中节点？').then(doDelete).catch(() => {})
      }
    },

    removeModelNodeByKey(node, key) {
      if (!node || !key) {
        return false
      }
      if (node.type === 'then') {
        const idx = (node.children || []).findIndex(c => c._key === key)
        if (idx >= 0) {
          node.children.splice(idx, 1)
          return true
        }
        for (const child of node.children || []) {
          if (this.removeModelNodeByKey(child, key)) {
            return true
          }
        }
      }
      if (node.type === 'if') {
        if (node.trueBranch && node.trueBranch._key === key) {
          node.trueBranch = this.createFallbackComponent()
          return true
        }
        if (node.falseBranch && node.falseBranch._key === key) {
          node.falseBranch = this.createFallbackComponent()
          return true
        }
      }
      if (node.type === 'switch') {
        const bi = (node.branches || []).findIndex(b => b._key === key)
        if (bi >= 0) {
          if (node.branches.length <= 1) {
            return false
          }
          node.branches.splice(bi, 1)
          return true
        }
      }
      if (node.type === 'when') {
        const wi = (node.children || []).findIndex(c => c._key === key)
        if (wi >= 0) {
          if (node.children.length <= 1) {
            return false
          }
          node.children.splice(wi, 1)
          return true
        }
      }
      if (node.type === 'for') {
        if (node.body && node.body._key === key) {
          node.body = this.createFallbackComponent('processOrderItem')
          return true
        }
      }
      if (node.type === 'catch') {
        if (node.tryNode && this.removeModelNodeByKey(node.tryNode, key)) {
          return true
        }
        if (node.fallback && node.fallback._key === key) {
          node.fallback = this.createFallbackComponent('notifyFallback')
          return true
        }
      }
      if (node.type === 'retry') {
        if (node.target && node.target._key === key) {
          node.target = this.createFallbackComponent('sendNotify')
          return true
        }
      }
      return false
    },

    createFallbackComponent(preferredId) {
      const comp = this.components.find(c => c.nodeId === preferredId) || this.commonComponents[0]
      const id = comp ? comp.nodeId : (preferredId || 'nodeA')
      const name = comp ? comp.name : id
      return createComponentNode(id, name)
    },

    highlightSelectedGraphNode() {
      if (!this.graph) {
        return
      }
      const selectedKey = this.selectedModelKey
      this.graph.getNodes().forEach(node => {
        const data = node.getData() || {}
        const isSelected = selectedKey && data.modelKey === selectedKey
        const lfType = data.lfType
        const strokeWidth = isSelected ? 3 : 1
        if (lfType === 'component') {
          node.attr('body/stroke', '#409EFF')
          node.attr('body/strokeWidth', strokeWidth)
          node.attr('body/fill', isSelected ? '#d9ecff' : '#ecf5ff')
        } else if (lfType === 'if') {
          node.attr('body/stroke', '#E6A23C')
          node.attr('body/strokeWidth', strokeWidth)
          node.attr('body/fill', isSelected ? '#faecd8' : '#fdf6ec')
        } else if (lfType === 'switch') {
          node.attr('body/stroke', '#67C23A')
          node.attr('body/strokeWidth', strokeWidth)
        } else if (lfType === 'when') {
          node.attr('body/stroke', '#909399')
          node.attr('body/strokeWidth', strokeWidth)
        } else if (lfType === 'for') {
          node.attr('body/stroke', '#9C27B0')
          node.attr('body/strokeWidth', strokeWidth)
        } else if (lfType === 'catch') {
          node.attr('body/stroke', '#F56C6C')
          node.attr('body/strokeWidth', strokeWidth)
        } else if (lfType === 'subchain') {
          node.attr('body/stroke', '#13C2C2')
          node.attr('body/strokeWidth', strokeWidth)
          node.attr('body/fill', isSelected ? '#b5f5ec' : '#e6fffb')
        }
      })
    },

    assignModelKeys(node, prefix = 'root') {
      if (!node) {
        return
      }
      node._key = prefix
      if (node.type === 'then') {
        ;(node.children || []).forEach((c, i) => this.assignModelKeys(c, `${prefix}.c${i}`))
      } else if (node.type === 'if') {
        this.assignModelKeys(node.trueBranch, `${prefix}.t`)
        this.assignModelKeys(node.falseBranch, `${prefix}.f`)
      } else if (node.type === 'switch') {
        ;(node.branches || []).forEach((b, i) => this.assignModelKeys(b, `${prefix}.b${i}`))
      } else if (node.type === 'when') {
        ;(node.children || []).forEach((c, i) => this.assignModelKeys(c, `${prefix}.w${i}`))
      } else if (node.type === 'for') {
        this.assignModelKeys(node.body, `${prefix}.body`)
      } else if (node.type === 'catch') {
        this.assignModelKeys(node.tryNode, `${prefix}.try`)
        this.assignModelKeys(node.fallback, `${prefix}.fb`)
      } else if (node.type === 'retry') {
        this.assignModelKeys(node.target, `${prefix}.tgt`)
      }
    },

    findModelByKey(node, key) {
      if (!node) {
        return null
      }
      if (node._key === key) {
        return node
      }
      if (node.type === 'then') {
        for (const c of node.children || []) {
          const found = this.findModelByKey(c, key)
          if (found) {
            return found
          }
        }
      }
      if (node.type === 'if') {
        return this.findModelByKey(node.trueBranch, key) || this.findModelByKey(node.falseBranch, key)
      }
      if (node.type === 'switch') {
        for (const b of node.branches || []) {
          const found = this.findModelByKey(b, key)
          if (found) {
            return found
          }
        }
      }
      if (node.type === 'when') {
        for (const c of node.children || []) {
          const found = this.findModelByKey(c, key)
          if (found) {
            return found
          }
        }
      }
      if (node.type === 'for') {
        return this.findModelByKey(node.body, key)
      }
      if (node.type === 'catch') {
        return this.findModelByKey(node.tryNode, key) || this.findModelByKey(node.fallback, key)
      }
      if (node.type === 'retry') {
        return this.findModelByKey(node.target, key)
      }
      return null
    },

    resetHistory() {
      this.historyStack = []
      this.historyIndex = -1
      if (this.flowModel) {
        this.pushHistory()
      }
    },

    pushHistory() {
      if (!this.flowModel) {
        return
      }
      const snap = JSON.stringify(this.flowModel)
      if (this.historyIndex >= 0 && this.historyStack[this.historyIndex] === snap) {
        return
      }
      this.historyStack = this.historyStack.slice(0, this.historyIndex + 1)
      this.historyStack.push(snap)
      if (this.historyStack.length > 30) {
        this.historyStack.shift()
      } else {
        this.historyIndex++
      }
    },

    undo() {
      if (!this.canUndo) {
        return
      }
      this.historyIndex--
      this.flowModel = JSON.parse(this.historyStack[this.historyIndex])
      enrichModelFromComponents(this.flowModel, this.components)
      this.renderFromModel(false)
    },

    redo() {
      if (!this.canRedo) {
        return
      }
      this.historyIndex++
      this.flowModel = JSON.parse(this.historyStack[this.historyIndex])
      enrichModelFromComponents(this.flowModel, this.components)
      this.renderFromModel(false)
    },

    mutateModel(mutator) {
      this.ensureFlowModel()
      this.pushHistory()
      mutator()
      enrichModelFromComponents(this.flowModel, this.components)
      this.renderFromModel(false)
    },

    selectModelNode(key) {
      const node = this.findModelByKey(this.flowModel, key)
      if (!node) {
        return
      }
      this.selectedModelKey = key
      const lfType = node.type === 'component' ? 'component' : node.type
      const view = {
        modelKey: key,
        lfType,
        name: node.name || node.nodeId || node.condition || '',
        remark: node.remark || ''
      }
      if (node.type === 'component') {
        view.nodeId = node.nodeId
        view.tag = node.tag || ''
        view.dataKey = node.dataKey || ''
        view.dataValue = node.dataValue || ''
        view.bind = node.bind || ''
      }
      if (node.type === 'subchain') {
        view.chainName = node.chainName
      }
      if (node.type === 'if') {
        view.condition = node.condition
        view.trueNodeId = node.trueBranch && node.trueBranch.nodeId
        view.falseNodeId = node.falseBranch && node.falseBranch.nodeId
      }
      if (node.type === 'switch') {
        view.condition = node.condition
        view.branchNodeIds = (node.branches || []).map(b => b.nodeId)
      }
      if (node.type === 'when') {
        view.maxWaitSeconds = node.maxWaitSeconds
        view.childNodeIds = (node.children || []).map(c => c.nodeId)
      }
      if (node.type === 'for') {
        view.loopComponent = node.loopComponent
        view.bodyNodeId = node.body && node.body.nodeId
      }
      if (node.type === 'catch') {
        if (node.tryNode && node.tryNode.type === 'retry') {
          view.tryNodeId = node.tryNode.target && node.tryNode.target.nodeId
        } else {
          view.tryNodeId = node.tryNode && node.tryNode.nodeId
        }
        view.fallbackNodeId = node.fallback && node.fallback.nodeId
      }
      this.selectedModelNode = view
    },

    onComponentMetaChange() {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node || node.type !== 'component') {
        return
      }
      node.tag = this.selectedModelNode.tag || ''
      node.dataKey = this.selectedModelNode.dataKey || ''
      node.dataValue = this.selectedModelNode.dataValue || ''
      node.bind = this.selectedModelNode.bind || ''
      this.pushHistory()
      this.renderFromModel(true)
    },

    onWhenChange() {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node || node.type !== 'when') {
        return
      }
      node.maxWaitSeconds = this.selectedModelNode.maxWaitSeconds
      node.children = this.selectedModelNode.childNodeIds.map(id => {
        const comp = this.components.find(c => c.nodeId === id)
        return createComponentNode(id, comp ? comp.name : id)
      })
      this.pushHistory()
      this.renderFromModel(true)
    },

    addWhenChild() {
      if (!this.selectedModelNode || this.selectedModelNode.lfType !== 'when') {
        return
      }
      this.selectedModelNode.childNodeIds.push(this.commonComponents[0]?.nodeId || '')
      this.onWhenChange()
    },

    onForChange() {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node || node.type !== 'for') {
        return
      }
      node.loopComponent = this.selectedModelNode.loopComponent
      const comp = this.components.find(c => c.nodeId === this.selectedModelNode.bodyNodeId)
      node.body = createComponentNode(this.selectedModelNode.bodyNodeId, comp ? comp.name : this.selectedModelNode.bodyNodeId)
      this.pushHistory()
      this.renderFromModel(true)
    },

    onCatchChange() {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node || node.type !== 'catch') {
        return
      }
      const tryComp = this.components.find(c => c.nodeId === this.selectedModelNode.tryNodeId)
      const fbComp = this.components.find(c => c.nodeId === this.selectedModelNode.fallbackNodeId)
      node.tryNode = createComponentNode(this.selectedModelNode.tryNodeId, tryComp ? tryComp.name : this.selectedModelNode.tryNodeId)
      node.fallback = createComponentNode(this.selectedModelNode.fallbackNodeId, fbComp ? fbComp.name : this.selectedModelNode.fallbackNodeId)
      this.pushHistory()
      this.renderFromModel(true)
    },

    onPropChange() {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node) {
        return
      }
      node.name = this.selectedModelNode.name
      node.remark = this.selectedModelNode.remark
      if (node.type === 'if') {
        node.condition = this.selectedModelNode.condition
      }
      if (node.type === 'switch') {
        node.condition = this.selectedModelNode.condition
      }
      if (node.type === 'subchain') {
        node.name = this.selectedModelNode.name
      }
      this.pushHistory()
      this.renderFromModel(true)
    },

    onIfBranchChange(which) {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node || node.type !== 'if') {
        return
      }
      const id = which === 'true' ? this.selectedModelNode.trueNodeId : this.selectedModelNode.falseNodeId
      const comp = this.components.find(c => c.nodeId === id)
      const branch = createComponentNode(id, comp ? comp.name : id)
      if (which === 'true') {
        node.trueBranch = branch
      } else {
        node.falseBranch = branch
      }
      this.pushHistory()
      this.renderFromModel(true)
    },

    onSwitchBranchChange() {
      const node = this.findModelByKey(this.flowModel, this.selectedModelKey)
      if (!node || node.type !== 'switch') {
        return
      }
      node.branches = this.selectedModelNode.branchNodeIds.map(id => {
        const comp = this.components.find(c => c.nodeId === id)
        return createComponentNode(id, comp ? comp.name : id)
      })
      this.pushHistory()
      this.renderFromModel(true)
    },

    addSwitchBranch() {
      if (!this.selectedModelNode || this.selectedModelNode.lfType !== 'switch') {
        return
      }
      this.selectedModelNode.branchNodeIds.push(this.commonComponents[0]?.nodeId || '')
      this.onSwitchBranchChange()
    },

    loadFromData(elData, graphJson) {
      this.elWarning = ''
      if (graphJson) {
        try {
          const data = typeof graphJson === 'string' ? JSON.parse(graphJson) : graphJson
          if (data && data.lfFlowModel) {
            this.flowModel = JSON.parse(JSON.stringify(data.lfFlowModel))
            enrichModelFromComponents(this.flowModel, this.components)
            this.renderFromModel(false)
            this.resetHistory()
            return
          }
        } catch (e) {
          // fall through
        }
      }
      this.loadFromEl(elData)
    },

    loadFromEl(elData) {
      const model = parseEl(elData)
      if (model) {
        this.flowModel = model
        enrichModelFromComponents(this.flowModel, this.components)
        this.renderFromModel(false)
        this.resetHistory()
        return
      }
      if (elData && elData.trim()) {
        this.elWarning = '无法解析 EL 表达式，请检查语法或在链路管理中编辑。'
        this.elPreview = elData
      }
    },

    autoLayout() {
      if (!this.flowModel) {
        return
      }
      this.pushHistory()
      this.renderFromModel(true)
      this.$nextTick(() => {
        if (this.graph) {
          this.graph.centerContent()
        }
      })
      this.$message.success('已按流程模型重新布局')
    },

    renderFromModel(keepSelection = true) {
      if (!this.graph || !this.flowModel) {
        return
      }
      const prevKey = keepSelection ? this.selectedModelKey : null
      this.assignModelKeys(this.flowModel)
      const { nodes, edges } = layoutFlowModel(this.flowModel, this.components)
      this.graph.clearCells()
      nodes.forEach(n => {
        this.graph.addNode({
          id: n.cellId,
          shape: n.shape,
          x: n.x,
          y: n.y,
          width: n.width,
          height: n.height,
          label: n.label,
          data: n.data,
          attrs: n.attrs,
          ports: n.ports
        })
      })
      edges.forEach(e => {
        this.graph.addEdge({
          source: { cell: e.source, port: e.sourcePort },
          target: { cell: e.target, port: e.targetPort },
          attrs: e.attrs
        })
      })
      this.elPreview = buildEl(this.flowModel)
      if (prevKey) {
        this.selectModelNode(prevKey)
      }
      this.$nextTick(() => {
        this.highlightSelectedGraphNode()
        if (this.executeResult && this.executeResult.executeStepStr) {
          this.highlightSteps(this.executeResult.executeStepStr, {
            failedNodeId: this.executeResult.failedNodeId,
            success: this.executeResult.success
          })
        } else {
          this.applyLogHighlight()
        }
      })
    },

    ensureFlowModel() {
      if (!this.flowModel) {
        this.flowModel = { type: 'then', children: [] }
      }
      if (this.flowModel.type !== 'then') {
        this.flowModel = { type: 'then', children: [this.flowModel] }
      }
    },

    appendComponent(item) {
      this.mutateModel(() => {
        this.ensureFlowModel()
        this.flowModel.children.push(createComponentNode(item.nodeId, item.name))
      })
    },

    appendSubChain(item) {
      if (!item || !item.chainName) {
        return
      }
      this.mutateModel(() => {
        this.ensureFlowModel()
        this.flowModel.children.push(createSubChainNode(item.chainName, item.chainDesc || item.chainName))
      })
    },

    onSubChainDragStart(event, item) {
      event.dataTransfer.setData('text/plain', 'subchain:' + item.chainName)
    },

    addIfNode() {
      this.mutateModel(() => {
        this.ensureFlowModel()
        const cond = this.booleanComponents[0]?.nodeId || 'hasStock'
        const ifNode = createIfNode(cond)
        ifNode.trueBranch = createComponentNode('calcDiscount', '优惠计算')
        ifNode.falseBranch = createComponentNode('orderFail', '订单失败')
        this.flowModel.children.push(ifNode)
      })
    },

    addSwitchNode() {
      this.mutateModel(() => {
        this.ensureFlowModel()
        const cond = this.switchComponents[0]?.nodeId || 'payType'
        const ids = ['aliPay', 'wechatPay', 'balancePay'].filter(id => this.components.some(c => c.nodeId === id))
        this.flowModel.children.push(createSwitchNode(cond, ids.length ? ids : ['branch1', 'branch2']))
      })
    },

    addWhenNode() {
      this.mutateModel(() => {
        this.ensureFlowModel()
        const ids = this.commonComponents.slice(0, 3).map(c => c.nodeId)
        this.flowModel.children.push(createWhenNode(ids.length >= 2 ? ids : ['nodeA', 'nodeB'], 3))
      })
    },

    addForNode() {
      this.mutateModel(() => {
        this.ensureFlowModel()
        const loop = this.forComponents[0]?.nodeId || 'batchCount'
        const body = this.commonComponents[0]?.nodeId || 'processOrderItem'
        this.flowModel.children.push(createForNode(loop, body))
      })
    },

    addCatchNode() {
      this.mutateModel(() => {
        this.ensureFlowModel()
        const tryId = this.commonComponents[0]?.nodeId || 'sendNotify'
        const fbId = this.commonComponents[1]?.nodeId || 'notifyFallback'
        this.flowModel.children.push(createCatchNode(tryId, fbId))
      })
    },

    copyEl() {
      this.elPreview = buildEl(this.flowModel)
      if (!this.elPreview) {
        this.$modal.msgWarning('暂无 EL 内容')
        return
      }
      if (navigator.clipboard) {
        navigator.clipboard.writeText(this.elPreview).then(() => {
          this.$modal.msgSuccess('EL 已复制到剪贴板')
        }).catch(() => {
          this.$modal.msgSuccess('EL: ' + this.elPreview)
        })
      } else {
        this.$modal.msgSuccess('EL: ' + this.elPreview)
      }
    },

    handleValidate() {
      this.elPreview = buildEl(this.flowModel)
      const local = validateFlowModel(this.flowModel, this.components)
      if (!local.valid) {
        this.validateResult = local.errors.join('；')
        this.validateResultType = 'error'
        return
      }
      validateEl(this.elPreview).then(() => {
        const warn = local.warnings.length ? '（' + local.warnings.join('；') + '）' : ''
        this.validateResult = 'EL 校验通过' + warn
        this.validateResultType = 'success'
      }).catch(err => {
        this.validateResult = (err && err.msg) || '服务端 EL 校验失败'
        this.validateResultType = 'error'
      })
    },

    onDragStart(event, item) {
      this.dragNodeId = item.nodeId
      event.dataTransfer.setData('text/plain', item.nodeId)
    },

    clearCanvas() {
      this.$modal.confirm('确认清空画布？').then(() => {
        this.flowModel = { type: 'then', children: [] }
        this.selectedModelKey = null
        this.selectedModelNode = null
        this.renderFromModel(false)
        this.resetHistory()
      }).catch(() => {})
    },

    handleSave() {
      if (this.readonly) {
        this.$modal.msgWarning(this.readonlyMessage || '当前环境为只读模式，禁止保存')
        return
      }
      if (this.editMode === 'el') {
        const model = parseEl(this.elTextDraft)
        if (!model) {
          this.$modal.msgError('EL 文本无法解析，请修正后再保存')
          return
        }
        this.flowModel = model
        enrichModelFromComponents(this.flowModel, this.components)
      }
      if (!this.flowModel || !this.flowModel.children || !this.flowModel.children.length) {
        this.$modal.msgWarning('请至少添加一个节点')
        return
      }
      const local = validateFlowModel(this.flowModel, this.components)
      if (!local.valid) {
        this.$modal.msgError(local.errors.join('；'))
        return
      }
      this.elPreview = buildEl(this.flowModel)
      validateEl(this.elPreview).then(() => {
        this.$emit('save', {
          elData: this.elPreview,
          graphJson: JSON.stringify({
            lfFlowModel: this.flowModel,
            cells: this.graph ? this.graph.toJSON().cells : []
          }),
          flowModel: this.flowModel
        })
      }).catch(err => {
        this.$modal.msgError((err && err.msg) || 'EL 校验失败，无法保存')
      })
    },

    onEditModeChange(mode) {
      if (mode === 'el') {
        this.elTextDraft = buildEl(this.flowModel) || this.elPreview
        return
      }
      const model = parseEl(this.elTextDraft)
      if (!model) {
        this.$modal.msgError('EL 无法解析，请修正后再切回可视化')
        this.$nextTick(() => { this.editMode = 'el' })
        return
      }
      this.flowModel = model
      enrichModelFromComponents(this.flowModel, this.components)
      this.$nextTick(() => {
        this.initGraph()
        this.bindDropZone()
        this.renderFromModel(false)
        this.resetHistory()
        this.$nextTick(() => {
          if (this.graph) {
            this.graph.resize()
          }
        })
      })
    },

    openTestRun() {
      if (!this.chainName) {
        return
      }
      this.executeParamJson = JSON.stringify(getDefaultExecuteParam(this.chainName), null, 2)
      this.executeResult = null
      this.executeOpen = true
    },

    submitTestRun() {
      let param = {}
      try {
        param = JSON.parse(this.executeParamJson || '{}')
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      executeChain(this.chainName, param).then(res => {
        this.executeResult = res.data
        if (this.editMode === 'visual' && this.executeResult.executeStepStr) {
          this.highlightSteps(this.executeResult.executeStepStr, {
            failedNodeId: this.executeResult.failedNodeId,
            success: this.executeResult.success
          })
        }
      })
    },

    openElDebug() {
      this.elDebugEl = this.editMode === 'el' ? this.elTextDraft : (buildEl(this.flowModel) || this.elPreview || '')
      this.elDebugContextClass = this.contextClass || ''
      this.elDebugParamJson = JSON.stringify(getDefaultExecuteParam(this.chainName), null, 2)
      this.elDebugResult = null
      this.elDebugOpen = true
    },

    submitElDebug() {
      let param = {}
      try {
        param = JSON.parse(this.elDebugParamJson || '{}')
      } catch (e) {
        this.$modal.msgError('JSON 格式不正确')
        return
      }
      if (!this.elDebugEl || !this.elDebugEl.trim()) {
        this.$modal.msgWarning('EL 不能为空')
        return
      }
      executeEl({
        elData: this.elDebugEl.trim(),
        param: param,
        contextClass: this.elDebugContextClass || undefined
      }).then(res => {
        this.elDebugResult = res.data
      })
    },

    applyLogHighlight() {
      if (!this.logHighlight || !this.logHighlight.executeStepStr || !this.graph) {
        return
      }
      if (this.editMode !== 'visual') {
        this.editMode = 'visual'
      }
      this.highlightSteps(this.logHighlight.executeStepStr, {
        failedNodeId: this.logHighlight.failedNodeId,
        success: this.logHighlight.success !== false && this.logHighlight.success !== 0 && this.logHighlight.success !== '0'
      })
    },

    formatExecuteResult(result) {
      return JSON.stringify(result, null, 2)
    },

    highlightSteps(stepStr, options = {}) {
      this.clearStepHighlight()
      if (!stepStr || !this.graph) {
        return
      }
      const { failedNodeId, success } = options
      const isFailureRun = success === false || success === 0 || success === '0'
      const ids = stepStr.split('==>').map(s => s.trim()).filter(Boolean)
      let failedKey = null
      this.graph.getNodes().forEach(node => {
        const data = node.getData() || {}
        const nodeId = data.nodeId
        if (!nodeId || !ids.includes(nodeId)) {
          return
        }
        const isFailedNode = isFailureRun && failedNodeId && nodeId === failedNodeId
        if (isFailedNode) {
          node.attr('body/stroke', '#F56C6C')
          node.attr('body/strokeWidth', 3)
          node.attr('body/fill', '#fde2e2')
          failedKey = data.modelKey
        } else {
          node.attr('body/stroke', '#67C23A')
          node.attr('body/strokeWidth', 3)
          node.attr('body/fill', '#e1f3d8')
        }
      })
      if (failedKey) {
        this.selectModelNode(failedKey)
        this.highlightSelectedGraphNode()
      }
    },

    clearStepHighlight() {
      if (!this.graph) {
        return
      }
      this.graph.getNodes().forEach(node => {
        const data = node.getData() || {}
        const lfType = data.lfType
        if (lfType === 'component') {
          node.attr('body/stroke', '#409EFF')
          node.attr('body/strokeWidth', 1)
          node.attr('body/fill', '#ecf5ff')
        } else if (lfType === 'if') {
          node.attr('body/stroke', '#E6A23C')
          node.attr('body/strokeWidth', 1)
          node.attr('body/fill', '#fdf6ec')
        } else if (lfType === 'switch') {
          node.attr('body/stroke', '#67C23A')
          node.attr('body/strokeWidth', 1)
        } else if (lfType === 'when') {
          node.attr('body/stroke', '#909399')
          node.attr('body/strokeWidth', 1)
        } else if (lfType === 'for') {
          node.attr('body/stroke', '#9C27B0')
          node.attr('body/strokeWidth', 1)
        } else if (lfType === 'catch') {
          node.attr('body/stroke', '#F56C6C')
          node.attr('body/strokeWidth', 1)
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.lf-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  min-height: 520px;
}

.lf-editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 12px;
  border-bottom: 1px solid #ebeef5;
  .chain-title { font-size: 16px; font-weight: 600; color: #303133; flex-shrink: 0; }
  .toolbar-center { flex: 1; text-align: center; }
  .toolbar-actions { flex-shrink: 0; }
}

.lf-editor-alert { margin-top: 8px; }

.lf-editor-body {
  flex: 1;
  display: flex;
  gap: 12px;
  margin-top: 12px;
  min-height: 0;
  .graph-wrap {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }
}

.el-text-mode {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  min-height: 0;
}

.panel-left, .panel-right {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.left-panel-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  ::v-deep .el-tabs__header {
    margin: 0;
    padding: 0 8px;
    background: #f5f7fa;
  }
  ::v-deep .el-tabs__content {
    flex: 1;
    overflow: hidden;
  }
  ::v-deep .el-tab-pane {
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

.panel-search {
  padding: 8px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.logic-panel {
  padding: 10px;
  overflow-y: auto;
}

.logic-desc {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px;
  line-height: 1.5;
}

.logic-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.logic-card {
  padding: 10px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
  background: #fff;
  i { font-size: 20px; display: block; margin-bottom: 4px; }
  .logic-label { font-size: 13px; font-weight: 600; color: #303133; display: block; }
  .logic-tip { font-size: 11px; color: #909399; display: block; margin-top: 2px; }
  &:hover {
    background: #f5f7fa;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
    transform: translateY(-1px);
  }
}

.panel-title {
  padding: 8px 12px;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}

.logic-tools {
  padding: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  border-bottom: 1px solid #ebeef5;
}

.component-groups {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px 8px;
  ::v-deep .el-collapse { border: none; }
  ::v-deep .el-collapse-item__header {
    height: 36px;
    line-height: 36px;
    font-size: 13px;
    border-bottom: 1px solid #f0f0f0;
  }
  ::v-deep .el-collapse-item__wrap { border-bottom: none; }
  ::v-deep .el-collapse-item__content { padding-bottom: 4px; }
}

.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #606266;
}

.group-badge {
  ::v-deep .el-badge__content { height: 16px; line-height: 16px; padding: 0 5px; }
}

.component-item {
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: grab;
  background: #fafafa;
  transition: all 0.15s;
  &:hover {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 1px 4px rgba(64, 158, 255, 0.15);
  }
  .component-item-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 4px;
  }
  .node-id { font-size: 13px; font-weight: 600; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .node-name { font-size: 12px; color: #909399; margin-top: 2px; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

.panel-tip {
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  border-top: 1px solid #ebeef5;
  margin: 0;
}

.graph-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
}

.lf-minimap {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 180px;
  height: 120px;
  z-index: 5;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  pointer-events: auto;
}

.graph-context-menu {
  position: absolute;
  z-index: 20;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  min-width: 168px;
  li {
    padding: 8px 14px;
    font-size: 13px;
    color: #606266;
    cursor: pointer;
    i { margin-right: 6px; }
    &:hover:not(.disabled):not(.divider) { background: #f5f7fa; color: #409eff; }
    &.disabled {
      color: #c0c4cc;
      cursor: not-allowed;
    }
    &.divider {
      padding: 0;
      margin: 4px 0;
      height: 1px;
      background: #ebeef5;
      cursor: default;
    }
    &.danger {
      color: #f56c6c;
      &:hover:not(.disabled) { color: #f56c6c; background: #fef0f0; }
    }
  }
}

.insert-dialog-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #909399;
}

.graph-container {
  width: 100%;
  height: 100%;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fafafa;
}

.prop-form {
  padding: 8px;
  overflow-y: auto;
  flex: 1;
}

.branch-row { margin-bottom: 6px; }

.panel-right ::v-deep .el-textarea { padding: 8px; }

.exec-result-box {
  border-top: 1px solid #ebeef5;
  padding: 8px;
  .exec-steps { font-size: 12px; color: #606266; margin: 8px 0 0; word-break: break-all; }
}

.execute-result-pre {
  margin-top: 12px;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
}
</style>

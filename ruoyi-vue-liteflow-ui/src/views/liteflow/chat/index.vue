<template>
  <div class="app-container chat-page">
    <div class="chat-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <!-- 折叠后的窄条 -->
      <aside v-if="sidebarCollapsed" class="chat-rail">
        <el-tooltip content="展开会话" placement="right">
          <button type="button" class="rail-btn" @click="toggleSidebar">
            <i class="el-icon-s-unfold" />
          </button>
        </el-tooltip>
        <el-tooltip content="新对话" placement="right">
          <button
            type="button"
            class="rail-btn primary"
            v-hasPermi="['liteflow:chat:send']"
            @click="handleNewSession"
          >
            <i class="el-icon-plus" />
          </button>
        </el-tooltip>
      </aside>

      <!-- 左侧会话 -->
      <aside v-show="!sidebarCollapsed" class="chat-sidebar">
        <div class="chat-sidebar__header">
          <div class="chat-sidebar__brand">
            <span class="brand-icon"><i class="el-icon-chat-dot-round" /></span>
            <div class="brand-text">
              <div class="brand-title">AI 助手</div>
              <div class="brand-sub">内部轻量对话</div>
            </div>
            <el-tooltip content="折叠侧栏" placement="bottom">
              <button type="button" class="collapse-btn" @click="toggleSidebar">
                <i class="el-icon-s-fold" />
              </button>
            </el-tooltip>
          </div>
          <el-button
            type="primary"
            size="small"
            icon="el-icon-plus"
            class="btn-new"
            @click="handleNewSession"
            v-hasPermi="['liteflow:chat:send']"
          >新对话</el-button>
        </div>
        <div v-loading="sessionLoading" class="chat-sidebar__list">
          <div
            v-for="item in sessions"
            :key="item.id"
            class="session-item"
            :class="{ active: currentSessionId === item.id }"
            @click="selectSession(item)"
          >
            <div class="session-item__title" :title="item.title">{{ item.title || '新对话' }}</div>
            <div class="session-item__meta">
              <el-tag size="mini" type="info" effect="plain">{{ item.modelName || item.modelCode || '默认模型' }}</el-tag>
              <span class="session-time">{{ formatSessionTime(item.updateTime || item.createTime) }}</span>
              <i
                class="el-icon-delete"
                title="删除"
                v-hasPermi="['liteflow:chat:remove']"
                @click.stop="handleDeleteSession(item)"
              />
            </div>
          </div>
          <div v-if="!sessionLoading && !sessions.length" class="chat-empty">
            <i class="el-icon-chat-line-square" />
            <p>暂无会话</p>
            <span>发一条消息即可开始</span>
          </div>
        </div>
      </aside>

      <!-- 主对话区 -->
      <section class="chat-main">
        <div class="chat-main__tip">
          <i class="el-icon-info" />
          <span>复用「模型配置」默认模型与日配额 · 对话仅本人可见 · 请先配置 API Key</span>
        </div>

        <div ref="messageBox" class="chat-messages">
          <div class="chat-messages__inner">
            <div v-if="!messages.length && !sending" class="chat-welcome">
              <div class="welcome-badge">LiteFlow</div>
              <h2>有什么可以帮你？</h2>
              <p>编排规则、链路试跑、模型配置与平台使用问题，都可以问我。</p>
              <div class="chat-suggestions">
                <button
                  v-for="tip in suggestions"
                  :key="tip"
                  type="button"
                  class="suggest-card"
                  @click="applySuggestion(tip)"
                >
                  <i class="el-icon-right" />
                  <span>{{ tip }}</span>
                </button>
              </div>
            </div>

            <div
              v-for="(msg, idx) in messages"
              :key="msg.id || ('tmp-' + idx)"
              class="msg-row"
              :class="'role-' + msg.role"
            >
              <div class="msg-avatar" :class="msg.role">
                <i v-if="msg.role === 'assistant'" class="el-icon-cpu" />
                <span v-else>我</span>
              </div>
              <div class="msg-body">
                <div class="msg-meta">
                  <span class="msg-role-label">{{ msg.role === 'assistant' ? 'AI 助手' : '我' }}</span>
                  <span v-if="formatMsgTime(msg)" class="msg-time">{{ formatMsgTime(msg) }}</span>
                  <span v-if="msg.stopped" class="msg-stopped">已停止</span>
                </div>
                <div class="msg-bubble" :class="{ streaming: msg.streaming && !msg.content }">
                  <div
                    v-if="msg.role === 'assistant'"
                    class="msg-md"
                    v-html="renderMarkdown(msg.content, msg.streaming)"
                  />
                  <div v-else class="msg-text">{{ msg.content }}</div>
                  <span v-if="msg.streaming" class="typing-cursor" />
                </div>
                <div v-if="msg.role === 'assistant' && !msg.streaming && msg.content" class="msg-actions">
                  <el-button type="text" size="mini" icon="el-icon-document-copy" @click="copyText(msg.content)">复制</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-composer">
          <div class="composer-card">
            <el-input
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
              v-model="input"
              :disabled="sending"
              resize="none"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              @keydown.native="onKeydown"
            />
            <div class="composer-bar">
              <div class="composer-meta">
                <el-tag size="mini" effect="plain" type="success">
                  <i class="el-icon-connection" /> {{ modelHint }}
                </el-tag>
                <span v-if="sending" class="sending-hint">
                  <i class="el-icon-loading" /> 生成中…
                </span>
              </div>
              <div class="composer-actions">
                <el-button
                  v-if="sending"
                  type="danger"
                  plain
                  size="small"
                  icon="el-icon-video-pause"
                  @click="handleStop"
                >停止</el-button>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  icon="el-icon-s-promotion"
                  :disabled="!input.trim()"
                  @click="handleSend"
                  v-hasPermi="['liteflow:chat:send']"
                >发送</el-button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import {
  listChatSessions,
  listChatMessages,
  deleteChatSessions,
  chatStream
} from '@/api/liteflow/chat'

const SIDEBAR_KEY = 'lf-chat-sidebar-collapsed'

export default {
  name: 'LiteFlowChat',
  data() {
    return {
      sessions: [],
      sessionLoading: false,
      currentSessionId: null,
      messages: [],
      input: '',
      sending: false,
      sidebarCollapsed: localStorage.getItem(SIDEBAR_KEY) === '1',
      streamCtl: null,
      modelHint: '使用「模型配置」默认模型',
      suggestions: [
        'LiteFlow THEN 和 WHEN 有什么区别？',
        '如何配置 DeepSeek 模型 Key？',
        '链路试跑失败怎么排查？'
      ]
    }
  },
  created() {
    this.loadSessions()
  },
  beforeDestroy() {
    this.handleStop(true)
  },
  methods: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(SIDEBAR_KEY, this.sidebarCollapsed ? '1' : '0')
    },
    escapeHtml(text) {
      return String(text == null ? '' : text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
    },
    renderMarkdown(raw, streaming) {
      if (!raw) {
        return streaming ? '<span class="md-placeholder">正在思考…</span>' : ''
      }
      let text = this.escapeHtml(raw)
      const blocks = []
      text = text.replace(/```([\s\S]*?)```/g, (_, code) => {
        const i = blocks.length
        blocks.push('<pre class="md-code"><code>' + code.trim() + '</code></pre>')
        return '\u0000BLOCK' + i + '\u0000'
      })
      text = text.replace(/`([^`\n]+)`/g, '<code class="md-inline">$1</code>')
      text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      text = text.replace(/^### (.+)$/gm, '<h4 class="md-h">$1</h4>')
      text = text.replace(/^## (.+)$/gm, '<h3 class="md-h">$1</h3>')
      text = text.replace(/^# (.+)$/gm, '<h3 class="md-h">$1</h3>')
      text = text.replace(/^\s*[-*] (.+)$/gm, '<li>$1</li>')
      text = text.replace(/^\s*\d+\. (.+)$/gm, '<li>$1</li>')
      text = text.replace(/(<li>[\s\S]*?<\/li>)(?:\n<li>[\s\S]*?<\/li>)*/g, m => '<ul class="md-list">' + m + '</ul>')
      text = text.replace(/\n{2,}/g, '</p><p class="md-p">')
      text = text.replace(/\n/g, '<br>')
      text = '<p class="md-p">' + text + '</p>'
      text = text.replace(/\u0000BLOCK(\d+)\u0000/g, (_, i) => blocks[Number(i)])
      return text
    },
    parseTime(value) {
      if (!value) return null
      if (value instanceof Date) return value
      if (typeof value === 'number') return new Date(value)
      const s = String(value).replace(/-/g, '/')
      const d = new Date(s)
      return isNaN(d.getTime()) ? null : d
    },
    pad(n) {
      return n < 10 ? '0' + n : '' + n
    },
    formatClock(d) {
      return this.pad(d.getHours()) + ':' + this.pad(d.getMinutes())
    },
    formatSessionTime(value) {
      const d = this.parseTime(value)
      if (!d) return ''
      const now = new Date()
      const sameDay = d.toDateString() === now.toDateString()
      if (sameDay) return this.formatClock(d)
      return (d.getMonth() + 1) + '/' + d.getDate()
    },
    formatMsgTime(msg) {
      const d = this.parseTime(msg && (msg.createTime || msg._localTime))
      return d ? this.formatClock(d) : ''
    },
    copyText(text) {
      if (!text) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => this.$modal.msgSuccess('已复制'))
        return
      }
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
      this.$modal.msgSuccess('已复制')
    },
    loadSessions() {
      this.sessionLoading = true
      listChatSessions().then(res => {
        this.sessions = res.data || []
      }).finally(() => {
        this.sessionLoading = false
      })
    },
    handleNewSession() {
      if (this.sending) {
        this.handleStop()
      }
      this.currentSessionId = null
      this.messages = []
      this.modelHint = '使用「模型配置」默认模型'
      this.$nextTick(() => this.scrollBottom())
    },
    selectSession(item) {
      if (this.sending) {
        this.$modal.msgWarning('请先停止当前生成')
        return
      }
      this.currentSessionId = item.id
      this.modelHint = item.modelName || item.modelCode || '使用「模型配置」默认模型'
      listChatMessages(item.id).then(res => {
        this.messages = res.data || []
        this.$nextTick(() => this.scrollBottom())
      })
    },
    handleDeleteSession(item) {
      this.$modal.confirm('确认删除会话「' + (item.title || '新对话') + '」？').then(() => {
        return deleteChatSessions(item.id)
      }).then(() => {
        this.$modal.msgSuccess('已删除')
        if (this.currentSessionId === item.id) {
          this.handleNewSession()
        }
        this.loadSessions()
      }).catch(() => {})
    },
    applySuggestion(tip) {
      this.input = tip
      this.handleSend()
    },
    onKeydown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        if (!this.sending) {
          this.handleSend()
        }
      }
    },
    handleStop(silent) {
      if (this.streamCtl && this.streamCtl.controller) {
        this.streamCtl.controller.abort()
      }
      this.streamCtl = null
      if (this.sending) {
        const last = this.messages[this.messages.length - 1]
        if (last && last.role === 'assistant' && last.streaming) {
          last.streaming = false
          last.stopped = true
          if (!last.content) {
            last.content = '（已停止生成）'
          }
        }
        this.sending = false
        if (!silent) {
          this.loadSessions()
        }
      }
    },
    handleSend() {
      const content = (this.input || '').trim()
      if (!content || this.sending) return

      const now = new Date()
      this.sending = true
      this.input = ''
      this.messages.push({ role: 'user', content, _localTime: now })
      const assistant = { role: 'assistant', content: '', streaming: true, _localTime: now }
      this.messages.push(assistant)
      this.$nextTick(() => this.scrollBottom())

      this.streamCtl = chatStream(
        { sessionId: this.currentSessionId, content },
        {
          onEvent: ({ type, payload }) => {
            if (type === 'delta' && payload && payload.text) {
              assistant.content += payload.text
              this.$forceUpdate()
              this.$nextTick(() => this.scrollBottom())
            }
          },
          onDone: (payload) => {
            if (payload) {
              assistant.content = payload.content || assistant.content
              assistant.id = payload.messageId
              assistant.createTime = payload.createTime || assistant._localTime
              this.currentSessionId = payload.sessionId || this.currentSessionId
              if (payload.model) {
                this.modelHint = payload.model
              }
            }
            assistant.streaming = false
            this.sending = false
            this.streamCtl = null
            this.loadSessions()
            this.$nextTick(() => this.scrollBottom())
          },
          onError: (err) => {
            const msg = (err && err.message) || '发送失败'
            if (!assistant.content) {
              assistant.content = '错误：' + msg
            } else {
              this.$modal.msgError(msg)
            }
            assistant.streaming = false
            this.sending = false
            this.streamCtl = null
            this.loadSessions()
          }
        }
      )

      this.streamCtl.done.then(result => {
        if (result && result.aborted) {
          // handleStop 已处理 UI
          return
        }
      }).catch(() => {
        this.sending = false
        this.streamCtl = null
      })
    },
    scrollBottom() {
      const box = this.$refs.messageBox
      if (box) {
        box.scrollTop = box.scrollHeight
      }
    }
  }
}
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 110px);
  min-height: 560px;
  display: flex;
  flex-direction: column;
  padding-bottom: 8px;
}
.chat-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

/* —— 折叠窄条 —— */
.chat-rail {
  width: 52px;
  border-right: 1px solid #eef1f6;
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
}
.rail-btn {
  width: 34px;
  height: 34px;
  border: 1px solid #e4eaf2;
  border-radius: 8px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s, color 0.15s, background 0.15s;
}
.rail-btn:hover {
  border-color: #b3d8ff;
  color: #409eff;
}
.rail-btn.primary {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.rail-btn.primary:hover {
  background: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}

/* —— 侧栏 —— */
.chat-sidebar {
  width: 268px;
  border-right: 1px solid #eef1f6;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f7f9fc 0%, #f3f5f9 100%);
  transition: width 0.2s ease;
}
.chat-sidebar__header {
  padding: 14px 14px 12px;
  border-bottom: 1px solid #e8edf4;
}
.chat-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.brand-text {
  flex: 1;
  min-width: 0;
}
.collapse-btn {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #909399;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.collapse-btn:hover {
  background: #e8eef6;
  color: #409eff;
}
.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.brand-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2a37;
  line-height: 1.2;
}
.brand-sub {
  font-size: 12px;
  color: #8a94a6;
  margin-top: 2px;
}
.btn-new {
  width: 100%;
}
.chat-sidebar__list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.session-item {
  padding: 11px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
}
.session-item:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: #e4eaf2;
}
.session-item.active {
  background: #fff;
  border-color: #c6e2ff;
  box-shadow: 0 1px 4px rgba(64, 158, 255, 0.12);
}
.session-item__title {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-item__meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.session-time {
  margin-left: auto;
  font-size: 11px;
  color: #a0a7b4;
}
.session-item__meta .el-icon-delete {
  opacity: 0;
  cursor: pointer;
  color: #909399;
  padding: 2px;
  flex-shrink: 0;
}
.session-item:hover .el-icon-delete,
.session-item.active .el-icon-delete {
  opacity: 1;
}
.session-item__meta .el-icon-delete:hover {
  color: #f56c6c;
}
.chat-empty {
  padding: 36px 12px;
  text-align: center;
  color: #909399;
}
.chat-empty i {
  font-size: 28px;
  color: #c0c4cc;
}
.chat-empty p {
  margin: 8px 0 4px;
  font-size: 13px;
  color: #606266;
}
.chat-empty span {
  font-size: 12px;
}

/* —— 主区 —— */
.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
}
.chat-main__tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  font-size: 12px;
  color: #6b7785;
  background: #f0f5ff;
  border-bottom: 1px solid #e4ecf8;
}
.chat-main__tip i {
  color: #409eff;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0 8px;
}
.chat-messages__inner {
  width: min(820px, 100%);
  margin: 0 auto;
  padding: 0 24px 12px;
}

.chat-welcome {
  text-align: center;
  padding: 56px 12px 32px;
  color: #606266;
}
.welcome-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  margin-bottom: 12px;
  letter-spacing: 0.02em;
}
.chat-welcome h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #1f2a37;
  font-weight: 650;
  letter-spacing: -0.02em;
}
.chat-welcome p {
  margin: 0 0 24px;
  font-size: 14px;
  color: #7a8494;
}
.chat-suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  max-width: 720px;
  margin: 0 auto;
  text-align: left;
}
.suggest-card {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  background: #fff;
  color: #303133;
  font-size: 13px;
  line-height: 1.45;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}
.suggest-card i {
  color: #409eff;
  margin-top: 2px;
}
.suggest-card:hover {
  border-color: #b3d8ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  transform: translateY(-1px);
}

.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}
.msg-row.role-user {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: 600;
  margin-top: 18px;
}
.msg-avatar.assistant {
  background: linear-gradient(145deg, #409eff, #337ecc);
  font-size: 16px;
}
.msg-avatar.user {
  background: #67c23a;
}
.msg-body {
  max-width: 78%;
  min-width: 0;
}
.msg-row.role-user .msg-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  padding: 0 2px;
}
.msg-row.role-user .msg-meta {
  flex-direction: row-reverse;
}
.msg-role-label {
  font-size: 12px;
  color: #8a94a6;
  font-weight: 500;
}
.msg-time {
  font-size: 11px;
  color: #b0b7c3;
}
.msg-stopped {
  font-size: 11px;
  color: #e6a23c;
}
.msg-bubble {
  position: relative;
  border-radius: 12px;
  padding: 12px 14px;
  line-height: 1.65;
}
.msg-row.role-assistant .msg-bubble {
  background: #fff;
  border: 1px solid #e8edf4;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.msg-row.role-user .msg-bubble {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  color: #1f2a37;
}
.msg-bubble.streaming {
  min-width: 72px;
}
.msg-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  color: #303133;
}
.msg-actions {
  margin-top: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}
.msg-row:hover .msg-actions {
  opacity: 1;
}
.typing-cursor {
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  vertical-align: -2px;
  background: #409eff;
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  50% { opacity: 0; }
}

.msg-md {
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}
.msg-md >>> .md-p {
  margin: 0 0 8px;
}
.msg-md >>> .md-p:last-child {
  margin-bottom: 0;
}
.msg-md >>> .md-h {
  margin: 12px 0 8px;
  font-size: 15px;
  font-weight: 650;
  color: #1f2a37;
}
.msg-md >>> .md-list {
  margin: 6px 0 8px;
  padding-left: 1.25em;
}
.msg-md >>> .md-list li {
  margin: 3px 0;
}
.msg-md >>> .md-inline {
  padding: 1px 5px;
  border-radius: 4px;
  background: #f2f4f7;
  font-family: Consolas, Monaco, monospace;
  font-size: 12.5px;
  color: #c7254e;
}
.msg-md >>> .md-code {
  margin: 8px 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #1e2430;
  color: #e8ecf1;
  overflow-x: auto;
  font-size: 12.5px;
  line-height: 1.5;
}
.msg-md >>> .md-code code {
  font-family: Consolas, Monaco, monospace;
  white-space: pre;
}
.msg-md >>> .md-placeholder {
  color: #909399;
}

.chat-composer {
  padding: 8px 24px 16px;
  background: linear-gradient(180deg, rgba(250, 251, 252, 0) 0%, #fafbfc 28%);
}
.composer-card {
  width: min(820px, 100%);
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e4eaf2;
  border-radius: 14px;
  padding: 10px 12px 10px;
  box-shadow: 0 6px 18px rgba(16, 24, 40, 0.06);
}
.composer-card >>> .el-textarea__inner {
  border: none;
  box-shadow: none;
  padding: 6px 4px;
  background: transparent;
  font-size: 14px;
  line-height: 1.55;
}
.composer-card >>> .el-textarea__inner:focus {
  border: none;
  box-shadow: none;
}
.composer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 6px;
  border-top: 1px solid #f0f2f5;
  margin-top: 4px;
}
.composer-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.composer-meta .el-tag {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.composer-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.sending-hint {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 900px) {
  .chat-sidebar {
    width: 210px;
  }
  .msg-body {
    max-width: 88%;
  }
  .chat-messages__inner {
    padding-left: 12px;
    padding-right: 12px;
  }
  .chat-composer {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>

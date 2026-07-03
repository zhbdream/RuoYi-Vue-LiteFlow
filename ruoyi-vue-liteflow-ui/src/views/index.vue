<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-content">
        <div class="hero-badge">LiteFlow 2.16 · AntV X6</div>
        <h1 class="hero-title">RuoYi-Vue-LiteFlow</h1>
        <p class="hero-subtitle">开箱即用的 Java 业务编排中台</p>
        <p class="hero-desc">
          拖拽画流程、EL 双向同步、规则热更新 —— 若依权限后台 + LiteFlow 规则引擎 + 可视化编排，覆盖 THEN / IF / SWITCH / WHEN / FOR / CATCH 全算子。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="medium" icon="el-icon-s-operation" @click="goEditor('helloChain')">打开编排器</el-button>
          <el-button size="medium" icon="el-icon-connection" @click="goTarget('/liteflow/chain')">链路管理</el-button>
        </div>
      </div>
      <div class="hero-visual">
        <div class="flow-preview">
          <div class="flow-node start">init</div>
          <div class="flow-arrow">↓</div>
          <div class="flow-node logic">IF</div>
          <div class="flow-branches">
            <div class="flow-branch">
              <span class="branch-line"></span>
              <div class="flow-node comp">nodeA</div>
            </div>
            <div class="flow-branch">
              <span class="branch-line alt"></span>
              <div class="flow-node comp">nodeB</div>
            </div>
          </div>
          <div class="flow-arrow">↓</div>
          <div class="flow-node end">complete</div>
        </div>
      </div>
    </section>

    <!-- Quick entries -->
    <section class="section">
      <h2 class="section-title">快速入口</h2>
      <el-row :gutter="16">
        <el-col v-for="item in quickLinks" :key="item.path" :xs="12" :sm="8" :md="6" :lg="4">
          <div class="quick-card" :style="{ '--accent': item.color }" @click="goTarget(item.path)">
            <div class="quick-icon"><i :class="item.icon"></i></div>
            <div class="quick-label">{{ item.label }}</div>
            <div class="quick-tip">{{ item.tip }}</div>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- Capabilities -->
    <section class="section">
      <h2 class="section-title">核心能力</h2>
      <el-row :gutter="16">
        <el-col v-for="cap in capabilities" :key="cap.title" :xs="24" :sm="12" :md="8">
          <div class="cap-card">
            <i :class="cap.icon" class="cap-icon"></i>
            <h3>{{ cap.title }}</h3>
            <p>{{ cap.desc }}</p>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- Demos -->
    <section class="section">
      <div class="section-head">
        <h2 class="section-title">内置 Demo 矩阵</h2>
        <span class="section-extra">链路管理 → 试跑 / 编排</span>
      </div>
      <el-row :gutter="12">
        <el-col v-for="demo in demos" :key="demo.name" :xs="24" :sm="12" :md="8" :lg="6">
          <div class="demo-card" @click="goEditor(demo.name)">
            <div class="demo-head">
              <span class="demo-name">{{ demo.name }}</span>
              <el-tag size="mini" :type="demo.tagType" effect="plain">{{ demo.tag }}</el-tag>
            </div>
            <p class="demo-desc">{{ demo.desc }}</p>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- Docs -->
    <section class="section section-last">
      <h2 class="section-title">文档</h2>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8" v-for="doc in docs" :key="doc.name">
          <div class="doc-card">
            <i :class="doc.icon"></i>
            <div>
              <div class="doc-name">{{ doc.name }}</div>
              <div class="doc-path">{{ doc.path }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script>
export default {
  name: 'Index',
  data() {
    return {
      quickLinks: [
        { label: '链路管理', tip: 'CRUD / 发布 / 克隆', path: '/liteflow/chain', icon: 'el-icon-connection', color: '#409EFF' },
        { label: '可视化编排', tip: 'X6 拖拽画流程', path: '/liteflow/editor', icon: 'el-icon-s-operation', color: '#67C23A' },
        { label: '脚本管理', tip: 'Groovy / QLExpress', path: '/liteflow/script', icon: 'el-icon-document', color: '#E6A23C' },
        { label: '组件中心', tip: '扫描 / 引用分析', path: '/liteflow/component', icon: 'el-icon-cpu', color: '#909399' },
        { label: '执行日志', tip: '步骤 / 失败定位', path: '/liteflow/log', icon: 'el-icon-tickets', color: '#F56C6C' },
        { label: '监控仪表盘', tip: '成功率 / Top N', path: '/liteflow/dashboard', icon: 'el-icon-data-line', color: '#13C2C2' },
        { label: 'Swagger', tip: 'API 在线调试', path: '/tool/swagger/index', icon: 'el-icon-link', color: '#9C27B0' }
      ],
      capabilities: [
        { title: '可视化编排', icon: 'el-icon-s-grid', desc: 'AntV X6 画布，组件拖拽 + 逻辑容器，EL 实时预览与双向同步。' },
        { title: '规则生命周期', icon: 'el-icon-refresh', desc: '草稿 / 发布、版本快照、克隆导入导出、规则变更审计。' },
        { title: '开放集成', icon: 'el-icon-share', desc: '开放执行 API、编排器试跑、执行日志 requestId 全链路追踪。' }
      ],
      demos: [
        { name: 'helloChain', tag: 'THEN', tagType: '', desc: '三节点串行入门' },
        { name: 'orderProcess', tag: 'IF·SWITCH', tagType: 'success', desc: '订单处理主打 Demo' },
        { name: 'dynamicPricing', tag: '脚本', tagType: 'warning', desc: '动态定价 + Groovy' },
        { name: 'parallelAudit', tag: 'WHEN', tagType: 'info', desc: '并行校验 + 超时' },
        { name: 'resilientNotify', tag: 'CATCH', tagType: 'danger', desc: '异常重试与降级' },
        { name: 'batchProcess', tag: 'FOR', tagType: '', desc: '批量循环处理' },
        { name: 'routeDemo', tag: 'Route', tagType: 'warning', desc: '决策路由 namespace=routeDemo' }
      ],
      docs: [
        { name: '编排器指南', path: 'docs/EDITOR.md', icon: 'el-icon-reading' },
        { name: 'API 文档', path: 'docs/API.md', icon: 'el-icon-link' },
        { name: 'GitHub README', path: 'README.md', icon: 'el-icon-document' }
      ]
    }
  },
  methods: {
    goTarget(path) {
      this.$router.push({ path })
    },
    goEditor(chainName) {
      this.$router.push({ path: '/liteflow/editor', query: { chainId: chainName } })
    }
  }
}
</script>

<style scoped lang="scss">
.home {
  padding: 0 8px 24px;
  background: #f5f7fb;
  min-height: calc(100vh - 84px);
}

/* Hero */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding: 36px 40px;
  margin-bottom: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1a2a4a 0%, #2d5a8e 45%, #409eff 100%);
  color: #fff;
  overflow: hidden;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    right: -60px;
    top: -60px;
    width: 280px;
    height: 280px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
  }
}

.hero-content {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  font-size: 12px;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.hero-title {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.hero-subtitle {
  margin: 0 0 12px;
  font-size: 16px;
  opacity: 0.9;
  font-weight: 500;
}

.hero-desc {
  margin: 0 0 20px;
  max-width: 520px;
  line-height: 1.75;
  font-size: 14px;
  opacity: 0.82;
}

.hero-actions .el-button + .el-button {
  margin-left: 10px;
}

.hero-visual {
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.flow-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 28px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
}

.flow-node {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  min-width: 64px;

  &.start, &.end { background: rgba(64, 158, 255, 0.5); }
  &.logic { background: rgba(230, 162, 60, 0.6); }
  &.comp { background: rgba(103, 194, 58, 0.5); font-size: 11px; }
}

.flow-arrow {
  font-size: 14px;
  opacity: 0.6;
  line-height: 1.4;
}

.flow-branches {
  display: flex;
  gap: 24px;
  margin: 4px 0;
}

.flow-branch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.branch-line {
  width: 2px;
  height: 16px;
  background: #67c23a;
  border-radius: 1px;
  &.alt { background: #f56c6c; }
}

/* Sections */
.section {
  margin-bottom: 24px;
}

.section-last {
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;

  .section-title { margin-bottom: 0; }
}

.section-extra {
  font-size: 13px;
  color: #909399;
}

/* Quick cards */
.quick-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 14px;
  margin-bottom: 16px;
  cursor: pointer;
  border: 1px solid #ebeef5;
  transition: all 0.22s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    border-color: var(--accent);
  }
}

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #f4f6f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;

  i {
    font-size: 20px;
    color: var(--accent);
  }
}

.quick-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.quick-tip {
  font-size: 12px;
  color: #909399;
}

/* Capability cards */
.cap-card {
  background: #fff;
  border-radius: 10px;
  padding: 22px 20px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  height: calc(100% - 16px);

  h3 {
    margin: 10px 0 8px;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #606266;
    line-height: 1.7;
  }
}

.cap-icon {
  font-size: 28px;
  color: #409eff;
}

/* Demo cards */
.demo-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
  }
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.demo-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  font-family: Consolas, 'Courier New', monospace;
}

.demo-desc {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

/* Doc cards */
.doc-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;

  i {
    font-size: 24px;
    color: #409eff;
  }
}

.doc-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.doc-path {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  font-family: Consolas, monospace;
}

@media (max-width: 992px) {
  .hero {
    flex-direction: column;
    padding: 28px 24px;
  }
  .hero-visual {
    width: 100%;
    display: flex;
    justify-content: center;
  }
}
</style>

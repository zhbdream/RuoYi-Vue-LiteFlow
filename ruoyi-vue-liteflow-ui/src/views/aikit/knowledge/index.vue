<template>
  <div class="app-container">
    <el-alert title="支持上传 txt / md / pdf / docx。分片落库；进程启动后自动从分片重建内存索引。默认本地 All-MiniLM，可配远程 Embedding。" type="info" :closable="false" show-icon class="mb8" />
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="编码" prop="kbCode">
        <el-input v-model="queryParams.kbCode" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['aikit:knowledge:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="编码" prop="kbCode" width="140" />
      <el-table-column label="名称" prop="kbName" min-width="120" />
      <el-table-column label="分片数" prop="chunkCount" width="80" />
      <el-table-column label="状态" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" prop="description" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="380" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="openDocs(scope.row)" v-hasPermi="['aikit:knowledge:query']">文档</el-button>
          <el-button size="mini" type="text" @click="handleUpload(scope.row)" v-hasPermi="['aikit:knowledge:upload']">上传</el-button>
          <el-button size="mini" type="text" @click="handleReindex(scope.row)" v-hasPermi="['aikit:knowledge:reindex']">重建索引</el-button>
          <el-button size="mini" type="text" @click="openSearch(scope.row)" v-hasPermi="['aikit:knowledge:query']">试检索</el-button>
          <el-button size="mini" type="text" @click="handleUpdate(scope.row)" v-hasPermi="['aikit:knowledge:edit']">编辑</el-button>
          <el-button size="mini" type="text" @click="handleDelete(scope.row)" v-hasPermi="['aikit:knowledge:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="编码" prop="kbCode">
          <el-input v-model="form.kbCode" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="名称" prop="kbName">
          <el-input v-model="form.kbName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="文档列表" :visible.sync="docsOpen" width="720px" append-to-body>
      <el-table :data="docs" size="small">
        <el-table-column label="文档名" prop="docName" min-width="160" />
        <el-table-column label="分片" prop="chunkCount" width="70" />
        <el-table-column label="状态" prop="status" width="80">
          <template slot-scope="scope">{{ scope.row.status === '1' ? '已索引' : (scope.row.status === '2' ? '失败' : '待索引') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="removeDoc(scope.row)" v-hasPermi="['aikit:knowledge:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog title="上传文档" :visible.sync="uploadOpen" width="480px" append-to-body>
      <el-upload drag action="#" :http-request="doUpload" :show-file-list="false" accept=".txt,.md,.markdown,.pdf,.docx">
        <i class="el-icon-upload" />
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">支持 txt / md / pdf / docx</div>
      </el-upload>
    </el-dialog>

    <el-dialog title="试检索" :visible.sync="searchOpen" width="680px" append-to-body>
      <el-form size="small" label-width="80px">
        <el-form-item label="知识库">{{ searchForm.kbCode }}</el-form-item>
        <el-form-item label="问题">
          <el-input v-model="searchForm.query" type="textarea" :rows="2" placeholder="如：七天无理由怎么退？" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="searchLoading" @click="doSearch">检 索</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="searchHits" size="small" max-height="320">
        <el-table-column label="来源" prop="source" width="140" show-overflow-tooltip />
        <el-table-column label="分数" prop="score" width="90">
          <template slot-scope="scope">{{ Number(scope.row.score).toFixed(3) }}</template>
        </el-table-column>
        <el-table-column label="片段" prop="text" min-width="280" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import {
  listAiKnowledge, getAiKnowledge, addAiKnowledge, updateAiKnowledge, delAiKnowledge,
  listAiKnowledgeDocs, uploadAiKnowledgeDoc, delAiKnowledgeDocs, reindexAiKnowledge, searchAiKnowledge
} from '@/api/aikit/platform'

export default {
  name: 'Knowledge',
  data() {
    return {
      loading: false,
      showSearch: true,
      total: 0,
      list: [],
      open: false,
      title: '',
      queryParams: { pageNum: 1, pageSize: 10, kbCode: undefined },
      form: {},
      rules: {
        kbCode: [{ required: true, message: '编码不能为空', trigger: 'blur' }]
      },
      docsOpen: false,
      docs: [],
      currentKbId: null,
      uploadOpen: false,
      searchOpen: false,
      searchLoading: false,
      searchHits: [],
      searchForm: { kbCode: '', query: '七天无理由怎么退？' }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAiKnowledge(this.queryParams).then(res => {
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
    reset() {
      this.form = { id: undefined, kbCode: undefined, kbName: undefined, description: undefined, status: '0' }
      this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增知识库'
    },
    handleUpdate(row) {
      this.reset()
      getAiKnowledge(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改知识库'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const req = this.form.id != null ? updateAiKnowledge(this.form) : addAiKnowledge(this.form)
        req.then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('是否确认删除知识库？').then(() => delAiKnowledge(row.id)).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    openDocs(row) {
      this.currentKbId = row.id
      listAiKnowledgeDocs(row.id).then(res => {
        this.docs = res.data || []
        this.docsOpen = true
      })
    },
    handleUpload(row) {
      this.currentKbId = row.id
      this.uploadOpen = true
    },
    doUpload(option) {
      uploadAiKnowledgeDoc(this.currentKbId, option.file).then(() => {
        this.$modal.msgSuccess('上传并索引成功')
        this.uploadOpen = false
        this.getList()
      }).catch(() => {})
    },
    removeDoc(row) {
      this.$modal.confirm('是否删除该文档？').then(() => delAiKnowledgeDocs(row.id)).then(() => {
        this.$modal.msgSuccess('已删除')
        this.openDocs({ id: this.currentKbId })
        this.getList()
      }).catch(() => {})
    },
    handleReindex(row) {
      this.$modal.loading('重建索引中...')
      reindexAiKnowledge(row.id).then(res => {
        this.$modal.closeLoading()
        this.$modal.msgSuccess('完成，分片数=' + ((res.data && res.data.chunkCount) || 0))
        this.getList()
      }).catch(() => { this.$modal.closeLoading() })
    },
    openSearch(row) {
      this.searchForm = { kbCode: row.kbCode, query: '七天无理由怎么退？' }
      this.searchHits = []
      this.searchOpen = true
    },
    doSearch() {
      if (!this.searchForm.query) {
        this.$modal.msgWarning('请输入问题')
        return
      }
      this.searchLoading = true
      searchAiKnowledge(this.searchForm.kbCode, { query: this.searchForm.query, maxResults: 5, minScore: 0.2 }).then(res => {
        this.searchHits = res.data || []
        this.searchLoading = false
        if (!this.searchHits.length) this.$modal.msgWarning('无命中，可先重建索引或降低分数阈值')
      }).catch(() => { this.searchLoading = false })
    }
  }
}
</script>

<template>
  <div class="app-container">
    <el-alert title="支持上传 txt/md；上传或重建索引后会切分并载入内存向量库，供绑定该知识库的智能体检索。" type="info" :closable="false" show-icon class="mb8" />
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
      <el-table-column label="操作" width="320" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="openDocs(scope.row)" v-hasPermi="['aikit:knowledge:query']">文档</el-button>
          <el-button size="mini" type="text" @click="handleUpload(scope.row)" v-hasPermi="['aikit:knowledge:upload']">上传</el-button>
          <el-button size="mini" type="text" @click="handleReindex(scope.row)" v-hasPermi="['aikit:knowledge:reindex']">重建索引</el-button>
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
      <el-upload drag action="#" :http-request="doUpload" :show-file-list="false" accept=".txt,.md,.markdown">
        <i class="el-icon-upload" />
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">仅支持 txt / md</div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script>
import {
  listAiKnowledge, getAiKnowledge, addAiKnowledge, updateAiKnowledge, delAiKnowledge,
  listAiKnowledgeDocs, uploadAiKnowledgeDoc, delAiKnowledgeDocs, reindexAiKnowledge
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
      uploadOpen: false
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
    }
  }
}
</script>

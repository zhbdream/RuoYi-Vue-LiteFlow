<template>
  <div class="app-container">
    <el-alert
      title="链路级权限：配置后仅指定角色可执行/编排；未配置时仍按菜单权限（liteflow:execute / liteflow:chain:edit）控制。"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <el-form :inline="true" size="small" label-width="72px">
      <el-form-item label="链路ID">
        <el-select v-model="selectedChain" filterable placeholder="选择链路" style="width: 240px" @change="loadPermissions">
          <el-option v-for="c in chainOptions" :key="c.chainName" :label="c.chainName" :value="c.chainName" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" :disabled="!selectedChain" @click="loadPermissions">加载</el-button>
        <el-button type="success" icon="el-icon-check" :disabled="!selectedChain" @click="submitPermissions" v-hasPermi="['liteflow:chain:permission']">保存</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="roleRows" border size="small">
      <el-table-column label="角色ID" prop="roleId" width="90" align="center" />
      <el-table-column label="角色名称" prop="roleName" min-width="140" />
      <el-table-column label="可执行" width="100" align="center">
        <template slot-scope="scope">
          <el-checkbox v-model="scope.row.canExecute" true-label="1" false-label="0" />
        </template>
      </el-table-column>
      <el-table-column label="可编排" width="100" align="center">
        <template slot-scope="scope">
          <el-checkbox v-model="scope.row.canEdit" true-label="1" false-label="0" />
        </template>
      </el-table-column>
    </el-table>
    <p class="perm-tip">勾选「可执行」允许试跑/内部执行；勾选「可编排」允许保存草稿、发布、编排器保存。</p>
  </div>
</template>

<script>
import { listChain, listChainPermission, saveChainPermission } from '@/api/liteflow/chain'
import { listRole } from '@/api/system/role'

export default {
  name: 'LfChainPermission',
  data() {
    return {
      loading: false,
      selectedChain: '',
      chainOptions: [],
      roleRows: []
    }
  },
  created() {
    this.initPage()
  },
  methods: {
    initPage() {
      Promise.all([
        listChain({ pageNum: 1, pageSize: 500 }),
        listRole({ pageNum: 1, pageSize: 200, status: '0' })
      ]).then(([chainRes, roleRes]) => {
        this.chainOptions = chainRes.rows || []
        const roles = roleRes.rows || []
        this.roleRows = roles.map(r => ({
          roleId: r.roleId,
          roleName: r.roleName,
          canExecute: '0',
          canEdit: '0'
        }))
        const q = this.$route.query.chainName
        if (q) {
          this.selectedChain = q
          this.loadPermissions()
        }
      })
    },
    loadPermissions() {
      if (!this.selectedChain || !this.roleRows.length) {
        return
      }
      this.loading = true
      listChainPermission(this.selectedChain).then(res => {
        const map = {}
        ;(res.data || []).forEach(item => {
          map[item.roleId] = item
        })
        this.roleRows = this.roleRows.map(row => {
          const hit = map[row.roleId]
          return {
            ...row,
            canExecute: hit && hit.canExecute === '1' ? '1' : '0',
            canEdit: hit && hit.canEdit === '1' ? '1' : '0'
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    submitPermissions() {
      const permissions = this.roleRows
        .filter(row => row.canExecute === '1' || row.canEdit === '1')
        .map(row => ({
          roleId: row.roleId,
          canExecute: row.canExecute,
          canEdit: row.canEdit
        }))
      saveChainPermission({
        chainName: this.selectedChain,
        permissions
      }).then(() => {
        this.$modal.msgSuccess('权限已保存')
        this.loadPermissions()
      })
    }
  }
}
</script>

<style scoped>
.perm-tip {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
}
</style>

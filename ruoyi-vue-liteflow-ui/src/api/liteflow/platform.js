import request from '@/utils/request'

export function getLiteFlowConfig() {
  return request({
    url: '/liteflow/config',
    method: 'get'
  })
}

export function listScript(query) {
  return request({
    url: '/liteflow/script/list',
    method: 'get',
    params: query
  })
}

export function getScript(id) {
  return request({
    url: '/liteflow/script/' + id,
    method: 'get'
  })
}

export function addScript(data) {
  return request({
    url: '/liteflow/script',
    method: 'post',
    data: data
  })
}

export function updateScript(data) {
  return request({
    url: '/liteflow/script',
    method: 'put',
    data: data
  })
}

export function delScript(id) {
  return request({
    url: '/liteflow/script/' + id,
    method: 'delete'
  })
}

export function validateScript(data) {
  return request({
    url: '/liteflow/script/validate',
    method: 'post',
    data: data
  })
}

export function getScriptRefs(scriptId) {
  return request({
    url: '/liteflow/script/refs/' + scriptId,
    method: 'get'
  })
}

export function listScriptVersions(scriptPk) {
  return request({
    url: '/liteflow/script/versions/' + scriptPk,
    method: 'get'
  })
}

export function getScriptVersion(id) {
  return request({
    url: '/liteflow/script/version/' + id,
    method: 'get'
  })
}

export function generateComponentScaffold(data) {
  return request({
    url: '/liteflow/component/scaffold',
    method: 'post',
    data: data
  })
}

export function listExecLog(query) {
  return request({
    url: '/liteflow/log/list',
    method: 'get',
    params: query
  })
}

export function getExecLog(id) {
  return request({
    url: '/liteflow/log/' + id,
    method: 'get'
  })
}

export function delExecLog(id) {
  return request({
    url: '/liteflow/log/' + id,
    method: 'delete'
  })
}

export function cleanExecLog() {
  return request({
    url: '/liteflow/log/clean',
    method: 'delete'
  })
}

export function listComponentCenter() {
  return request({
    url: '/liteflow/component/center',
    method: 'get'
  })
}

export function getComponentRefs(nodeId) {
  return request({
    url: '/liteflow/component/refs/' + nodeId,
    method: 'get'
  })
}

export function listChainAudit(query) {
  return request({
    url: '/liteflow/audit/list',
    method: 'get',
    params: query
  })
}

export function getChainAudit(id) {
  return request({
    url: '/liteflow/audit/' + id,
    method: 'get'
  })
}

export function getDashboard(days) {
  return request({
    url: '/liteflow/dashboard',
    method: 'get',
    params: { days }
  })
}

export function delChainAudit(id) {
  return request({
    url: '/liteflow/audit/' + id,
    method: 'delete'
  })
}

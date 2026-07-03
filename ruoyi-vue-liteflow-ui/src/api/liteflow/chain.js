import request from '@/utils/request'

export function listChain(query) {
  return request({
    url: '/liteflow/chain/list',
    method: 'get',
    params: query
  })
}

export function getChain(id) {
  return request({
    url: '/liteflow/chain/' + id,
    method: 'get'
  })
}

export function getChainByName(chainName) {
  return request({
    url: '/liteflow/chain/name/' + chainName,
    method: 'get'
  })
}

export function addChain(data) {
  return request({
    url: '/liteflow/chain',
    method: 'post',
    data: data
  })
}

export function updateChain(data) {
  return request({
    url: '/liteflow/chain',
    method: 'put',
    data: data
  })
}

export function delChain(id) {
  return request({
    url: '/liteflow/chain/' + id,
    method: 'delete'
  })
}

export function reloadChain(chainName) {
  return request({
    url: '/liteflow/chain/reload/' + chainName,
    method: 'post'
  })
}

export function listComponent() {
  return request({
    url: '/liteflow/component/list',
    method: 'get'
  })
}

export function executeChain(chainName, data) {
  return request({
    url: '/liteflow/execute/' + chainName,
    method: 'post',
    data: data
  })
}

export function validateEl(elData) {
  return request({
    url: '/liteflow/el/validate',
    method: 'post',
    data: { elData }
  })
}

export function executeEl(data) {
  return request({
    url: '/liteflow/el/execute',
    method: 'post',
    data: data
  })
}

export function executeRoute(data) {
  return request({
    url: '/liteflow/execute/route',
    method: 'post',
    data: data
  })
}

export function publishChain(id) {
  return request({
    url: '/liteflow/chain/publish/' + id,
    method: 'post'
  })
}

export function cloneChain(data) {
  return request({
    url: '/liteflow/chain/clone',
    method: 'post',
    data: data
  })
}

export function exportChain(id) {
  return request({
    url: '/liteflow/chain/export/' + id,
    method: 'get'
  })
}

export function importChain(data) {
  return request({
    url: '/liteflow/chain/import',
    method: 'post',
    data: data
  })
}

export function listChainVersions(query) {
  return request({
    url: '/liteflow/chain/versions/list',
    method: 'get',
    params: query
  })
}

export function listVersionsByChain(chainId) {
  return request({
    url: '/liteflow/chain/versions/chain/' + chainId,
    method: 'get'
  })
}

export function getChainVersion(id) {
  return request({
    url: '/liteflow/chain/versions/' + id,
    method: 'get'
  })
}

export function rollbackChainVersion(id) {
  return request({
    url: '/liteflow/chain/versions/rollback/' + id,
    method: 'post'
  })
}

export function listChainPermission(chainName) {
  return request({
    url: '/liteflow/chain/permission/' + chainName,
    method: 'get'
  })
}

export function saveChainPermission(data) {
  return request({
    url: '/liteflow/chain/permission',
    method: 'post',
    data: data
  })
}

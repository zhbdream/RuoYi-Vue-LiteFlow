import request from '@/utils/request'

export function listAgentModel(query) {
  return request({
    url: '/liteflow/agent/model/list',
    method: 'get',
    params: query
  })
}

export function getAgentModel(id) {
  return request({
    url: '/liteflow/agent/model/' + id,
    method: 'get'
  })
}

export function addAgentModel(data) {
  return request({
    url: '/liteflow/agent/model',
    method: 'post',
    data: data
  })
}

export function updateAgentModel(data) {
  return request({
    url: '/liteflow/agent/model',
    method: 'put',
    data: data
  })
}

export function delAgentModel(id) {
  return request({
    url: '/liteflow/agent/model/' + id,
    method: 'delete'
  })
}

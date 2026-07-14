import request from '@/utils/request'
import { getToken } from '@/utils/auth'

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

/**
 * 流式试跑（SSE）。onEvent({ type, payload })，结束回调 onDone(result) / onError(err)
 */
export function executeChainStream(chainName, data, { onEvent, onDone, onError } = {}) {
  const baseURL = process.env.VUE_APP_BASE_API || ''
  return fetch(baseURL + '/liteflow/execute/stream/' + encodeURIComponent(chainName), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + getToken()
    },
    body: JSON.stringify(data || {})
  }).then(async res => {
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || ('HTTP ' + res.status))
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let currentEvent = 'message'
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        const lines = part.split('\n')
        let eventName = currentEvent
        const dataLines = []
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim())
          }
        }
        if (!dataLines.length) continue
        let payload
        try {
          payload = JSON.parse(dataLines.join('\n'))
        } catch (e) {
          payload = dataLines.join('\n')
        }
        if (eventName === 'done') {
          onDone && onDone(payload)
        } else if (eventName === 'error') {
          onError && onError(payload)
        } else {
          onEvent && onEvent({ type: eventName, payload })
        }
      }
    }
  }).catch(err => {
    onError && onError({ message: err.message || String(err) })
    return Promise.reject(err)
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

import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export function listAiModel(query) {
  return request({ url: '/aikit/model/list', method: 'get', params: query })
}

export function getAiModel(id) {
  return request({ url: '/aikit/model/' + id, method: 'get' })
}

export function addAiModel(data) {
  return request({ url: '/aikit/model', method: 'post', data })
}

export function updateAiModel(data) {
  return request({ url: '/aikit/model', method: 'put', data })
}

export function delAiModel(id) {
  return request({ url: '/aikit/model/' + id, method: 'delete' })
}

export function testAiModel(data) {
  return request({ url: '/aikit/model/test', method: 'post', data })
}

export function getAiModelSources() {
  return request({ url: '/aikit/model/sources', method: 'get' })
}

export function listAiTool(query) {
  return request({ url: '/aikit/tool/list', method: 'get', params: query })
}

export function getAiTool(id) {
  return request({ url: '/aikit/tool/' + id, method: 'get' })
}

export function addAiTool(data) {
  return request({ url: '/aikit/tool', method: 'post', data })
}

export function updateAiTool(data) {
  return request({ url: '/aikit/tool', method: 'put', data })
}

export function delAiTool(id) {
  return request({ url: '/aikit/tool/' + id, method: 'delete' })
}

export function listAiAgent(query) {
  return request({ url: '/aikit/agent/list', method: 'get', params: query })
}

export function getAiAgent(id) {
  return request({ url: '/aikit/agent/' + id, method: 'get' })
}

export function addAiAgent(data) {
  return request({ url: '/aikit/agent', method: 'post', data })
}

export function updateAiAgent(data) {
  return request({ url: '/aikit/agent', method: 'put', data })
}

export function delAiAgent(id) {
  return request({ url: '/aikit/agent/' + id, method: 'delete' })
}

export function listAiAgentLogs(agentCode, query) {
  return request({ url: '/aikit/agent/' + encodeURIComponent(agentCode) + '/logs', method: 'get', params: query })
}

export function runAiAgent(agentCode, data) {
  return request({ url: '/aikit/agent/' + agentCode + '/run', method: 'post', data, timeout: 120000 })
}

/**
 * 流式试跑。事件：delta / tool / done / error
 */
export function streamAiAgent(agentCode, data, { onEvent, onDone, onError } = {}) {
  const baseURL = process.env.VUE_APP_BASE_API || ''
  const controller = new AbortController()
  const done = fetch(baseURL + '/aikit/agent/' + encodeURIComponent(agentCode) + '/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + getToken()
    },
    body: JSON.stringify(data || {}),
    signal: controller.signal
  }).then(async res => {
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || ('HTTP ' + res.status))
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      if (controller.signal.aborted) {
        try { reader.cancel() } catch (e) { /* ignore */ }
        break
      }
      const { done: streamDone, value } = await reader.read()
      if (streamDone) break
      buffer += decoder.decode(value, { stream: true })
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        const lines = part.split('\n')
        let eventName = 'message'
        const dataLines = []
        for (const line of lines) {
          if (line.startsWith('event:')) eventName = line.slice(6).trim()
          else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
        }
        if (!dataLines.length) continue
        let payload
        try { payload = JSON.parse(dataLines.join('\n')) } catch (e) { payload = dataLines.join('\n') }
        if (eventName === 'done') onDone && onDone(payload)
        else if (eventName === 'error') onError && onError(payload)
        else onEvent && onEvent({ type: eventName, payload })
      }
    }
  }).catch(err => {
    if (err && (err.name === 'AbortError' || controller.signal.aborted)) {
      return { aborted: true }
    }
    onError && onError({ message: err.message || String(err) })
    return Promise.reject(err)
  })
  return { abort: () => controller.abort(), done }
}

export function searchAiKnowledge(kbCode, data) {
  return request({ url: '/aikit/knowledge/' + encodeURIComponent(kbCode) + '/search', method: 'post', data })
}

export function listAiKnowledge(query) {
  return request({ url: '/aikit/knowledge/list', method: 'get', params: query })
}

export function getAiKnowledge(id) {
  return request({ url: '/aikit/knowledge/' + id, method: 'get' })
}

export function addAiKnowledge(data) {
  return request({ url: '/aikit/knowledge', method: 'post', data })
}

export function updateAiKnowledge(data) {
  return request({ url: '/aikit/knowledge', method: 'put', data })
}

export function delAiKnowledge(id) {
  return request({ url: '/aikit/knowledge/' + id, method: 'delete' })
}

export function listAiKnowledgeDocs(kbId) {
  return request({ url: '/aikit/knowledge/' + kbId + '/docs', method: 'get' })
}

export function uploadAiKnowledgeDoc(kbId, file) {
  const form = new FormData()
  form.append('file', file)
  return request({
    url: '/aikit/knowledge/' + kbId + '/upload',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function delAiKnowledgeDocs(ids) {
  return request({ url: '/aikit/knowledge/doc/' + ids, method: 'delete' })
}

export function reindexAiKnowledge(kbId) {
  return request({ url: '/aikit/knowledge/' + kbId + '/reindex', method: 'post' })
}

export function listAiSkill(query) {
  return request({ url: '/aikit/skill/list', method: 'get', params: query })
}

export function getAiSkill(id) {
  return request({ url: '/aikit/skill/' + id, method: 'get' })
}

export function addAiSkill(data) {
  return request({ url: '/aikit/skill', method: 'post', data })
}

export function updateAiSkill(data) {
  return request({ url: '/aikit/skill', method: 'put', data })
}

export function delAiSkill(id) {
  return request({ url: '/aikit/skill/' + id, method: 'delete' })
}

export function runAiSkill(skillCode, data) {
  return request({ url: '/aikit/skill/' + encodeURIComponent(skillCode) + '/run', method: 'post', data, timeout: 60000 })
}

export function listAiMemory(query) {
  return request({ url: '/aikit/memory/list', method: 'get', params: query })
}

export function addAiMemory(data) {
  return request({ url: '/aikit/memory', method: 'post', data })
}

export function delAiMemory(id) {
  return request({ url: '/aikit/memory/' + id, method: 'delete' })
}

export function clearAiMemory(data) {
  return request({ url: '/aikit/memory/clear', method: 'post', data })
}

export function purgeAiMemory(days) {
  return request({ url: '/aikit/memory/purge', method: 'post', params: { days } })
}

export function listAiContext(query) {
  return request({ url: '/aikit/context/list', method: 'get', params: query })
}

export function getAiContext(id) {
  return request({ url: '/aikit/context/' + id, method: 'get' })
}

export function addAiContext(data) {
  return request({ url: '/aikit/context', method: 'post', data })
}

export function updateAiContext(data) {
  return request({ url: '/aikit/context', method: 'put', data })
}

export function delAiContext(id) {
  return request({ url: '/aikit/context/' + id, method: 'delete' })
}

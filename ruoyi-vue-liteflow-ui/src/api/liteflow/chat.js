import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export function listChatSessions() {
  return request({
    url: '/liteflow/chat/session/list',
    method: 'get'
  })
}

export function createChatSession(data) {
  return request({
    url: '/liteflow/chat/session',
    method: 'post',
    data: data || {}
  })
}

export function listChatMessages(sessionId) {
  return request({
    url: '/liteflow/chat/session/' + sessionId + '/messages',
    method: 'get'
  })
}

export function deleteChatSessions(ids) {
  return request({
    url: '/liteflow/chat/session/' + ids,
    method: 'delete'
  })
}

/**
 * 流式对话（SSE）。事件：delta / done / error
 * @returns {{ abort: Function, done: Promise }}
 */
export function chatStream(data, { onEvent, onDone, onError, signal } = {}) {
  const baseURL = process.env.VUE_APP_BASE_API || ''
  const controller = signal ? null : new AbortController()
  const abortSignal = signal || controller.signal

  const done = fetch(baseURL + '/liteflow/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + getToken()
    },
    body: JSON.stringify(data || {}),
    signal: abortSignal
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
      if (abortSignal.aborted) {
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
    if (err && (err.name === 'AbortError' || abortSignal.aborted)) {
      return { aborted: true }
    }
    onError && onError({ message: err.message || String(err) })
    return Promise.reject(err)
  })

  return {
    abort: () => {
      if (controller) {
        controller.abort()
      } else if (signal && typeof signal.throwIfAborted !== 'undefined') {
        // caller owns signal
      }
    },
    controller,
    done
  }
}

/**
 * LiteFlow EL 模型：解析 / 构建 THEN、IF、SWITCH、WHEN、FOR、CATCH、RETRY
 */

function splitTopLevel(str) {
  const parts = []
  let depth = 0
  let current = ''
  for (let i = 0; i < str.length; i++) {
    const c = str[i]
    if (c === '(') {
      depth++
    } else if (c === ')') {
      depth--
    } else if (c === ',' && depth === 0) {
      parts.push(current.trim())
      current = ''
      continue
    }
    current += c
  }
  if (current.trim()) {
    parts.push(current.trim())
  }
  return parts
}

function extractParenContent(str, fromIndex) {
  let i = fromIndex
  while (i < str.length && str[i] !== '(') {
    i++
  }
  if (str[i] !== '(') {
    return null
  }
  let depth = 0
  const start = i + 1
  for (; i < str.length; i++) {
    if (str[i] === '(') {
      depth++
    }
    if (str[i] === ')') {
      depth--
      if (depth === 0) {
        return str.substring(start, i)
      }
    }
  }
  return null
}

function parseComponent(str) {
  const trimmed = str.trim()
  const idMatch = trimmed.match(/^([a-zA-Z_]\w*)/)
  if (!idMatch) {
    return { type: 'component', nodeId: trimmed, remark: '' }
  }
  const node = {
    type: 'component',
    nodeId: idMatch[1],
    name: idMatch[1],
    remark: '',
    tag: '',
    dataKey: '',
    dataValue: '',
    bind: ''
  }
  let rest = trimmed.substring(idMatch[0].length)
  const tagM = rest.match(/^\.tag\s*\(\s*"([^"]*)"\s*\)/)
  if (tagM) {
    node.tag = tagM[1]
    rest = rest.substring(tagM[0].length)
  }
  const dataM = rest.match(/^\.data\s*\(\s*"([^"]*)"\s*,\s*"([^"]*)"\s*\)/)
  if (dataM) {
    node.dataKey = dataM[1]
    node.dataValue = dataM[2]
    rest = rest.substring(dataM[0].length)
  }
  const bindM = rest.match(/^\.bind\s*\(\s*"([^"]*)"\s*\)/)
  if (bindM) {
    node.bind = bindM[1]
  }
  return node
}

function parseSwitch(str) {
  const match = str.match(/^SWITCH\s*\(\s*([^()]+?)\s*\)\s*\.\s*to\s*\(\s*([\s\S]+)\s*\)$/i)
  if (!match) {
    return null
  }
  return {
    type: 'switch',
    condition: match[1].trim(),
    branches: splitTopLevel(match[2]).map(part => parseComponent(part))
  }
}

function parseIf(str) {
  if (!str.trim().toUpperCase().startsWith('IF(')) {
    return null
  }
  const inner = extractParenContent(str, 2)
  if (inner == null) {
    return null
  }
  const parts = splitTopLevel(inner)
  if (parts.length < 3) {
    return null
  }
  return {
    type: 'if',
    condition: parts[0].trim(),
    trueBranch: parseExpr(parts[1]),
    falseBranch: parseExpr(parts[2]),
    remark: ''
  }
}

function parseThen(str) {
  if (!str.trim().toUpperCase().startsWith('THEN(')) {
    return null
  }
  const inner = extractParenContent(str, 4)
  if (inner == null) {
    return null
  }
  return {
    type: 'then',
    children: splitTopLevel(inner).map(parseExpr)
  }
}

function parseWhen(str) {
  const match = str.match(/^WHEN\s*\(\s*([\s\S]+?)\s*\)(?:\s*\.\s*maxWaitSeconds\s*\(\s*(\d+(?:\.\d+)?)\s*\))?$/i)
  if (!match) {
    return null
  }
  return {
    type: 'when',
    children: splitTopLevel(match[1]).map(parseExpr),
    maxWaitSeconds: match[2] != null ? Number(match[2]) : null,
    remark: ''
  }
}

function parseFor(str) {
  const match = str.match(/^FOR\s*\(\s*([^()]+?)\s*\)\s*\.\s*DO\s*\(\s*([\s\S]+)\s*\)$/i)
  if (!match) {
    return null
  }
  return {
    type: 'for',
    loopComponent: match[1].trim(),
    body: parseExpr(match[2]),
    remark: ''
  }
}

function parseRetry(str) {
  const match = str.match(/^RETRY\s*\(\s*([\s\S]+?)\s*\)\s*\.\s*times\s*\(\s*(\d+)\s*\)$/i)
  if (!match) {
    return null
  }
  return {
    type: 'retry',
    target: parseExpr(match[1]),
    times: Number(match[2]),
    remark: ''
  }
}

function parseCatch(str) {
  const trimmed = str.trim()
  if (!trimmed.toUpperCase().startsWith('CATCH(')) {
    return null
  }
  const doMatch = trimmed.match(/^CATCH\s*\(\s*([\s\S]+?)\s*\)\s*\.\s*DO\s*\(\s*([\s\S]+)\s*\)$/i)
  if (doMatch) {
    return {
      type: 'catch',
      tryNode: parseExpr(doMatch[1]),
      fallback: parseExpr(doMatch[2]),
      remark: ''
    }
  }
  const inner = extractParenContent(trimmed, 5)
  if (inner == null) {
    return null
  }
  const parts = splitTopLevel(inner)
  if (parts.length < 2) {
    return null
  }
  return {
    type: 'catch',
    tryNode: parseExpr(parts[0]),
    fallback: parseExpr(parts[1]),
    remark: ''
  }
}

export function parseExpr(str) {
  if (!str || typeof str !== 'string') {
    return null
  }
  const trimmed = str.trim()
  if (!trimmed) {
    return null
  }
  return parseThen(trimmed)
    || parseIf(trimmed)
    || parseSwitch(trimmed)
    || parseWhen(trimmed)
    || parseFor(trimmed)
    || parseCatch(trimmed)
    || parseRetry(trimmed)
    || parseComponent(trimmed)
}

export function parseEl(el) {
  if (!el || typeof el !== 'string') {
    return null
  }
  const trimmed = el.trim().replace(/;\s*$/, '')
  const expr = parseExpr(trimmed)
  if (!expr) {
    return null
  }
  if (expr.type === 'then') {
    return expr
  }
  return { type: 'then', children: [expr] }
}

function buildComponentExpr(node) {
  if (!node || !node.nodeId) {
    return ''
  }
  let s = node.nodeId
  if (node.tag) {
    s += `.tag("${node.tag}")`
  }
  if (node.dataKey) {
    s += `.data("${node.dataKey}","${node.dataValue || ''}")`
  }
  if (node.bind) {
    s += `.bind("${node.bind}")`
  }
  return s
}

export function buildExpr(node) {
  if (!node) {
    return ''
  }
  if (node.type === 'component') {
    return buildComponentExpr(node)
  }
  if (node.type === 'subchain') {
    return node.chainName || ''
  }
  if (node.type === 'if') {
    return `IF(${node.condition}, ${buildExpr(node.trueBranch)}, ${buildExpr(node.falseBranch)})`
  }
  if (node.type === 'switch') {
    const ids = (node.branches || []).map(b => buildExpr(b)).join(', ')
    return `SWITCH(${node.condition}).to(${ids})`
  }
  if (node.type === 'when') {
    const inner = (node.children || []).map(buildExpr).join(', ')
    let s = `WHEN(${inner})`
    if (node.maxWaitSeconds != null && node.maxWaitSeconds > 0) {
      s += `.maxWaitSeconds(${node.maxWaitSeconds})`
    }
    return s
  }
  if (node.type === 'for') {
    return `FOR(${node.loopComponent}).DO(${buildExpr(node.body)})`
  }
  if (node.type === 'retry') {
    return `RETRY(${buildExpr(node.target)}).times(${node.times || 3})`
  }
  if (node.type === 'catch') {
    if (node.tryNode && node.tryNode.type === 'retry') {
      return `CATCH(${buildExpr(node.tryNode)}, ${buildExpr(node.fallback)})`
    }
    return `CATCH(${buildExpr(node.tryNode)}).DO(${buildExpr(node.fallback)})`
  }
  if (node.type === 'then') {
    return `THEN(${(node.children || []).map(buildExpr).join(', ')})`
  }
  return ''
}

export function buildEl(model) {
  const expr = buildExpr(model)
  return expr ? `${expr};` : ''
}

export function createSubChainNode(chainName, name) {
  return {
    type: 'subchain',
    chainName: chainName || '',
    name: name || chainName || '',
    remark: ''
  }
}

export function createComponentNode(nodeId, name) {
  return {
    type: 'component',
    nodeId,
    name: name || nodeId,
    remark: '',
    tag: '',
    dataKey: '',
    dataValue: '',
    bind: ''
  }
}

export function createIfNode(condition) {
  return {
    type: 'if',
    condition: condition || '',
    trueBranch: createComponentNode('', ''),
    falseBranch: createComponentNode('', ''),
    remark: ''
  }
}

export function createSwitchNode(condition, branchIds) {
  return {
    type: 'switch',
    condition: condition || '',
    branches: (branchIds || ['', '']).map(id => createComponentNode(id, id)),
    remark: ''
  }
}

export function createWhenNode(childIds, maxWaitSeconds) {
  return {
    type: 'when',
    children: (childIds || ['', '']).map(id => createComponentNode(id, id)),
    maxWaitSeconds: maxWaitSeconds != null ? maxWaitSeconds : null,
    remark: ''
  }
}

export function createForNode(loopComponent, bodyNodeId) {
  return {
    type: 'for',
    loopComponent: loopComponent || '',
    body: createComponentNode(bodyNodeId || '', bodyNodeId || ''),
    remark: ''
  }
}

export function createCatchNode(tryNodeId, fallbackNodeId) {
  return {
    type: 'catch',
    tryNode: createComponentNode(tryNodeId || '', tryNodeId || ''),
    fallback: createComponentNode(fallbackNodeId || '', fallbackNodeId || ''),
    remark: ''
  }
}

export function createRetryNode(targetNodeId, times) {
  return {
    type: 'retry',
    target: createComponentNode(targetNodeId || '', targetNodeId || ''),
    times: times || 3,
    remark: ''
  }
}

export function collectNodeIds(model) {
  const ids = []
  function walk(node) {
    if (!node) {
      return
    }
    if (node.type === 'component' && node.nodeId) {
      ids.push(node.nodeId)
    }
    if (node.type === 'subchain' && node.chainName) {
      ids.push(node.chainName)
    }
    if (node.type === 'if') {
      if (node.condition) {
        ids.push(node.condition)
      }
      walk(node.trueBranch)
      walk(node.falseBranch)
    }
    if (node.type === 'switch') {
      if (node.condition) {
        ids.push(node.condition)
      }
      ;(node.branches || []).forEach(walk)
    }
    if (node.type === 'when') {
      ;(node.children || []).forEach(walk)
    }
    if (node.type === 'for') {
      if (node.loopComponent) {
        ids.push(node.loopComponent)
      }
      walk(node.body)
    }
    if (node.type === 'catch') {
      walk(node.tryNode)
      walk(node.fallback)
    }
    if (node.type === 'retry') {
      walk(node.target)
    }
    if (node.type === 'then') {
      ;(node.children || []).forEach(walk)
    }
  }
  walk(model)
  return ids
}

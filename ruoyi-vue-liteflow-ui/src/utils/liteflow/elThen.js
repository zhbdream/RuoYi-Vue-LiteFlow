/**
 * 简单 THEN 串行 EL 构建与解析（Step A 范围）
 */

export function buildThenEl(nodeIds) {
  if (!nodeIds || !nodeIds.length) {
    return ''
  }
  if (nodeIds.length === 1) {
    return `THEN(${nodeIds[0]});`
  }
  return `THEN(${nodeIds.join(', ')});`
}

/**
 * 解析顶层 THEN(a, b, c) 表达式，不支持嵌套 IF/SWITCH
 */
export function parseSimpleThen(el) {
  if (!el || typeof el !== 'string') {
    return null
  }
  const trimmed = el.trim().replace(/;\s*$/, '')
  const match = trimmed.match(/^THEN\s*\(\s*([^()]+)\s*\)$/i)
  if (!match) {
    return null
  }
  const ids = match[1]
    .split(',')
    .map(s => s.trim())
    .filter(Boolean)
  return ids.length ? ids : null
}

export function isSimpleThenEl(el) {
  return parseSimpleThen(el) !== null
}

export const NODE_TYPE_LABELS = {
  common: '普通组件',
  boolean: '布尔组件',
  switch: '选择组件',
  for: '循环组件',
  iterator: '迭代组件',
  unknown: '其他'
}

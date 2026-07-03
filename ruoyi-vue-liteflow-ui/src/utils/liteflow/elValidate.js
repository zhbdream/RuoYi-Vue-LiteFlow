/**
 * EL 模型校验（保存前）
 */

function walk(node, errors, warnings, registeredIds) {
  if (!node) {
    return
  }
  if (node.type === 'component') {
    if (!node.nodeId || !node.nodeId.trim()) {
      errors.push('存在未配置 nodeId 的组件节点')
    } else if (registeredIds && registeredIds.size && !registeredIds.has(node.nodeId)) {
      warnings.push(`组件「${node.nodeId}」未在系统中注册`)
    }
    return
  }
  if (node.type === 'if') {
    if (!node.condition) {
      errors.push('IF 节点缺少条件组件')
    }
    if (!node.trueBranch || !node.trueBranch.nodeId) {
      errors.push('IF 节点缺少真分支')
    }
    if (!node.falseBranch || !node.falseBranch.nodeId) {
      errors.push('IF 节点缺少假分支')
    }
    walk(node.trueBranch, errors, warnings, registeredIds)
    walk(node.falseBranch, errors, warnings, registeredIds)
    return
  }
  if (node.type === 'switch') {
    if (!node.condition) {
      errors.push('SWITCH 节点缺少路由组件')
    }
    if (!node.branches || node.branches.length < 2) {
      errors.push('SWITCH 至少需要 2 个分支')
    }
    ;(node.branches || []).forEach(b => walk(b, errors, warnings, registeredIds))
    return
  }
  if (node.type === 'when') {
    if (!node.children || node.children.length < 2) {
      errors.push('WHEN 并行至少需要 2 个子节点')
    }
    ;(node.children || []).forEach(c => walk(c, errors, warnings, registeredIds))
    return
  }
  if (node.type === 'for') {
    if (!node.loopComponent) {
      errors.push('FOR 节点缺少循环次数组件')
    }
    if (!node.body) {
      errors.push('FOR 节点缺少 DO 循环体')
    }
    walk(node.body, errors, warnings, registeredIds)
    return
  }
  if (node.type === 'catch') {
    if (!node.tryNode) {
      errors.push('CATCH 节点缺少 try 分支')
    }
    if (!node.fallback) {
      errors.push('CATCH 节点缺少 fallback 分支')
    }
    walk(node.tryNode, errors, warnings, registeredIds)
    walk(node.fallback, errors, warnings, registeredIds)
    return
  }
  if (node.type === 'retry') {
    if (!node.target) {
      errors.push('RETRY 节点缺少目标组件')
    }
    if (!node.times || node.times < 1) {
      errors.push('RETRY 次数至少为 1')
    }
    walk(node.target, errors, warnings, registeredIds)
    return
  }
  if (node.type === 'then') {
    if (!node.children || !node.children.length) {
      errors.push('链路不能为空')
    }
    ;(node.children || []).forEach(c => walk(c, errors, warnings, registeredIds))
  }
}

export function validateFlowModel(model, components) {
  const errors = []
  const warnings = []
  const registeredIds = new Set((components || []).map(c => c.nodeId))
  walk(model, errors, warnings, registeredIds)
  return {
    valid: errors.length === 0,
    errors: [...new Set(errors)],
    warnings: [...new Set(warnings)]
  }
}

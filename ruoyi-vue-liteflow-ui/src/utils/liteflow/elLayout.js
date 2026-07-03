/**
 * 将 EL 模型布局为 X6 节点/边
 */

const NODE_W = 160
const NODE_H = 48
const LOGIC_W = 140
const LOGIC_H = 52
const GAP_Y = 88
const BRANCH_X = 190
const MAIN_X = 260
const START_Y = 40

let cellSeq = 0

function nextCellId(prefix) {
  cellSeq += 1
  return `${prefix}-${cellSeq}`
}

function edgeAttrs(color = '#409EFF', dashed = false) {
  return {
    line: {
      stroke: color,
      strokeWidth: 2,
      strokeDasharray: dashed ? '5 5' : undefined,
      targetMarker: { name: 'block', width: 8, height: 8 }
    }
  }
}

function compPorts() {
  return {
    groups: {
      top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#409EFF', fill: '#fff' } } },
      bottom: { position: 'bottom', attrs: { circle: { r: 4, magnet: true, stroke: '#409EFF', fill: '#fff' } } }
    },
    items: [{ id: 'in', group: 'top' }, { id: 'out', group: 'bottom' }]
  }
}

function ifPorts() {
  return {
    groups: {
      top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#E6A23C', fill: '#fff' } } },
      true: { position: { name: 'left', args: { dx: 20, dy: 26 } }, attrs: { circle: { r: 4, magnet: true, stroke: '#67C23A', fill: '#fff' } } },
      false: { position: { name: 'right', args: { dx: -20, dy: 26 } }, attrs: { circle: { r: 4, magnet: true, stroke: '#F56C6C', fill: '#fff' } } }
    },
    items: [{ id: 'in', group: 'top' }, { id: 'true', group: 'true' }, { id: 'false', group: 'false' }]
  }
}

function switchPorts(branchCount) {
  const groups = {
    top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#67C23A', fill: '#fff' } } }
  }
  const items = [{ id: 'in', group: 'top' }]
  for (let i = 0; i < branchCount; i++) {
    const ratio = branchCount === 1 ? 0.5 : i / (branchCount - 1)
    groups[`b${i}`] = {
      position: { name: 'bottom', args: { dx: (ratio - 0.5) * (branchCount > 2 ? 120 : 80) } },
      attrs: { circle: { r: 4, magnet: true, stroke: '#67C23A', fill: '#fff' } }
    }
    items.push({ id: `b${i}`, group: `b${i}` })
  }
  return { groups, items }
}

function makeSubChainCell(modelNode, x, y) {
  const cellId = nextCellId(modelNode.chainName || 'subchain')
  return {
    cellId,
    x,
    y,
    width: NODE_W,
    height: NODE_H,
    shape: 'rect',
    label: '⛓ ' + (modelNode.chainName || ''),
    data: {
      lfType: 'subchain',
      modelKey: modelNode._key,
      chainName: modelNode.chainName,
      name: modelNode.name || modelNode.chainName,
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#13C2C2', fill: '#e6fffb', rx: 6, ry: 6 },
      label: { fill: '#08979c', fontSize: 12, fontWeight: 600 }
    },
    ports: compPorts()
  }
}

function layoutSubChain(modelNode, x, y, compMap) {
  const cell = makeSubChainCell(modelNode, x, y)
  return {
    nodes: [cell],
    edges: [],
    exitIds: [cell.cellId],
    bottomY: y + NODE_H
  }
}

function makeComponentCell(modelNode, x, y, compMap) {
  const meta = compMap[modelNode.nodeId] || {}
  const cellId = nextCellId(modelNode.nodeId || 'comp')
  return {
    cellId,
    x,
    y,
    width: NODE_W,
    height: NODE_H,
    shape: 'rect',
    label: modelNode.nodeId,
    data: {
      lfType: 'component',
      modelKey: modelNode._key,
      nodeId: modelNode.nodeId,
      name: modelNode.name || meta.name || modelNode.nodeId,
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#409EFF', fill: '#ecf5ff', rx: 6, ry: 6 },
      label: { fill: '#303133', fontSize: 13, fontWeight: 600 }
    },
    ports: compPorts()
  }
}

function layoutComponent(modelNode, x, y, compMap) {
  const cell = makeComponentCell(modelNode, x, y, compMap)
  return {
    nodes: [cell],
    edges: [],
    exitIds: [cell.cellId],
    bottomY: y + NODE_H
  }
}

function layoutIf(modelNode, x, y, compMap) {
  const ifId = nextCellId('if')
  const nodes = [{
    cellId: ifId,
    x: x - LOGIC_W / 2 + NODE_W / 2,
    y,
    width: LOGIC_W,
    height: LOGIC_H,
    shape: 'rect',
    label: `IF: ${modelNode.condition}`,
    data: {
      lfType: 'if',
      modelKey: modelNode._key,
      condition: modelNode.condition,
      name: `IF ${modelNode.condition}`,
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#E6A23C', fill: '#fdf6ec', rx: 8, ry: 8 },
      label: { fill: '#E6A23C', fontSize: 12, fontWeight: 600 }
    },
    ports: ifPorts()
  }]
  const edges = []
  const branchY = y + GAP_Y
  const trueLayout = layoutComponent(modelNode.trueBranch, x - BRANCH_X, branchY, compMap)
  const falseLayout = layoutComponent(modelNode.falseBranch, x + BRANCH_X, branchY, compMap)
  nodes.push(...trueLayout.nodes, ...falseLayout.nodes)
  edges.push(
    { source: ifId, sourcePort: 'true', target: trueLayout.exitIds[0], targetPort: 'in', attrs: edgeAttrs('#67C23A') },
    { source: ifId, sourcePort: 'false', target: falseLayout.exitIds[0], targetPort: 'in', attrs: edgeAttrs('#F56C6C') }
  )
  return {
    nodes,
    edges,
    exitIds: [...trueLayout.exitIds, ...falseLayout.exitIds],
    bottomY: branchY + NODE_H
  }
}

function layoutSwitch(modelNode, x, y, compMap) {
  const branches = modelNode.branches || []
  const swId = nextCellId('switch')
  const nodes = [{
    cellId: swId,
    x: x - LOGIC_W / 2 + NODE_W / 2,
    y,
    width: LOGIC_W,
    height: LOGIC_H,
    shape: 'rect',
    label: `SWITCH: ${modelNode.condition}`,
    data: {
      lfType: 'switch',
      modelKey: modelNode._key,
      condition: modelNode.condition,
      branchNodeIds: branches.map(b => b.nodeId),
      name: `SWITCH ${modelNode.condition}`,
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#67C23A', fill: '#f0f9eb', rx: 8, ry: 8 },
      label: { fill: '#67C23A', fontSize: 12, fontWeight: 600 }
    },
    ports: switchPorts(branches.length)
  }]
  const edges = []
  const branchY = y + GAP_Y
  const span = Math.max(branches.length - 1, 1) * BRANCH_X
  const startX = x - span / 2
  const exitIds = []
  branches.forEach((branch, index) => {
    const bx = branches.length === 1 ? x : startX + index * BRANCH_X
    const bl = layoutComponent(branch, bx, branchY, compMap)
    nodes.push(...bl.nodes)
    edges.push({
      source: swId,
      sourcePort: `b${index}`,
      target: bl.exitIds[0],
      targetPort: 'in',
      attrs: edgeAttrs('#67C23A')
    })
    exitIds.push(bl.exitIds[0])
  })
  return {
    nodes,
    edges,
    exitIds,
    bottomY: branchY + NODE_H
  }
}

function whenPorts(count) {
  const groups = {
    top: { position: 'top', attrs: { circle: { r: 4, magnet: true, stroke: '#909399', fill: '#fff' } } }
  }
  const items = [{ id: 'in', group: 'top' }]
  for (let i = 0; i < count; i++) {
    const ratio = count === 1 ? 0.5 : i / (count - 1)
    groups[`p${i}`] = {
      position: { name: 'bottom', args: { dx: (ratio - 0.5) * (count > 2 ? 140 : 90) } },
      attrs: { circle: { r: 4, magnet: true, stroke: '#909399', fill: '#fff' } }
    }
    items.push({ id: `p${i}`, group: `p${i}` })
  }
  return { groups, items }
}

function layoutWhen(modelNode, x, y, compMap) {
  const children = modelNode.children || []
  const whenId = nextCellId('when')
  const label = modelNode.maxWaitSeconds
    ? `WHEN (${modelNode.maxWaitSeconds}s)`
    : 'WHEN 并行'
  const nodes = [{
    cellId: whenId,
    x: x - LOGIC_W / 2 + NODE_W / 2,
    y,
    width: LOGIC_W,
    height: LOGIC_H,
    shape: 'rect',
    label,
    data: {
      lfType: 'when',
      modelKey: modelNode._key,
      maxWaitSeconds: modelNode.maxWaitSeconds,
      childNodeIds: children.map(c => c.nodeId),
      name: label,
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#909399', fill: '#f4f4f5', rx: 8, ry: 8 },
      label: { fill: '#606266', fontSize: 12, fontWeight: 600 }
    },
    ports: whenPorts(children.length)
  }]
  const edges = []
  const branchY = y + GAP_Y
  const span = Math.max(children.length - 1, 1) * BRANCH_X
  const startX = x - span / 2
  const exitIds = []
  children.forEach((child, index) => {
    const bx = children.length === 1 ? x : startX + index * BRANCH_X
    const bl = layoutStep(child, bx, branchY, compMap)
    nodes.push(...bl.nodes)
    edges.push(...bl.edges)
    const entryId = bl.nodes[0] && bl.nodes[0].cellId
    if (entryId) {
      edges.push({
        source: whenId,
        sourcePort: `p${index}`,
        target: entryId,
        targetPort: 'in',
        attrs: edgeAttrs('#909399')
      })
      exitIds.push(...bl.exitIds)
    }
  })
  return {
    nodes,
    edges,
    exitIds,
    bottomY: branchY + NODE_H + (children.some(c => c.type !== 'component') ? GAP_Y / 2 : 0)
  }
}

function layoutFor(modelNode, x, y, compMap) {
  const forId = nextCellId('for')
  const nodes = [{
    cellId: forId,
    x: x - LOGIC_W / 2 + NODE_W / 2,
    y,
    width: LOGIC_W,
    height: LOGIC_H,
    shape: 'rect',
    label: `FOR: ${modelNode.loopComponent}`,
    data: {
      lfType: 'for',
      modelKey: modelNode._key,
      loopComponent: modelNode.loopComponent,
      name: `FOR ${modelNode.loopComponent}`,
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#9C27B0', fill: '#f3e5f5', rx: 8, ry: 8 },
      label: { fill: '#9C27B0', fontSize: 12, fontWeight: 600 }
    },
    ports: compPorts()
  }]
  const edges = []
  const bodyY = y + GAP_Y
  const bodyLayout = layoutStep(modelNode.body, x, bodyY, compMap)
  nodes.push(...bodyLayout.nodes)
  edges.push(...bodyLayout.edges)
  const bodyEntry = bodyLayout.nodes[0] && bodyLayout.nodes[0].cellId
  if (bodyEntry) {
    edges.push({
      source: forId,
      sourcePort: 'out',
      target: bodyEntry,
      targetPort: 'in',
      attrs: edgeAttrs('#9C27B0')
    })
  }
  return {
    nodes,
    edges,
    exitIds: bodyLayout.exitIds.length ? bodyLayout.exitIds : [forId],
    bottomY: bodyLayout.bottomY
  }
}

function layoutCatch(modelNode, x, y, compMap) {
  const catchId = nextCellId('catch')
  const nodes = [{
    cellId: catchId,
    x: x - LOGIC_W / 2 + NODE_W / 2,
    y,
    width: LOGIC_W,
    height: LOGIC_H,
    shape: 'rect',
    label: 'CATCH',
    data: {
      lfType: 'catch',
      modelKey: modelNode._key,
      name: 'CATCH',
      remark: modelNode.remark || ''
    },
    attrs: {
      body: { stroke: '#F56C6C', fill: '#fef0f0', rx: 8, ry: 8 },
      label: { fill: '#F56C6C', fontSize: 12, fontWeight: 600 }
    },
    ports: ifPorts()
  }]
  const edges = []
  const branchY = y + GAP_Y
  const tryLayout = layoutStep(modelNode.tryNode, x - BRANCH_X, branchY, compMap)
  const fallbackLayout = layoutStep(modelNode.fallback, x + BRANCH_X, branchY, compMap)
  nodes.push(...tryLayout.nodes, ...fallbackLayout.nodes)
  edges.push(...tryLayout.edges, ...fallbackLayout.edges)
  const tryEntry = tryLayout.nodes[0] && tryLayout.nodes[0].cellId
  const fbEntry = fallbackLayout.nodes[0] && fallbackLayout.nodes[0].cellId
  if (tryEntry) {
    edges.push({ source: catchId, sourcePort: 'true', target: tryEntry, targetPort: 'in', attrs: edgeAttrs('#409EFF') })
  }
  if (fbEntry) {
    edges.push({ source: catchId, sourcePort: 'false', target: fbEntry, targetPort: 'in', attrs: edgeAttrs('#F56C6C') })
  }
  return {
    nodes,
    edges,
    exitIds: [...tryLayout.exitIds, ...fallbackLayout.exitIds],
    bottomY: branchY + NODE_H
  }
}

function layoutStep(modelNode, x, y, compMap) {
  if (modelNode.type === 'subchain') {
    return layoutSubChain(modelNode, x, y, compMap)
  }
  if (modelNode.type === 'if') {
    return layoutIf(modelNode, x, y, compMap)
  }
  if (modelNode.type === 'switch') {
    return layoutSwitch(modelNode, x, y, compMap)
  }
  if (modelNode.type === 'when') {
    return layoutWhen(modelNode, x, y, compMap)
  }
  if (modelNode.type === 'for') {
    return layoutFor(modelNode, x, y, compMap)
  }
  if (modelNode.type === 'catch') {
    return layoutCatch(modelNode, x, y, compMap)
  }
  if (modelNode.type === 'retry') {
    return layoutStep(modelNode.target, x, y, compMap)
  }
  return layoutComponent(modelNode, x, y, compMap)
}

function connectExits(exitIds, targetId, edges) {
  exitIds.forEach(exitId => {
    edges.push({
      source: exitId,
      sourcePort: 'out',
      target: targetId,
      targetPort: 'in',
      attrs: edgeAttrs('#909399', true)
    })
  })
}

export function layoutFlowModel(model, components) {
  cellSeq = 0
  const compMap = {}
  ;(components || []).forEach(c => {
    compMap[c.nodeId] = c
  })
  const nodes = []
  const edges = []
  if (!model || model.type !== 'then' || !model.children) {
    return { nodes, edges }
  }

  let y = START_Y
  let prevExitIds = []

  model.children.forEach(child => {
    const step = layoutStep(child, MAIN_X, y, compMap)
    nodes.push(...step.nodes)
    edges.push(...step.edges)
    const entryId = step.nodes[0] && step.nodes[0].cellId
    if (entryId && prevExitIds.length) {
      connectExits(prevExitIds, entryId, edges)
    }
    prevExitIds = step.exitIds
    y = step.bottomY + GAP_Y / 2
  })

  return { nodes, edges }
}

export function enrichModelFromComponents(model, components) {
  const compMap = {}
  ;(components || []).forEach(c => {
    compMap[c.nodeId] = c
  })
  function walk(node) {
    if (!node) {
      return
    }
    if (node.type === 'component' && node.nodeId && compMap[node.nodeId]) {
      node.name = node.name || compMap[node.nodeId].name
    }
    if (node.type === 'subchain' && node.chainName) {
      node.name = node.name || node.chainName
    }
    if (node.type === 'if') {
      walk(node.trueBranch)
      walk(node.falseBranch)
    }
    if (node.type === 'switch') {
      ;(node.branches || []).forEach(walk)
    }
    if (node.type === 'when') {
      ;(node.children || []).forEach(walk)
    }
    if (node.type === 'for') {
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
  return model
}

export { NODE_W, NODE_H, MAIN_X, START_Y, GAP_Y }

/**
 * 内置链路模板（从模板创建）
 */
export const CHAIN_TEMPLATES = [
  {
    key: 'blank',
    label: '空白链路',
    chainDesc: '空白 THEN 链路，在编排器中拖拽搭建',
    elData: '',
    contextClass: ''
  },
  {
    key: 'helloChain',
    label: 'helloChain 入门',
    chainDesc: 'THEN 串行入门 Demo',
    elData: 'THEN(helloA, helloB, helloC);',
    contextClass: ''
  },
  {
    key: 'orderProcess',
    label: 'orderProcess 订单',
    chainDesc: 'IF/SWITCH 订单处理 Demo',
    elData: 'THEN(initOrder, validateOrder, IF(hasStock, calcDiscount, orderFail), SWITCH(payType).to(aliPay, wechatPay, balancePay), completeOrder);',
    contextClass: 'com.ruoyiliteflow.liteflow.domain.context.OrderContext'
  },
  {
    key: 'dynamicPricing',
    label: 'dynamicPricing 定价',
    chainDesc: '脚本组件 + 定价 Demo2',
    elData: 'THEN(loadMemberLevel, calcFullReduction, applyCoupon, scriptPriceAdjust, buildPriceResult);',
    contextClass: 'com.ruoyiliteflow.liteflow.domain.context.PricingContext'
  },
  {
    key: 'parallelAudit',
    label: 'parallelAudit 并行校验',
    chainDesc: 'WHEN 并行 Demo3',
    elData: 'THEN(prepareAudit, WHEN(checkInventory, checkCredit, checkRisk).maxWaitSeconds(3), mergeAuditResult, IF(auditPassed, auditSuccess, auditReject));',
    contextClass: 'com.ruoyiliteflow.liteflow.domain.context.AuditContext'
  },
  {
    key: 'resilientNotify',
    label: 'resilientNotify 容错通知',
    chainDesc: 'CATCH/RETRY/PRE/FINALLY 容错 Demo4',
    elData: 'THEN(PRE(initNotify), CATCH(sendNotify.retry(3)).DO(notifyFallback), FINALLY(logNotify));',
    contextClass: 'com.ruoyiliteflow.liteflow.domain.context.NotifyContext'
  },
  {
    key: 'batchProcess',
    label: 'batchProcess 批量',
    chainDesc: 'FOR 循环 Demo6',
    elData: 'THEN(initBatch, FOR(batchCount).DO(processOrderItem), summarizeBatch);',
    contextClass: 'com.ruoyiliteflow.liteflow.domain.context.BatchContext'
  },
  {
    key: 'agentRiskDemo',
    label: 'agentRiskDemo 风控Agent',
    chainDesc: 'DeepSeek Re-Act Agent Demo7（需 DEEPSEEK_API_KEY）',
    elData: 'THEN(agentPrepare, riskAgent, agentNotify);',
    contextClass: 'com.ruoyiliteflow.agent.domain.AgentRiskContext'
  },
  {
    key: 'lc4jChatDemo',
    label: 'lc4jChatDemo LangChain4j',
    chainDesc: 'LangChain4j AiServices + Tool Demo8',
    elData: 'THEN(lc4jPrepare, lc4jChat, lc4jNotify);',
    contextClass: 'com.ruoyiliteflow.langchain.domain.Lc4jRiskContext'
  },
  {
    key: 'lc4jGraphDemo',
    label: 'lc4jGraphDemo LangGraph4j',
    chainDesc: 'LangGraph4j StateGraph 条件边 Demo9',
    elData: 'THEN(lc4jPrepare, lc4jGraph, lc4jNotify);',
    contextClass: 'com.ruoyiliteflow.langchain.domain.Lc4jRiskContext'
  },
  {
    key: 'lc4jRagDemo',
    label: 'lc4jRagDemo LangChain4j RAG',
    chainDesc: 'LangChain4j RAG 售后知识问答 Demo10',
    elData: 'THEN(lc4jRagPrepare, lc4jRag, lc4jRagNotify);',
    contextClass: 'com.ruoyiliteflow.langchain.domain.Lc4jRagContext'
  }
]

export const DEMO_EXECUTE_PARAMS = {
  helloChain: { name: 'RuoYi' },
  orderProcess: {
    userId: 1001,
    skuId: 'SKU-001',
    quantity: 2,
    payType: 'wechat',
    couponCode: 'SAVE10'
  },
  dynamicPricing: {
    userId: 1001,
    memberLevel: 'VIP',
    originalPrice: 299,
    couponCode: 'SAVE20'
  },
  parallelAudit: {
    orderId: 'ORD-2026-100',
    userId: 1001
  },
  resilientNotify: {
    userId: 'U10001',
    channel: 'sms',
    simulateFail: true
  },
  batchProcess: {
    orderId: 'ORD-2026-001',
    items: [
      { skuId: 'SKU-001', qty: 2 },
      { skuId: 'SKU-002', qty: 1 }
    ]
  },
  routeDemo: {
    userType: 'NEW'
  },
  agentRiskDemo: {
    orderId: 'ORD-AGENT-1001',
    userId: 1001,
    userType: 'NEW',
    amount: 1299.00,
    scene: 'checkout'
  },
  lc4jChatDemo: {
    orderId: 'ORD-LC4J-1001',
    userId: 2001,
    userType: 'NEW',
    amount: 2599.00,
    scene: 'checkout'
  },
  lc4jGraphDemo: {
    orderId: 'ORD-GRAPH-1001',
    userId: 2001,
    userType: 'NEW',
    amount: 9999.00,
    scene: 'checkout'
  },
  lc4jRagDemo: {
    question: '下单后第5天衣服尺码不合适，可以退货或换货吗？运费谁承担？'
  }
}

export function getDefaultExecuteParam(chainName) {
  return DEMO_EXECUTE_PARAMS[chainName] || {}
}

package com.ruoyiliteflow.langchain.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AiServices 接口：风控分析助理
 */
public interface RiskAssistant
{
    @SystemMessage("""
            你是电商支付前的风控分析助理，只做风险研判与建议，不要编造未给出的事实。
            如需了解订单上下文，请调用工具 read_order_risk_context。
            输出要求：
            1) 先给风险等级：LOW / MEDIUM / HIGH 之一（格式：风险等级：XXX）
            2) 再用 2~4 句中文说明理由与建议
            """)
    String analyze(@UserMessage String userMessage);
}
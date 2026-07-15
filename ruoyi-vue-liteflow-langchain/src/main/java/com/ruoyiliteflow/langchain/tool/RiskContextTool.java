package com.ruoyiliteflow.langchain.tool;

import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import dev.langchain4j.agent.tool.Tool;

/**
 * 供 AiServices / Graph 节点调用的业务 Tool
 */
public class RiskContextTool
{
    private final Lc4jRiskContext context;

    public RiskContextTool(Lc4jRiskContext context)
    {
        this.context = context;
    }

    @Tool("read_order_risk_context: 读取当前订单风控上下文（订单号、金额、用户类型、场景）")
    public String readOrderRiskContext()
    {
        if (context == null)
        {
            return "上下文为空";
        }
        return "orderId=" + context.getOrderId()
                + ", userId=" + context.getUserId()
                + ", userType=" + context.getUserType()
                + ", amount=" + context.getAmount()
                + ", scene=" + context.getScene();
    }
}
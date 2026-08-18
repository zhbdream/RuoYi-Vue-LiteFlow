package com.ruoyiliteflow.aicore.risk;

import java.math.BigDecimal;
import dev.langchain4j.agent.tool.Tool;

/**
 * 风控订单上下文（无 LiteFlow Slot 依赖）
 */
public class RiskOrderContext
{
    private String orderId;
    private Long userId;
    private String userType;
    private BigDecimal amount;
    private String scene;

    public String getOrderId()
    {
        return orderId;
    }

    public void setOrderId(String orderId)
    {
        this.orderId = orderId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserType()
    {
        return userType;
    }

    public void setUserType(String userType)
    {
        this.userType = userType;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    @Tool("read_order_risk_context: 读取当前订单风控上下文（订单号、金额、用户类型、场景）")
    public String readOrderRiskContext()
    {
        return "orderId=" + orderId
                + ", userId=" + userId
                + ", userType=" + userType
                + ", amount=" + amount
                + ", scene=" + scene;
    }
}

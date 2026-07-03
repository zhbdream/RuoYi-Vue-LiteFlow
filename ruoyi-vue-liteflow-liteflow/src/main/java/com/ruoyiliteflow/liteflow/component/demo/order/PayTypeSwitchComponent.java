package com.ruoyiliteflow.liteflow.component.demo.order;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;

@LiteflowComponent(value = "payType", name = "支付方式路由")
@Component
public class PayTypeSwitchComponent extends NodeSwitchComponent
{
    @Override
    public String processSwitch()
    {
        OrderContext ctx = this.getFirstContextBean();
        String payType = ctx.getPayType();
        ctx.addStep("payType=" + payType);
        if ("ali".equalsIgnoreCase(payType) || "alipay".equalsIgnoreCase(payType))
        {
            return "aliPay";
        }
        if ("balance".equalsIgnoreCase(payType))
        {
            return "balancePay";
        }
        return "wechatPay";
    }
}

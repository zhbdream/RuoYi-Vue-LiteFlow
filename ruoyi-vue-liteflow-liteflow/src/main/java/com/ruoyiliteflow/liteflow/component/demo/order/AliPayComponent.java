package com.ruoyiliteflow.liteflow.component.demo.order;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "aliPay", name = "支付宝")
@Component
public class AliPayComponent extends NodeComponent
{
    @Override
    public void process()
    {
        OrderContext ctx = this.getFirstContextBean();
        ctx.setPayChannel("支付宝");
        ctx.addStep("aliPay");
    }
}

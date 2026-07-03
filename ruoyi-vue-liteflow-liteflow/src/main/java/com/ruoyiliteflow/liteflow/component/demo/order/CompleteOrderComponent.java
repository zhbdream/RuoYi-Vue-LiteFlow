package com.ruoyiliteflow.liteflow.component.demo.order;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "completeOrder", name = "完成订单")
@Component
public class CompleteOrderComponent extends NodeComponent
{
    @Override
    public void process()
    {
        OrderContext ctx = this.getFirstContextBean();
        if (!ctx.isSuccess() && ctx.getMessage() == null)
        {
            ctx.setSuccess(true);
            ctx.setMessage("下单成功，支付渠道：" + ctx.getPayChannel());
        }
        ctx.addStep("completeOrder");
    }
}

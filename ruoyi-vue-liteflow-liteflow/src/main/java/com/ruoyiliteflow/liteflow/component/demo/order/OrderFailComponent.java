package com.ruoyiliteflow.liteflow.component.demo.order;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "orderFail", name = "订单失败")
@Component
public class OrderFailComponent extends NodeComponent
{
    @Override
    public void process()
    {
        OrderContext ctx = this.getFirstContextBean();
        ctx.setSuccess(false);
        ctx.setMessage("库存不足，下单失败");
        ctx.addStep("orderFail");
    }
}

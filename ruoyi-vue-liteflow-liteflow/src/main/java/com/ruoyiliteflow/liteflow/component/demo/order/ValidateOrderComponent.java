package com.ruoyiliteflow.liteflow.component.demo.order;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "validateOrder", name = "订单校验")
@Component
public class ValidateOrderComponent extends NodeComponent
{
    @Override
    public void process()
    {
        OrderContext ctx = this.getFirstContextBean();
        if (ctx.getUserId() == null || StringUtils.isEmpty(ctx.getSkuId()) || ctx.getQuantity() == null || ctx.getQuantity() <= 0)
        {
            throw new IllegalArgumentException("订单参数不完整");
        }
        ctx.addStep("validateOrder");
    }
}

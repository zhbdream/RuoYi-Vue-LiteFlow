package com.ruoyiliteflow.liteflow.component.demo.order;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent(value = "hasStock", name = "库存判断")
@Component
public class HasStockComponent extends NodeBooleanComponent
{
    @Override
    public boolean processBoolean()
    {
        OrderContext ctx = this.getFirstContextBean();
        boolean stock = ctx.getQuantity() <= 10;
        ctx.setHasStock(stock);
        ctx.addStep("hasStock=" + stock);
        return stock;
    }
}

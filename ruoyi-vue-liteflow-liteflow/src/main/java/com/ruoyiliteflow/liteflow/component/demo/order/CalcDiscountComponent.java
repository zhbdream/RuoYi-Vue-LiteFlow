package com.ruoyiliteflow.liteflow.component.demo.order;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "calcDiscount", name = "优惠计算")
@Component
public class CalcDiscountComponent extends NodeComponent
{
    @Override
    public void process()
    {
        OrderContext ctx = this.getFirstContextBean();
        BigDecimal discount = "SAVE10".equalsIgnoreCase(ctx.getCouponCode()) ? new BigDecimal("10.00") : BigDecimal.ZERO;
        ctx.setDiscountAmount(discount);
        ctx.setPayAmount(ctx.getOriginalAmount().subtract(discount));
        ctx.addStep("calcDiscount");
    }
}

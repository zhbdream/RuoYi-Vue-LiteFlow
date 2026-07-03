package com.ruoyiliteflow.liteflow.component.demo.pricing;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.PricingContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "calcFullReduction", name = "满减计算")
@Component
public class CalcFullReductionComponent extends NodeComponent
{
    @Override
    public void process()
    {
        PricingContext ctx = this.getFirstContextBean();
        if (ctx.getOriginalPrice().compareTo(new BigDecimal("200")) >= 0)
        {
            ctx.setReductionAmount(new BigDecimal("20"));
        }
        ctx.addStep("calcFullReduction");
    }
}

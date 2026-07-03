package com.ruoyiliteflow.liteflow.component.demo.pricing;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.PricingContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "buildPriceResult", name = "构建定价结果")
@Component
public class BuildPriceResultComponent extends NodeComponent
{
    @Override
    public void process()
    {
        PricingContext ctx = this.getFirstContextBean();
        ctx.setFinalPrice(ctx.getOriginalPrice()
                .subtract(ctx.getReductionAmount())
                .subtract(ctx.getCouponAmount()));
        ctx.addStep("buildPriceResult=" + ctx.getFinalPrice());
    }
}

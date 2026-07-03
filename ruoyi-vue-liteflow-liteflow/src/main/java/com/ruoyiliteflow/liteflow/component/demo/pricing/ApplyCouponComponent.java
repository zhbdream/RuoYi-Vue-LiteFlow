package com.ruoyiliteflow.liteflow.component.demo.pricing;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.PricingContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "applyCoupon", name = "优惠券抵扣")
@Component
public class ApplyCouponComponent extends NodeComponent
{
    @Override
    public void process()
    {
        PricingContext ctx = this.getFirstContextBean();
        Object req = this.getRequestData();
        if (req instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) req;
            String couponCode = String.valueOf(map.getOrDefault("couponCode", ""));
            ctx.setCouponCode(couponCode);
            if (couponCode != null && !couponCode.isEmpty() && !"null".equals(couponCode))
            {
                ctx.setCouponAmount(new BigDecimal("10"));
            }
        }
        ctx.addStep("applyCoupon");
    }
}

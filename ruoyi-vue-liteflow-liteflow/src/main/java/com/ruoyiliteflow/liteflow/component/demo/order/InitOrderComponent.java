package com.ruoyiliteflow.liteflow.component.demo.order;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.OrderContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "initOrder", name = "初始化订单")
@Component
public class InitOrderComponent extends NodeComponent
{
    @Override
    public void process()
    {
        OrderContext ctx = this.getFirstContextBean();
        Object req = this.getRequestData();
        if (req instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) req;
            if (map.get("userId") != null)
            {
                ctx.setUserId(Long.valueOf(String.valueOf(map.get("userId"))));
            }
            ctx.setSkuId(String.valueOf(map.getOrDefault("skuId", "")));
            if (map.get("quantity") != null)
            {
                ctx.setQuantity(Integer.valueOf(String.valueOf(map.get("quantity"))));
            }
            ctx.setPayType(String.valueOf(map.getOrDefault("payType", "wechat")));
            ctx.setCouponCode(String.valueOf(map.getOrDefault("couponCode", "")));
        }
        ctx.setOriginalAmount(new BigDecimal("199.00"));
        ctx.addStep("initOrder");
    }
}

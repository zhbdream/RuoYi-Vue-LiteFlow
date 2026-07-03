package com.ruoyiliteflow.liteflow.component.demo.pricing;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.PricingContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "loadMemberLevel", name = "加载会员等级")
@Component
public class LoadMemberLevelComponent extends NodeComponent
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
            if (map.get("userId") != null)
            {
                ctx.setUserId(Long.valueOf(String.valueOf(map.get("userId"))));
            }
            ctx.setMemberLevel(String.valueOf(map.getOrDefault("memberLevel", "NORMAL")));
        }
        else
        {
            ctx.setMemberLevel("NORMAL");
        }
        ctx.addStep("loadMemberLevel=" + ctx.getMemberLevel());
    }
}

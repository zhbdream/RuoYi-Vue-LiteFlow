package com.ruoyiliteflow.liteflow.component.demo.resilient;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.NotifyContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "notifyFallback", name = "通知降级")
@Component
public class NotifyFallbackComponent extends NodeComponent
{
    @Override
    public void process()
    {
        NotifyContext ctx = this.getFirstContextBean();
        ctx.setFallbackUsed(true);
        ctx.setNotified(true);
        ctx.addStep("notifyFallback");
    }
}

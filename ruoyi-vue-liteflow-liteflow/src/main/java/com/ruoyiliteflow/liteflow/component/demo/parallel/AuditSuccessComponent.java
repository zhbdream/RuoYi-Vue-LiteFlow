package com.ruoyiliteflow.liteflow.component.demo.parallel;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "auditSuccess", name = "审计通过")
@Component
public class AuditSuccessComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AuditContext ctx = this.getFirstContextBean();
        ctx.setMessage("audit passed for order " + ctx.getOrderId());
        ctx.addStep("auditSuccess");
    }
}

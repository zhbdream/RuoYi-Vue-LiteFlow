package com.ruoyiliteflow.liteflow.component.demo.parallel;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "auditReject", name = "审计拒绝")
@Component
public class AuditRejectComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AuditContext ctx = this.getFirstContextBean();
        ctx.setMessage("audit rejected for order " + ctx.getOrderId());
        ctx.addStep("auditReject");
    }
}

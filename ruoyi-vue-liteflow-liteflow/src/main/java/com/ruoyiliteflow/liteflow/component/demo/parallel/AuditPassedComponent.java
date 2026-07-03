package com.ruoyiliteflow.liteflow.component.demo.parallel;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent(value = "auditPassed", name = "审计是否通过")
@Component
public class AuditPassedComponent extends NodeBooleanComponent
{
    @Override
    public boolean processBoolean()
    {
        AuditContext ctx = this.getFirstContextBean();
        ctx.addStep("auditPassed=" + ctx.isAuditPassed());
        return ctx.isAuditPassed();
    }
}

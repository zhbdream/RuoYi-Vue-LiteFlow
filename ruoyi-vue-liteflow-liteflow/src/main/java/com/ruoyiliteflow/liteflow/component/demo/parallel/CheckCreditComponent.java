package com.ruoyiliteflow.liteflow.component.demo.parallel;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "checkCredit", name = "信用校验")
@Component
public class CheckCreditComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AuditContext ctx = this.getFirstContextBean();
        ctx.setCreditOk(true);
        ctx.addStep("checkCredit");
    }
}

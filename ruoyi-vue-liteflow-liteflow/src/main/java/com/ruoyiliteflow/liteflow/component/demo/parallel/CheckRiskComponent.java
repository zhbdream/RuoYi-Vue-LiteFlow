package com.ruoyiliteflow.liteflow.component.demo.parallel;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "checkRisk", name = "风控校验")
@Component
public class CheckRiskComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AuditContext ctx = this.getFirstContextBean();
        ctx.setRiskOk(true);
        ctx.addStep("checkRisk");
    }
}

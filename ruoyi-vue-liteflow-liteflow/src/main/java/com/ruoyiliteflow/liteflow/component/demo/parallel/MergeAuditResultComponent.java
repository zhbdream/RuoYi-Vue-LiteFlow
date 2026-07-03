package com.ruoyiliteflow.liteflow.component.demo.parallel;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "mergeAuditResult", name = "合并审计结果")
@Component
public class MergeAuditResultComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AuditContext ctx = this.getFirstContextBean();
        boolean passed = ctx.isInventoryOk() && ctx.isCreditOk() && ctx.isRiskOk();
        ctx.setAuditPassed(passed);
        ctx.addStep("mergeAuditResult=" + passed);
    }
}

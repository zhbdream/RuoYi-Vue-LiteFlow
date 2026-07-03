package com.ruoyiliteflow.liteflow.component.demo.batch;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.BatchContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeForComponent;

@LiteflowComponent(value = "batchCount", name = "批次数量")
@Component
public class BatchCountComponent extends NodeForComponent
{
    @Override
    public int processFor()
    {
        BatchContext ctx = this.getFirstContextBean();
        ctx.addStep("batchCount=" + ctx.getBatchCount());
        return ctx.getBatchCount();
    }
}

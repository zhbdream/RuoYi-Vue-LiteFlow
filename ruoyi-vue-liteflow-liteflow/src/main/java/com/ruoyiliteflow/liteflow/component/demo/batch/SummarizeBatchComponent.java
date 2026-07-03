package com.ruoyiliteflow.liteflow.component.demo.batch;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.BatchContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "summarizeBatch", name = "汇总批次")
@Component
public class SummarizeBatchComponent extends NodeComponent
{
    @Override
    public void process()
    {
        BatchContext ctx = this.getFirstContextBean();
        ctx.setSummary("processed " + ctx.getProcessedCount() + "/" + ctx.getBatchCount() + " items");
        ctx.addStep("summarizeBatch");
    }
}

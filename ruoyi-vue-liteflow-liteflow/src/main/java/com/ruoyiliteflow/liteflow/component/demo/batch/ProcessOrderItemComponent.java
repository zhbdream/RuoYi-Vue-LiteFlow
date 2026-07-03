package com.ruoyiliteflow.liteflow.component.demo.batch;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.BatchContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "processOrderItem", name = "处理订单项")
@Component
public class ProcessOrderItemComponent extends NodeComponent
{
    @Override
    public void process()
    {
        BatchContext ctx = this.getFirstContextBean();
        int index = this.getLoopIndex();
        ctx.setCurrentIndex(index);
        List<Map<String, Object>> items = ctx.getItems();
        if (index >= 0 && index < items.size())
        {
            Map<String, Object> item = items.get(index);
            String skuId = String.valueOf(item.getOrDefault("skuId", ""));
            ctx.getItemResults().add("processed:" + skuId);
        }
        ctx.setProcessedCount(ctx.getProcessedCount() + 1);
        ctx.addStep("processOrderItem[" + index + "]");
    }
}

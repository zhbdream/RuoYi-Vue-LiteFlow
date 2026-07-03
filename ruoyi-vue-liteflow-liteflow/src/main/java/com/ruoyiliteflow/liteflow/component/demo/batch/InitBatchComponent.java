package com.ruoyiliteflow.liteflow.component.demo.batch;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.BatchContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "initBatch", name = "初始化批次")
@Component
public class InitBatchComponent extends NodeComponent
{
    @Override
    public void process()
    {
        BatchContext ctx = this.getFirstContextBean();
        Object req = this.getRequestData();
        if (req instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) req;
            ctx.setOrderId(String.valueOf(map.getOrDefault("orderId", "")));
            Object itemsObj = map.get("items");
            if (itemsObj instanceof List)
            {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                ctx.setItems(items);
                ctx.setBatchCount(items.size());
            }
        }
        ctx.addStep("initBatch");
    }
}

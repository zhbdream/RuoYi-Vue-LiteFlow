package com.ruoyiliteflow.liteflow.component.demo.parallel;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.AuditContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "prepareAudit", name = "准备审计")
@Component
public class PrepareAuditComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AuditContext ctx = this.getFirstContextBean();
        Object req = this.getRequestData();
        if (req instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) req;
            ctx.setOrderId(String.valueOf(map.getOrDefault("orderId", "")));
        }
        ctx.addStep("prepareAudit");
    }
}

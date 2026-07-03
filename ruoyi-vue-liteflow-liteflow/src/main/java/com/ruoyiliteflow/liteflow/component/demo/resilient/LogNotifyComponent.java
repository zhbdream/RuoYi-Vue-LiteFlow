package com.ruoyiliteflow.liteflow.component.demo.resilient;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.NotifyContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "logNotify", name = "记录通知日志")
@Component
public class LogNotifyComponent extends NodeComponent
{
    @Override
    public void process()
    {
        NotifyContext ctx = this.getFirstContextBean();
        ctx.addStep("logNotify");
    }
}

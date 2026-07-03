package com.ruoyiliteflow.liteflow.component.demo.resilient;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.NotifyContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "sendNotify", name = "发送通知")
@Component
public class SendNotifyComponent extends NodeComponent
{
    @Override
    public void process()
    {
        NotifyContext ctx = this.getFirstContextBean();
        ctx.setAttemptCount(ctx.getAttemptCount() + 1);
        if (ctx.isSimulateFail() && ctx.getAttemptCount() < 3)
        {
            throw new RuntimeException("send notify failed, attempt=" + ctx.getAttemptCount());
        }
        ctx.setNotified(true);
        ctx.addStep("sendNotify");
    }
}

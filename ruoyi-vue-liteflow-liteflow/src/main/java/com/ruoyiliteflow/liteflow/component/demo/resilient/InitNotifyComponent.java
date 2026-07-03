package com.ruoyiliteflow.liteflow.component.demo.resilient;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.NotifyContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent(value = "initNotify", name = "初始化通知")
@Component
public class InitNotifyComponent extends NodeComponent
{
    @Override
    public void process()
    {
        NotifyContext ctx = this.getFirstContextBean();
        Object req = this.getRequestData();
        if (req instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) req;
            ctx.setUserId(String.valueOf(map.getOrDefault("userId", "")));
            ctx.setChannel(String.valueOf(map.getOrDefault("channel", "sms")));
            if (map.get("simulateFail") != null)
            {
                ctx.setSimulateFail(Boolean.parseBoolean(String.valueOf(map.get("simulateFail"))));
            }
        }
        ctx.addStep("initNotify");
    }
}

package com.ruoyiliteflow.langchain.component.demo;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.AiKitAgentContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * 从 requestData 写入 {@link AiKitAgentContext}。
 * <p>试跑入参示例：{@code {"agentCode":"rag","message":"七天无理由怎么退？"}}
 */
@LiteflowComponent(value = "aiKitAgentPrepare", name = "AIKit入参准备")
@Component
public class AiKitAgentPrepareComponent extends NodeComponent
{
    @Override
    public void process()
    {
        AiKitAgentContext ctx = this.getContextBean(AiKitAgentContext.class);
        Object reqData = this.getRequestData();
        if (reqData instanceof Map<?, ?> map)
        {
            Object code = map.get("agentCode");
            Object message = map.get("message");
            Object principal = map.get("principal");
            if (code != null && StringUtils.isNotEmpty(String.valueOf(code)))
            {
                ctx.setAgentCode(String.valueOf(code));
            }
            if (message != null)
            {
                ctx.setMessage(String.valueOf(message));
            }
            if (principal != null)
            {
                ctx.setPrincipal(String.valueOf(principal));
            }
        }
        else if (reqData != null && StringUtils.isEmpty(ctx.getMessage()))
        {
            ctx.setMessage(String.valueOf(reqData));
        }
        if (StringUtils.isEmpty(ctx.getAgentCode()))
        {
            ctx.setAgentCode("rag");
        }
    }
}

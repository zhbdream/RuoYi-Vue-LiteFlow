package com.ruoyiliteflow.langchain.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.runtime.AgentRunResult;
import com.ruoyiliteflow.aicore.runtime.AgentRuntime;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.AiKitAgentContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LiteFlow 薄适配：调用已配置的 AI Kit Agent（{@code AgentRuntime.invoke}）。
 * <p>EL：{@code THEN(aiKitAgentPrepare, aiKitAgent)}
 */
@LiteflowComponent(value = "aiKitAgent", name = "AIKit智能体")
@Component
public class AiKitAgentComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(AiKitAgentComponent.class);

    @Autowired
    private AgentRuntime agentRuntime;

    @Override
    public void process()
    {
        AiKitAgentContext ctx = this.getContextBean(AiKitAgentContext.class);
        String agentCode = StringUtils.isEmpty(ctx.getAgentCode()) ? "rag" : ctx.getAgentCode();
        String message = ctx.getMessage();
        if (StringUtils.isEmpty(message))
        {
            Object req = this.getRequestData();
            message = req == null ? "" : String.valueOf(req);
            ctx.setMessage(message);
        }

        AgentRunRequest req = new AgentRunRequest();
        req.setMessage(message);
        req.setPrincipal(StringUtils.isNotEmpty(ctx.getPrincipal()) ? ctx.getPrincipal() : currentUsername());

        log.info("aiKitAgent invoke agentCode={}", agentCode);
        AgentRunResult result = agentRuntime.invoke(agentCode, req);
        ctx.setAnswer(result.getContent());
        ctx.setModel(result.getModel());
        this.getSlot().setResponseData(result.getContent());
        log.info("aiKitAgent done model={} answerLen={}", result.getModel(),
                result.getContent() == null ? 0 : result.getContent().length());
    }

    private String currentUsername()
    {
        try
        {
            String name = SecurityUtils.getUsername();
            return StringUtils.isEmpty(name) ? "anonymous" : name;
        }
        catch (Exception e)
        {
            return "anonymous";
        }
    }
}

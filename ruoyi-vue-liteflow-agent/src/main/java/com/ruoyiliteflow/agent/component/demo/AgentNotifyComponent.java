package com.ruoyiliteflow.agent.component.demo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.domain.AgentRiskContext;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * Demo7：根据 Agent 回复整理风控结论
 */
@LiteflowComponent(value = "agentNotify", name = "Agent风控后处理")
@Component
public class AgentNotifyComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(AgentNotifyComponent.class);

    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "(?:风险等级|risk\\s*level)\\s*[:：]?\\s*(HIGH|MEDIUM|LOW)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void process()
    {
        AgentRiskContext ctx = this.getContextBean(AgentRiskContext.class);
        String reply = ctx.getAgentReply();
        if (StringUtils.isEmpty(reply))
        {
            Object slotReply = this.getSlot().getResponseData();
            if (slotReply != null)
            {
                reply = String.valueOf(slotReply);
                ctx.setAgentReply(reply);
            }
        }
        String level = parseRiskLevel(reply);
        // 避免「降低风险」等表述干扰：已由 parseRiskLevel 优先匹配「风险等级：XXX」
        ctx.setRiskLevel(level);
        ctx.setNotified(true);
        log.info("agentNotify done: riskLevel={}, reply={}", level, reply);
    }

    static String parseRiskLevel(String reply)
    {
        if (StringUtils.isEmpty(reply))
        {
            return "MEDIUM";
        }
        Matcher m = LEVEL_PATTERN.matcher(reply);
        if (m.find())
        {
            return m.group(1).toUpperCase();
        }
        String upper = reply.toUpperCase();
        // 避免把「降低风险」误判为 LOW：仅匹配独立等级词
        if (upper.matches("(?s).*\\bHIGH\\b.*") || reply.contains("高风险") || reply.contains("拒绝"))
        {
            return "HIGH";
        }
        if (upper.matches("(?s).*\\bMEDIUM\\b.*") || reply.contains("中风险") || reply.contains("中等风险"))
        {
            return "MEDIUM";
        }
        if (upper.matches("(?s).*\\bLOW\\b.*") || reply.contains("低风险") || reply.contains("放行"))
        {
            return "LOW";
        }
        return "MEDIUM";
    }
}

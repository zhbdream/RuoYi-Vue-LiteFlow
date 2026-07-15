package com.ruoyiliteflow.langchain.component.demo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * Demo：根据 LC4j / LangGraph 回复整理风控结论
 */
@LiteflowComponent(value = "lc4jNotify", name = "LC4j风控后处理")
@Component
public class Lc4jNotifyComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jNotifyComponent.class);

    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "(?:风险等级|risk\\s*level)\\s*[:：]?\\s*(HIGH|MEDIUM|LOW)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void process()
    {
        Lc4jRiskContext ctx = this.getContextBean(Lc4jRiskContext.class);
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
        if (StringUtils.isEmpty(ctx.getRiskLevel()))
        {
            ctx.setRiskLevel(parseRiskLevel(reply));
        }
        ctx.setNotified(true);
        log.info("lc4jNotify done: riskLevel={}, reply={}", ctx.getRiskLevel(), reply);
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
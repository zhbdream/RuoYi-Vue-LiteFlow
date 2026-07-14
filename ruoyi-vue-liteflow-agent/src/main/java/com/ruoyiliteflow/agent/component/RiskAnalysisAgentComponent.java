package com.ruoyiliteflow.agent.component;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.domain.AgentRiskContext;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.agent.support.AgentCredentialApplier;
import com.ruoyiliteflow.agent.tool.ChainMetaTool;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.support.AgentStreamMode;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.openai.DeepSeek;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.Msg;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * Demo7 风控分析 Agent（DeepSeek）。
 * <p>EL 示例：{@code THEN(agentPrepare, riskAgent, agentNotify)}
 * <p>优先使用「模型配置」页默认模型的加密 Key；否则回退 yml / 环境变量。
 */
@LiteflowComponent(value = "riskAgent", name = "DeepSeek风控Agent")
@Component
public class RiskAnalysisAgentComponent extends ReActAgentComponent
{
    @Value("${liteflow.agent.demo.model:deepseek-chat}")
    private String modelName;

    @Autowired
    private ChainMetaTool chainMetaTool;

    @Autowired
    private AgentCredentialApplier credentialApplier;

    @Autowired
    private IAgentQuotaService agentQuotaService;

    @Override
    protected ModelSpec<?> model()
    {
        agentQuotaService.assertWithinQuota(currentUsername(), this.getChainId());
        String runtimeModel = credentialApplier.applyDefault(this.agentConfig());
        String name = StringUtils.isNotEmpty(runtimeModel) ? runtimeModel : modelName;
        var spec = DeepSeek.of(name).temperature(0.2);
        if (AgentStreamMode.isEnabled())
        {
            spec.stream(true);
        }
        return spec;
    }

    @Override
    protected String systemPrompt()
    {
        return """
                你是电商支付前的风控分析助理，只做风险研判与建议，不要编造未给出的事实。
                输出要求：
                1) 先给风险等级：LOW / MEDIUM / HIGH 之一
                2) 再用 2~4 句中文说明理由与建议
                如需了解订单上下文，可调用工具 read_order_risk_context；
                如需对照系统中已有链路说明，可调用 query_chain_meta。
                """;
    }

    @Override
    protected String userPrompt()
    {
        AgentRiskContext ctx = this.getContextBean(AgentRiskContext.class);
        if (ctx == null)
        {
            Object req = this.getRequestData();
            return "请对以下入参做风控分析：" + (req == null ? "{}" : String.valueOf(req));
        }
        return "请分析以下待支付订单风险：orderId=" + ctx.getOrderId()
                + ", userId=" + ctx.getUserId()
                + ", userType=" + ctx.getUserType()
                + ", amount=" + ctx.getAmount()
                + ", scene=" + ctx.getScene()
                + "。必要时可调用工具补充信息。";
    }

    @Override
    protected List<Object> tools()
    {
        // tools() 在 process 线程构建 Agent 时调用，此处可安全 getSlot；勿在 Tool 回调里再调 getSlot/ctx（异步线程无 ThreadLocal）
        return List.of(chainMetaTool, new ContextReadTool(this.getSlot()));
    }

    /**
     * 每次执行用独立 conversation，避免缓存 Agent 绑到过期 Slot。
     */
    @Override
    protected String resolveConversationId()
    {
        try
        {
            String requestId = this.getSlot() == null ? null : this.getSlot().getRequestId();
            if (StringUtils.isNotEmpty(requestId))
            {
                return requestId;
            }
        }
        catch (Exception ignored)
        {
            // fall through
        }
        return super.resolveConversationId();
    }

    @Override
    protected boolean enableShellTool()
    {
        return false;
    }

    @Override
    protected boolean enableWorkspaceFileTools()
    {
        return false;
    }

    @Override
    protected int maxIterations()
    {
        return 6;
    }

    @Override
    protected void handleReply(Msg reply)
    {
        String text = reply == null ? null : reply.getTextContent();
        this.getSlot().setResponseData(text);
        try
        {
            AgentRiskContext ctx = this.getContextBean(AgentRiskContext.class);
            if (ctx != null && StringUtils.isNotEmpty(text))
            {
                ctx.setAgentReply(text);
            }
        }
        catch (Exception ignored)
        {
            // 无上下文时仅写 slot
        }
        long tokens = estimateTokens(text);
        try
        {
            Object usage = this.ctx().getChatUsage();
            if (usage != null)
            {
                tokens = Math.max(tokens, extractTotalTokens(usage));
            }
        }
        catch (Exception ignored)
        {
            // 用量不可用时用估算
        }
        agentQuotaService.recordUsage(currentUsername(), this.getChainId(), tokens);
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }

    private long estimateTokens(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return 1L;
        }
        return Math.max(1L, text.length() / 2L);
    }

    private long extractTotalTokens(Object usage)
    {
        try
        {
            var m = usage.getClass().getMethod("getTotalTokens");
            Object v = m.invoke(usage);
            if (v instanceof Number n)
            {
                return n.longValue();
            }
        }
        catch (Exception ignored)
        {
            // ignore
        }
        try
        {
            var m = usage.getClass().getMethod("getTotal");
            Object v = m.invoke(usage);
            if (v instanceof Number n)
            {
                return n.longValue();
            }
        }
        catch (Exception ignored)
        {
            // ignore
        }
        return 0L;
    }

    /**
     * 内部工具：持有构建期捕获的 Slot，异步回调可安全读 ContextBean。
     */
    public static class ContextReadTool
    {
        private final Slot slot;

        public ContextReadTool(Slot slot)
        {
            this.slot = slot;
        }

        @Tool(name = "read_order_risk_context", description = "读取当前链路 AgentRiskContext 中的订单风控字段")
        public String readContext(@ToolParam(name = "unused", description = "占位参数，可传 empty") String unused)
        {
            try
            {
                if (slot == null)
                {
                    return "{\"error\":\"slot is null\"}";
                }
                AgentRiskContext ctx = slot.getContextBean(AgentRiskContext.class);
                if (ctx == null)
                {
                    return "{\"error\":\"AgentRiskContext not found\"}";
                }
                return "{\"orderId\":\"" + nvl(ctx.getOrderId()) + "\""
                        + ",\"userId\":" + ctx.getUserId()
                        + ",\"userType\":\"" + nvl(ctx.getUserType()) + "\""
                        + ",\"amount\":\"" + ctx.getAmount() + "\""
                        + ",\"scene\":\"" + nvl(ctx.getScene()) + "\"}";
            }
            catch (Exception e)
            {
                return "{\"error\":\"" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().replace("\"", "'")) + "\"}";
            }
        }

        private String nvl(String s)
        {
            return s == null ? "" : s.replace("\"", "'");
        }
    }
}

package com.ruoyiliteflow.langchain.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.assistant.RiskAssistant;
import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import com.ruoyiliteflow.langchain.support.Lc4jChatModelFactory;
import com.ruoyiliteflow.langchain.tool.RiskContextTool;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

/**
 * LangChain4j 节点：AiServices + Tool Calling。
 * <p>EL：{@code THEN(lc4jPrepare, lc4jChat, lc4jNotify)}
 */
@LiteflowComponent(value = "lc4jChat", name = "LangChain4j风控Chat")
@Component
public class Lc4jChatRiskComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jChatRiskComponent.class);

    @Autowired
    private Lc4jChatModelFactory chatModelFactory;

    @Autowired
    private IAgentQuotaService agentQuotaService;

    @Override
    public void process()
    {
        agentQuotaService.assertWithinQuota(currentUsername(), this.getChainId());
        Lc4jRiskContext ctx = this.getContextBean(Lc4jRiskContext.class);

        ChatModel model = chatModelFactory.createChatModel();
        RiskAssistant assistant = AiServices.builder(RiskAssistant.class)
                .chatModel(model)
                .tools(new RiskContextTool(ctx))
                .build();

        String userMessage = buildUserMessage(ctx);
        log.info("lc4jChat invoke: orderId={}", ctx.getOrderId());
        String reply = assistant.analyze(userMessage);
        ctx.setAgentReply(reply);
        this.getSlot().setResponseData(reply);
        log.info("lc4jChat done: replyLen={}", reply == null ? 0 : reply.length());
    }

    private String buildUserMessage(Lc4jRiskContext ctx)
    {
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
                + "。如需细节可调用工具 read_order_risk_context。";
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
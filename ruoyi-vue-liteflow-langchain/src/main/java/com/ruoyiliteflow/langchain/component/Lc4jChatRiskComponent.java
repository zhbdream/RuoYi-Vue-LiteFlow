package com.ruoyiliteflow.langchain.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.facade.IAiRiskFacade;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeRequest;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeResult;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LangChain4j 节点：委托 {@link IAiRiskFacade}（与独立 Agent / MCP 共用内核）。
 * <p>EL：{@code THEN(lc4jPrepare, lc4jChat, lc4jNotify)}
 */
@LiteflowComponent(value = "lc4jChat", name = "LangChain4j风控Chat")
@Component
public class Lc4jChatRiskComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jChatRiskComponent.class);

    @Autowired
    private IAiRiskFacade aiRiskFacade;

    @Override
    public void process()
    {
        Lc4jRiskContext ctx = this.getContextBean(Lc4jRiskContext.class);
        RiskAnalyzeRequest req = new RiskAnalyzeRequest();
        if (ctx != null)
        {
            req.setOrderId(ctx.getOrderId());
            req.setUserId(ctx.getUserId());
            req.setUserType(ctx.getUserType());
            req.setAmount(ctx.getAmount());
            req.setScene(ctx.getScene());
        }
        req.setPrincipal(currentUsername());

        log.info("lc4jChat invoke via ai-core facade: orderId={}", req.getOrderId());
        RiskAnalyzeResult result = aiRiskFacade.analyze(req);
        String reply = result.getAnalysis();
        if (ctx != null)
        {
            ctx.setAgentReply(reply);
            ctx.setRiskLevel(result.getRiskLevel());
        }
        this.getSlot().setResponseData(reply);
        log.info("lc4jChat done: replyLen={}", reply == null ? 0 : reply.length());
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

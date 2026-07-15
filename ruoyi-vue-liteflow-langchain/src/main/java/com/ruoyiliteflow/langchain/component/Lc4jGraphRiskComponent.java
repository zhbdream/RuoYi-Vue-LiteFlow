package com.ruoyiliteflow.langchain.component;

import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import com.ruoyiliteflow.langchain.graph.RiskGraphBuilder;
import com.ruoyiliteflow.langchain.graph.RiskGraphState;
import com.ruoyiliteflow.langchain.support.Lc4jChatModelFactory;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import dev.langchain4j.model.chat.ChatModel;
import org.bsc.langgraph4j.CompiledGraph;

/**
 * LangGraph4j 节点：在单个 LiteFlow 节点内跑 StateGraph（含条件边）。
 * <p>EL：{@code THEN(lc4jPrepare, lc4jGraph, lc4jNotify)}
 */
@LiteflowComponent(value = "lc4jGraph", name = "LangGraph4j风控图")
@Component
public class Lc4jGraphRiskComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jGraphRiskComponent.class);

    @Autowired
    private Lc4jChatModelFactory chatModelFactory;

    @Autowired
    private IAgentQuotaService agentQuotaService;

    @Override
    public void process() throws Exception
    {
        agentQuotaService.assertWithinQuota(currentUsername(), this.getChainId());
        Lc4jRiskContext ctx = this.getContextBean(Lc4jRiskContext.class);

        ChatModel model = chatModelFactory.createChatModel();
        CompiledGraph<RiskGraphState> graph = RiskGraphBuilder.build(model, ctx).compile();

        RiskGraphState last = null;
        for (var item : graph.stream(Map.of()))
        {
            last = item.state();
            log.debug("lc4jGraph step node={} messages={}", item.node(), last.messages());
        }
        if (last == null)
        {
            throw new IllegalStateException("LangGraph4j 未产生任何状态输出");
        }

        String reply = last.analysis();
        String level = last.riskLevel();
        String trace = last.messages().stream().collect(Collectors.joining(" | "));

        ctx.setAgentReply(reply);
        ctx.setRiskLevel(level);
        ctx.setGraphTrace(trace);
        this.getSlot().setResponseData(reply);
        log.info("lc4jGraph done: riskLevel={}, trace={}", level, trace);
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
package com.ruoyiliteflow.langchain.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.facade.IAiRagFacade;
import com.ruoyiliteflow.aicore.model.RagAskRequest;
import com.ruoyiliteflow.aicore.model.RagAskResult;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRagContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * LangChain4j RAG 节点：委托 {@link IAiRagFacade}。
 * <p>EL：{@code THEN(lc4jRagPrepare, lc4jRag, lc4jRagNotify)}
 */
@LiteflowComponent(value = "lc4jRag", name = "LangChain4jRAG问答")
@Component
public class Lc4jRagComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jRagComponent.class);

    @Autowired
    private IAiRagFacade aiRagFacade;

    @Value("${liteflow.langchain.rag.max-results:3}")
    private int maxResults;

    @Value("${liteflow.langchain.rag.min-score:0.45}")
    private double minScore;

    @Override
    public void process()
    {
        Lc4jRagContext ctx = this.getContextBean(Lc4jRagContext.class);
        String question = ctx.getQuestion();
        if (StringUtils.isEmpty(question))
        {
            Object reqData = this.getRequestData();
            question = reqData == null ? "" : String.valueOf(reqData);
            ctx.setQuestion(question);
        }

        RagAskRequest req = new RagAskRequest();
        req.setQuestion(question);
        req.setPrincipal(currentUsername());
        req.setMaxResults(maxResults);
        req.setMinScore(minScore);

        log.info("lc4jRag invoke via ai-core facade: question={}", question);
        RagAskResult result = aiRagFacade.ask(req);
        ctx.setHitCount(result.getHitCount());
        ctx.setRetrievedContext(result.getRetrievedContext());
        ctx.setAnswer(result.getAnswer());
        this.getSlot().setResponseData(result.getAnswer());
        log.info("lc4jRag done: hits={}, answerLen={}", result.getHitCount(),
                result.getAnswer() == null ? 0 : result.getAnswer().length());
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

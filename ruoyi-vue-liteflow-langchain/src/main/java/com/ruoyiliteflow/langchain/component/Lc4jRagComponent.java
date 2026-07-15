package com.ruoyiliteflow.langchain.component;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.service.IAgentQuotaService;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRagContext;
import com.ruoyiliteflow.langchain.rag.RagKnowledgeBase;
import com.ruoyiliteflow.langchain.support.Lc4jChatModelFactory;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;

/**
 * LangChain4j RAG 节点：向量检索售后知识 + ChatModel 生成答案。
 * <p>EL：{@code THEN(lc4jRagPrepare, lc4jRag, lc4jRagNotify)}
 */
@LiteflowComponent(value = "lc4jRag", name = "LangChain4jRAG问答")
@Component
public class Lc4jRagComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jRagComponent.class);

    @Autowired
    private RagKnowledgeBase knowledgeBase;

    @Autowired
    private Lc4jChatModelFactory chatModelFactory;

    @Autowired
    private IAgentQuotaService agentQuotaService;

    @Value("${liteflow.langchain.rag.max-results:3}")
    private int maxResults;

    @Value("${liteflow.langchain.rag.min-score:0.45}")
    private double minScore;

    @Override
    public void process()
    {
        agentQuotaService.assertWithinQuota(currentUsername(), this.getChainId());
        Lc4jRagContext ctx = this.getContextBean(Lc4jRagContext.class);
        String question = ctx.getQuestion();
        if (StringUtils.isEmpty(question))
        {
            Object req = this.getRequestData();
            question = req == null ? "" : String.valueOf(req);
            ctx.setQuestion(question);
        }

        List<EmbeddingMatch<TextSegment>> matches = knowledgeBase.search(question, maxResults, minScore);
        String retrieved = formatMatches(matches);
        ctx.setHitCount(matches.size());
        ctx.setRetrievedContext(retrieved);

        ChatModel model = chatModelFactory.createChatModel();
        String prompt = """
                你是电商售后政策助手。请仅根据下列「参考资料」回答用户问题。
                若资料不足，请明确说明「资料未覆盖」，不要编造政策。
                回答使用简洁中文，必要时分点列出。

                【参考资料】
                %s

                【用户问题】
                %s
                """.formatted(StringUtils.isEmpty(retrieved) ? "（未检索到相关片段）" : retrieved, question);

        log.info("lc4jRag invoke: hits={}, question={}", matches.size(), question);
        String answer = model.chat(prompt);
        ctx.setAnswer(answer);
        this.getSlot().setResponseData(answer);
        log.info("lc4jRag done: answerLen={}", answer == null ? 0 : answer.length());
    }

    private static String formatMatches(List<EmbeddingMatch<TextSegment>> matches)
    {
        if (matches == null || matches.isEmpty())
        {
            return "";
        }
        return matches.stream().map(m -> {
            String source = "";
            if (m.embedded() != null && m.embedded().metadata() != null)
            {
                Object src = m.embedded().metadata().toMap().get("source");
                source = src == null ? "" : String.valueOf(src);
            }
            String text = m.embedded() == null ? "" : m.embedded().text();
            return "- source=" + source + ", score=" + String.format("%.3f", m.score()) + "\n" + text;
        }).collect(Collectors.joining("\n\n"));
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
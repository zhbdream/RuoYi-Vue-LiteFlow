package com.ruoyiliteflow.aicore.facade.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.facade.IAiRagFacade;
import com.ruoyiliteflow.aicore.model.RagAskRequest;
import com.ruoyiliteflow.aicore.model.RagAskResult;
import com.ruoyiliteflow.aicore.rag.AiRagKnowledgeBase;
import com.ruoyiliteflow.aicore.spi.AiQuotaGuard;
import com.ruoyiliteflow.aicore.support.AiChatModelFactory;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;

@Service
public class AiRagFacadeImpl implements IAiRagFacade
{
    private static final Logger log = LoggerFactory.getLogger(AiRagFacadeImpl.class);

    @Autowired
    private AiRagKnowledgeBase knowledgeBase;

    @Autowired
    private AiChatModelFactory chatModelFactory;

    @Autowired(required = false)
    private AiQuotaGuard quotaGuard;

    @Value("${ruoyi.ai.rag.max-results:3}")
    private int defaultMaxResults;

    @Value("${ruoyi.ai.rag.min-score:0.45}")
    private double defaultMinScore;

    @Value("${ruoyi.ai.quota.dimension.rag:agent:rag}")
    private String quotaDimension;

    @Override
    public RagAskResult ask(RagAskRequest request)
    {
        if (quotaGuard != null)
        {
            String principal = StringUtils.isEmpty(request.getPrincipal()) ? "anonymous" : request.getPrincipal();
            quotaGuard.assertWithinQuota(principal, quotaDimension);
        }

        String question = request.getQuestion();
        int maxResults = request.getMaxResults() == null ? defaultMaxResults : request.getMaxResults();
        double minScore = request.getMinScore() == null ? defaultMinScore : request.getMinScore();

        List<EmbeddingMatch<TextSegment>> matches = knowledgeBase.search(question, maxResults, minScore);
        String retrieved = knowledgeBase.formatMatches(matches);

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

        log.info("ai-core rag ask: hits={}", matches.size());
        String answer = model.chat(prompt);

        RagAskResult result = new RagAskResult();
        result.setAnswer(answer);
        result.setRetrievedContext(retrieved);
        result.setHitCount(matches.size());
        return result;
    }
}

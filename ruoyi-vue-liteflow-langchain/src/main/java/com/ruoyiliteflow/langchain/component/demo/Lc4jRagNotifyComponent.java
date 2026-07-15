package com.ruoyiliteflow.langchain.component.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRagContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * Demo10：RAG 后处理（落库前整理）
 */
@LiteflowComponent(value = "lc4jRagNotify", name = "LC4jRAG后处理")
@Component
public class Lc4jRagNotifyComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jRagNotifyComponent.class);

    @Override
    public void process()
    {
        Lc4jRagContext ctx = this.getContextBean(Lc4jRagContext.class);
        if (StringUtils.isEmpty(ctx.getAnswer()))
        {
            Object slot = this.getSlot().getResponseData();
            if (slot != null)
            {
                ctx.setAnswer(String.valueOf(slot));
            }
        }
        ctx.setNotified(true);
        log.info("lc4jRagNotify done: hitCount={}, answerLen={}",
                ctx.getHitCount(),
                ctx.getAnswer() == null ? 0 : ctx.getAnswer().length());
    }
}
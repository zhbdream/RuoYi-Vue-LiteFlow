package com.ruoyiliteflow.langchain.component.demo;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.langchain.domain.Lc4jRagContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * Demo10：整理售后问答入参到 {@link Lc4jRagContext}
 */
@LiteflowComponent(value = "lc4jRagPrepare", name = "LC4jRAG准备")
@Component
public class Lc4jRagPrepareComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jRagPrepareComponent.class);

    private static final String DEFAULT_QUESTION =
            "下单后第5天衣服尺码不合适，可以退货或换货吗？运费谁承担？";

    @Override
    public void process()
    {
        Lc4jRagContext ctx = this.getContextBean(Lc4jRagContext.class);
        Object req = this.getRequestData();
        Map<String, Object> map = toMap(req);
        String question = str(map.get("question"), DEFAULT_QUESTION);
        ctx.setQuestion(question);
        ctx.setPrepared(true);
        log.info("lc4jRagPrepare done: question={}", question);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object req)
    {
        if (req == null)
        {
            return Map.of();
        }
        if (req instanceof Map)
        {
            return (Map<String, Object>) req;
        }
        return JSON.parseObject(JSON.toJSONString(req));
    }

    private String str(Object v, String def)
    {
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v).trim();
    }
}
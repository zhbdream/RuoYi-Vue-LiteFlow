package com.ruoyiliteflow.aikit.platform.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * 默认真机本地 All-MiniLM；配置了 base-url + api-key 则走远程 OpenAI 兼容 Embedding。
 */
@Component
public class KitEmbeddingModelFactory
{
    private static final Logger log = LoggerFactory.getLogger(KitEmbeddingModelFactory.class);

    @Value("${ruoyi.ai-kit.embedding.base-url:}")
    private String baseUrl;

    @Value("${ruoyi.ai-kit.embedding.api-key:}")
    private String apiKey;

    @Value("${ruoyi.ai-kit.embedding.model:}")
    private String model;

    private volatile EmbeddingModel cached;
    private volatile String source = "local-all-minilm";

    public EmbeddingModel get()
    {
        if (cached == null)
        {
            synchronized (this)
            {
                if (cached == null)
                {
                    cached = create();
                }
            }
        }
        return cached;
    }

    public String source()
    {
        get();
        return source;
    }

    private EmbeddingModel create()
    {
        if (StringUtils.isNotEmpty(baseUrl) && StringUtils.isNotEmpty(apiKey))
        {
            String name = StringUtils.isEmpty(model) ? "text-embedding-3-small" : model;
            source = "remote:" + name;
            log.info("Knowledge embedding uses remote model={} baseUrl={}", name, baseUrl);
            return OpenAiEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey)
                    .modelName(name)
                    .build();
        }
        source = "local-all-minilm";
        log.info("Knowledge embedding uses local All-MiniLM");
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }
}

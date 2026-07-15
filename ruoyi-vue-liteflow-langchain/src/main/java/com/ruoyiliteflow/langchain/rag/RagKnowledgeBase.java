package com.ruoyiliteflow.langchain.rag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;

/**
 * 内存向量知识库：启动时加载 classpath:kb/*.md 并建立索引。
 * Embedding 使用本地 All-MiniLM（不依赖外部 Embedding API）。
 */
@Component
public class RagKnowledgeBase
{
    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeBase.class);

    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private volatile int segmentCount;

    @PostConstruct
    public void init() throws IOException
    {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:kb/*.md");
        List<Document> documents = new ArrayList<>();
        for (Resource resource : resources)
        {
            if (!resource.exists() || !resource.isReadable())
            {
                continue;
            }
            String text = readUtf8(resource);
            String name = resource.getFilename() == null ? "unknown.md" : resource.getFilename();
            documents.add(Document.from(text, Metadata.from(Map.of("source", name))));
        }
        if (documents.isEmpty())
        {
            log.warn("RAG knowledge base empty: no classpath:kb/*.md found");
            return;
        }
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(400, 40))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);
        segmentCount = documents.size();
        log.info("RAG knowledge base ready: docs={}, embedding=AllMiniLmL6V2Quantized", documents.size());
    }

    /**
     * @return 命中片段（已按相关度排序）
     */
    public List<EmbeddingMatch<TextSegment>> search(String question, int maxResults, double minScore)
    {
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches();
    }

    public int getDocumentCount()
    {
        return segmentCount;
    }

    private static String readUtf8(Resource resource) throws IOException
    {
        try (InputStream in = resource.getInputStream())
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
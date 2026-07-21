package com.ruoyiliteflow.aicore.rag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

@Component
public class AiRagKnowledgeBase
{
    private static final Logger log = LoggerFactory.getLogger(AiRagKnowledgeBase.class);

    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private volatile int documentCount;

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
            log.warn("AI-core RAG knowledge base empty: no classpath:kb/*.md found");
            return;
        }
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(400, 40))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);
        documentCount = documents.size();
        log.info("AI-core RAG ready: docs={}", documents.size());
    }

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

    public String formatMatches(List<EmbeddingMatch<TextSegment>> matches)
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

    public int getDocumentCount()
    {
        return documentCount;
    }

    private static String readUtf8(Resource resource) throws IOException
    {
        try (InputStream in = resource.getInputStream())
        {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

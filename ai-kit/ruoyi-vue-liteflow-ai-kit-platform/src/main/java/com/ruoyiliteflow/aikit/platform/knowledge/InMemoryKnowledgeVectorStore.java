package com.ruoyiliteflow.aikit.platform.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.spi.KnowledgeHit;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 可插拔向量存储（当前实现：内存 InMemory + 本地 All-MiniLM）。
 * 分片文本落库，进程启动/重建索引时载入内存。
 */
@Component
public class InMemoryKnowledgeVectorStore
{
    private static final Logger log = LoggerFactory.getLogger(InMemoryKnowledgeVectorStore.class);

    private final EmbeddingModel embeddingModel;
    private final ConcurrentHashMap<String, EmbeddingStore<TextSegment>> stores = new ConcurrentHashMap<>();

    public InMemoryKnowledgeVectorStore(KitEmbeddingModelFactory embeddingModelFactory)
    {
        this.embeddingModel = embeddingModelFactory.get();
    }

    public List<String> split(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return List.of();
        }
        Document doc = Document.from(text);
        List<TextSegment> segments = DocumentSplitters.recursive(400, 40).split(doc);
        List<String> out = new ArrayList<>();
        for (TextSegment seg : segments)
        {
            if (seg != null && StringUtils.isNotEmpty(seg.text()))
            {
                out.add(seg.text());
            }
        }
        return out;
    }

    public void replaceKb(String kbCode, List<ChunkItem> chunks)
    {
        if (StringUtils.isEmpty(kbCode))
        {
            return;
        }
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        if (chunks != null)
        {
            for (ChunkItem item : chunks)
            {
                if (item == null || StringUtils.isEmpty(item.content()))
                {
                    continue;
                }
                TextSegment segment = TextSegment.from(item.content(),
                        Metadata.from(Map.of(
                                "kbCode", kbCode,
                                "source", item.source() == null ? "" : item.source(),
                                "docId", String.valueOf(item.docId()))));
                Embedding embedding = embeddingModel.embed(segment).content();
                store.add(embedding, segment);
            }
        }
        stores.put(kbCode, store);
        log.info("Knowledge vector store ready kbCode={} chunks={}", kbCode, chunks == null ? 0 : chunks.size());
    }

    public void removeKb(String kbCode)
    {
        if (StringUtils.isNotEmpty(kbCode))
        {
            stores.remove(kbCode);
        }
    }

    public String search(List<String> kbCodes, String query, int maxResults, double minScore)
    {
        List<KnowledgeHit> hits = searchHits(kbCodes, query, maxResults, minScore);
        if (hits.isEmpty())
        {
            return "";
        }
        return hits.stream().map(h -> "- source=" + (h.getSource() == null ? "" : h.getSource())
                + ", score=" + String.format("%.3f", h.getScore()) + "\n" + h.getText())
                .collect(Collectors.joining("\n\n"));
    }

    public List<KnowledgeHit> searchHits(List<String> kbCodes, String query, int maxResults, double minScore)
    {
        List<KnowledgeHit> hits = new ArrayList<>();
        if (kbCodes == null || kbCodes.isEmpty() || StringUtils.isEmpty(query))
        {
            return hits;
        }
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> all = new ArrayList<>();
        Map<EmbeddingMatch<TextSegment>, String> codeOf = new java.util.HashMap<>();
        for (String code : kbCodes)
        {
            EmbeddingStore<TextSegment> store = stores.get(code);
            if (store == null)
            {
                continue;
            }
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .minScore(minScore)
                    .build();
            EmbeddingSearchResult<TextSegment> result = store.search(request);
            for (EmbeddingMatch<TextSegment> m : result.matches())
            {
                all.add(m);
                codeOf.put(m, code);
            }
        }
        if (all.isEmpty())
        {
            return hits;
        }
        all.sort((a, b) -> Double.compare(b.score(), a.score()));
        int limit = Math.min(maxResults, all.size());
        for (EmbeddingMatch<TextSegment> m : all.subList(0, limit))
        {
            String source = "";
            if (m.embedded() != null && m.embedded().metadata() != null)
            {
                Object src = m.embedded().metadata().toMap().get("source");
                source = src == null ? "" : String.valueOf(src);
            }
            String text = m.embedded() == null ? "" : m.embedded().text();
            hits.add(new KnowledgeHit(codeOf.get(m), source, m.score(), text));
        }
        return hits;
    }

    public record ChunkItem(long docId, String source, String content)
    {
    }
}

package com.ruoyiliteflow.aikit.platform.knowledge;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.spi.KnowledgeRetriever;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeBase;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeChunk;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeDoc;
import com.ruoyiliteflow.aikit.platform.mapper.AiKnowledgeBaseMapper;
import com.ruoyiliteflow.aikit.platform.mapper.AiKnowledgeChunkMapper;
import com.ruoyiliteflow.aikit.platform.mapper.AiKnowledgeDocMapper;
import com.ruoyiliteflow.common.utils.StringUtils;

@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformKnowledgeRetriever implements KnowledgeRetriever
{
    private static final Logger log = LoggerFactory.getLogger(PlatformKnowledgeRetriever.class);

    @Autowired
    private AiKnowledgeBaseMapper kbMapper;

    @Autowired
    private AiKnowledgeDocMapper docMapper;

    @Autowired
    private AiKnowledgeChunkMapper chunkMapper;

    @Autowired
    private InMemoryKnowledgeVectorStore vectorStore;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady()
    {
        Thread t = new Thread(this::warmUp, "aikit-kb-warmup");
        t.setDaemon(true);
        t.start();
    }

    public void warmUp()
    {
        try
        {
            int loaded = 0;
            int rebuilt = 0;
            List<AiKnowledgeBase> list = kbMapper.selectAiKnowledgeBaseList(new AiKnowledgeBase());
            for (AiKnowledgeBase kb : list)
            {
                if (kb == null || !"0".equals(kb.getStatus()))
                {
                    continue;
                }
                int count = chunkMapper.countByKbId(kb.getId());
                List<AiKnowledgeDoc> docs = docMapper.selectByKbId(kb.getId());
                if (count == 0 && docs != null && !docs.isEmpty())
                {
                    reindex(kb.getId());
                    rebuilt++;
                }
                else
                {
                    reloadKb(kb);
                    loaded++;
                }
            }
            log.info("PlatformKnowledgeRetriever warmed kbs={} reloaded={} rebuilt={}", list.size(), loaded, rebuilt);
        }
        catch (Exception e)
        {
            log.warn("Knowledge warm-up skipped: {}", e.getMessage());
        }
    }

    public synchronized int reindex(Long kbId)
    {
        AiKnowledgeBase kb = kbMapper.selectAiKnowledgeBaseById(kbId);
        if (kb == null)
        {
            return 0;
        }
        chunkMapper.deleteByKbId(kbId);
        List<AiKnowledgeDoc> docs = docMapper.selectByKbId(kbId);
        List<AiKnowledgeChunk> allChunks = new ArrayList<>();
        for (AiKnowledgeDoc doc : docs)
        {
            if (doc == null || StringUtils.isEmpty(doc.getContentText()))
            {
                continue;
            }
            List<String> parts = vectorStore.split(doc.getContentText());
            int idx = 0;
            for (String part : parts)
            {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk();
                chunk.setKbId(kbId);
                chunk.setDocId(doc.getId());
                chunk.setChunkIndex(idx++);
                chunk.setContent(part);
                allChunks.add(chunk);
            }
            doc.setStatus("1");
            doc.setChunkCount(parts.size());
            docMapper.update(doc);
        }
        if (!allChunks.isEmpty())
        {
            // batch insert in slices
            int batch = 100;
            for (int i = 0; i < allChunks.size(); i += batch)
            {
                chunkMapper.insertBatch(allChunks.subList(i, Math.min(i + batch, allChunks.size())));
            }
        }
        kbMapper.updateChunkCount(kbId, allChunks.size());
        reloadKb(kb);
        return allChunks.size();
    }

    public void reloadKb(AiKnowledgeBase kb)
    {
        if (kb == null || StringUtils.isEmpty(kb.getKbCode()))
        {
            return;
        }
        List<AiKnowledgeChunk> chunks = chunkMapper.selectByKbId(kb.getId());
        List<AiKnowledgeDoc> docs = docMapper.selectByKbId(kb.getId());
        java.util.Map<Long, String> docNames = new java.util.HashMap<>();
        for (AiKnowledgeDoc d : docs)
        {
            docNames.put(d.getId(), d.getDocName());
        }
        List<InMemoryKnowledgeVectorStore.ChunkItem> items = new ArrayList<>();
        for (AiKnowledgeChunk c : chunks)
        {
            items.add(new InMemoryKnowledgeVectorStore.ChunkItem(
                    c.getDocId(), docNames.getOrDefault(c.getDocId(), ""), c.getContent()));
        }
        vectorStore.replaceKb(kb.getKbCode(), items);
    }

    public void removeKb(String kbCode)
    {
        vectorStore.removeKb(kbCode);
    }

    @Override
    public String retrieveContext(List<String> knowledgeCodes, String query, int maxResults, double minScore)
    {
        return vectorStore.search(knowledgeCodes, query, maxResults, minScore);
    }

    @Override
    public List<com.ruoyiliteflow.aicore.spi.KnowledgeHit> searchHits(List<String> knowledgeCodes, String query,
            int maxResults, double minScore)
    {
        return vectorStore.searchHits(knowledgeCodes, query, maxResults, minScore);
    }

    public List<com.ruoyiliteflow.aicore.spi.KnowledgeHit> searchByCode(String kbCode, String query, int maxResults,
            double minScore)
    {
        if (StringUtils.isEmpty(kbCode))
        {
            return List.of();
        }
        return vectorStore.searchHits(List.of(kbCode), query, maxResults, minScore);
    }
}

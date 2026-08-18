package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeChunk;

public interface AiKnowledgeChunkMapper
{
    List<AiKnowledgeChunk> selectByKbId(Long kbId);

    int insertBatch(List<AiKnowledgeChunk> chunks);

    int deleteByKbId(Long kbId);

    int deleteByDocId(Long docId);

    int countByKbId(Long kbId);
}

package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeBase;

public interface AiKnowledgeBaseMapper
{
    List<AiKnowledgeBase> selectAiKnowledgeBaseList(AiKnowledgeBase query);

    AiKnowledgeBase selectAiKnowledgeBaseById(Long id);

    AiKnowledgeBase selectAiKnowledgeBaseByCode(String kbCode);

    int insertAiKnowledgeBase(AiKnowledgeBase kb);

    int updateAiKnowledgeBase(AiKnowledgeBase kb);

    int deleteAiKnowledgeBaseByIds(Long[] ids);

    int updateChunkCount(@Param("id") Long id, @Param("chunkCount") Integer chunkCount);
}

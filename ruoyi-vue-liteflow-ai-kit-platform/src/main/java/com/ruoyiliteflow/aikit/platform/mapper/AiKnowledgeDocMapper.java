package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeDoc;

public interface AiKnowledgeDocMapper
{
    List<AiKnowledgeDoc> selectByKbId(Long kbId);

    AiKnowledgeDoc selectById(Long id);

    int insert(AiKnowledgeDoc doc);

    int update(AiKnowledgeDoc doc);

    int deleteByIds(Long[] ids);

    int deleteByKbIds(Long[] kbIds);
}

package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiMemoryItem;

public interface IAiMemoryService
{
    List<AiMemoryItem> selectAiMemoryItemList(AiMemoryItem query);

    int insertAiMemoryItem(AiMemoryItem item);

    int deleteAiMemoryItemByIds(Long[] ids);
}

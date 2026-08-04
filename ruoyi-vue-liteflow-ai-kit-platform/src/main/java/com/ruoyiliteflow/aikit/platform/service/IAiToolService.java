package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;

public interface IAiToolService
{
    List<AiTool> selectAiToolList(AiTool query);

    AiTool selectAiToolById(Long id);

    int insertAiTool(AiTool tool);

    int updateAiTool(AiTool tool);

    int deleteAiToolByIds(Long[] ids);
}

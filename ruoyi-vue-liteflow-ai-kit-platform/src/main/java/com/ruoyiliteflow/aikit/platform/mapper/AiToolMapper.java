package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;

public interface AiToolMapper
{
    List<AiTool> selectAiToolList(AiTool query);

    AiTool selectAiToolById(Long id);

    AiTool selectAiToolByCode(String toolCode);

    List<AiTool> selectEnabledByCodes(@Param("codes") List<String> codes);

    List<AiTool> selectEnabledMcpTools();

    int insertAiTool(AiTool tool);

    int updateAiTool(AiTool tool);

    int deleteAiToolByIds(Long[] ids);
}

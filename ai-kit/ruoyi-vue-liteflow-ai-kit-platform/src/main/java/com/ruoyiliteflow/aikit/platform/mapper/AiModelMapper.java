package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.aikit.platform.domain.AiModel;

public interface AiModelMapper
{
    List<AiModel> selectAiModelList(AiModel query);

    AiModel selectAiModelById(Long id);

    AiModel selectAiModelByCode(String modelCode);

    AiModel selectDefaultEnabled();

    int insertAiModel(AiModel model);

    int updateAiModel(AiModel model);

    int clearDefaultFlag(@Param("excludeId") Long excludeId);

    int deleteAiModelByIds(Long[] ids);
}

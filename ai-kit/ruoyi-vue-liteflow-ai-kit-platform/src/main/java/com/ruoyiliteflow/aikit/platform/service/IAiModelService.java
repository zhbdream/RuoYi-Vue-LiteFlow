package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiModel;
import com.ruoyiliteflow.aicore.model.AiModelCredential;

public interface IAiModelService
{
    List<AiModel> selectAiModelList(AiModel query);

    AiModel selectAiModelById(Long id);

    AiModelCredential resolveRuntimeDefault();

    AiModelCredential resolveRuntimeById(Long id);

    int insertAiModel(AiModel model);

    int updateAiModel(AiModel model);

    int deleteAiModelByIds(Long[] ids);

    /** 连通测试：发送一条短对话，不回传 Key */
    String testConnectivity(AiModel model);

    java.util.Map<String, Object> describeSources();
}

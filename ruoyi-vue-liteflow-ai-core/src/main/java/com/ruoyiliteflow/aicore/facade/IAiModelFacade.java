package com.ruoyiliteflow.aicore.facade;

import java.util.List;
import com.ruoyiliteflow.aicore.model.AiModelInfo;

public interface IAiModelFacade
{
    List<AiModelInfo> listModels();

    AiModelInfo getDefaultModel();
}

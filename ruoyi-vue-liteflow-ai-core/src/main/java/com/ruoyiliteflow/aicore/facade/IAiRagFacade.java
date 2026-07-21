package com.ruoyiliteflow.aicore.facade;

import com.ruoyiliteflow.aicore.model.RagAskRequest;
import com.ruoyiliteflow.aicore.model.RagAskResult;

public interface IAiRagFacade
{
    RagAskResult ask(RagAskRequest request);
}

package com.ruoyiliteflow.aicore.facade;

import com.ruoyiliteflow.aicore.model.ChatCompletionRequest;
import com.ruoyiliteflow.aicore.model.ChatCompletionResult;

public interface IAiChatFacade
{
    ChatCompletionResult complete(ChatCompletionRequest request);
}

package com.ruoyiliteflow.aicore.facade.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.facade.IAiChatFacade;
import com.ruoyiliteflow.aicore.model.ChatCompletionRequest;
import com.ruoyiliteflow.aicore.model.ChatCompletionResult;
import com.ruoyiliteflow.aicore.spi.AiQuotaGuard;
import com.ruoyiliteflow.aicore.support.AiChatModelFactory;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.model.chat.ChatModel;

@Service
public class AiChatFacadeImpl implements IAiChatFacade
{
    @Autowired
    private AiChatModelFactory chatModelFactory;

    @Autowired(required = false)
    private AiQuotaGuard quotaGuard;

    @Value("${ruoyi.ai.quota.dimension.chat:agent:chat}")
    private String quotaDimension;

    @Override
    public ChatCompletionResult complete(ChatCompletionRequest request)
    {
        if (quotaGuard != null)
        {
            String principal = StringUtils.isEmpty(request.getPrincipal()) ? "anonymous" : request.getPrincipal();
            quotaGuard.assertWithinQuota(principal, quotaDimension);
        }

        double temperature = request.getTemperature() == null ? 0.3 : request.getTemperature();
        ChatModel model = chatModelFactory.createChatModel(temperature);

        String prompt;
        if (StringUtils.isNotEmpty(request.getSystemPrompt()))
        {
            prompt = request.getSystemPrompt() + "\n\n用户：" + request.getUserMessage();
        }
        else
        {
            prompt = request.getUserMessage();
        }

        String content = model.chat(prompt);
        String modelName = chatModelFactory.resolveCredential().getModelName();
        return new ChatCompletionResult(content, modelName);
    }
}

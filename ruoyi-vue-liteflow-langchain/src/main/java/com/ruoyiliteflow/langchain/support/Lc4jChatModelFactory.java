package com.ruoyiliteflow.langchain.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 从「模型配置」或 yml/环境变量组装 OpenAI 兼容 {@link ChatModel}（DeepSeek 等）
 */
@Component
public class Lc4jChatModelFactory
{
    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @Value("${liteflow.agent.openai-compatible.deepseek.api-key:}")
    private String ymlApiKey;

    @Value("${liteflow.agent.openai-compatible.deepseek.base-url:https://api.deepseek.com/v1}")
    private String ymlBaseUrl;

    @Value("${liteflow.agent.demo.model:deepseek-chat}")
    private String ymlModel;

    public ChatModel createChatModel()
    {
        Lc4jModelCredential cred = resolveCredential();
        return OpenAiChatModel.builder()
                .apiKey(cred.getApiKey())
                .baseUrl(cred.getBaseUrl())
                .modelName(cred.getModelName())
                .temperature(0.2)
                .build();
    }

    public Lc4jModelCredential resolveCredential()
    {
        LfAgentModel model = lfAgentModelService.resolveRuntimeDefault();
        if (model != null && StringUtils.isNotEmpty(model.getApiKey()))
        {
            String baseUrl = StringUtils.isNotEmpty(model.getBaseUrl())
                    ? model.getBaseUrl()
                    : ymlBaseUrl;
            String modelName = StringUtils.isNotEmpty(model.getModel())
                    ? model.getModel()
                    : ymlModel;
            return new Lc4jModelCredential(model.getApiKey(), baseUrl, modelName);
        }
        if (StringUtils.isEmpty(ymlApiKey))
        {
            throw new ServiceException(
                    "未配置 LLM API Key：请在「模型配置」页新增默认模型，或设置环境变量 DEEPSEEK_API_KEY");
        }
        return new Lc4jModelCredential(ymlApiKey, ymlBaseUrl, ymlModel);
    }
}
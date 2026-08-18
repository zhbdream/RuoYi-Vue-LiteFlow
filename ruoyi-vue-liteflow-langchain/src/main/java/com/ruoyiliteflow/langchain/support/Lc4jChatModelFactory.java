package com.ruoyiliteflow.langchain.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

/**
 * 从「AI能力 → 模型管理」或 yml/环境变量组装 OpenAI 兼容 {@link ChatModel}（DeepSeek 等）
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

    /** 内部助手流式对话 */
    public StreamingChatModel createStreamingChatModel()
    {
        return createStreamingChatModel(0.3);
    }

    public StreamingChatModel createStreamingChatModel(double temperature)
    {
        return createStreamingChatModel(temperature, resolveCredential());
    }

    public StreamingChatModel createStreamingChatModel(double temperature, Lc4jModelCredential cred)
    {
        if (cred == null)
        {
            throw new ServiceException("未配置 LLM API Key：请在「AI能力 → 模型管理」新增模型，或设置环境变量 DEEPSEEK_API_KEY");
        }
        return OpenAiStreamingChatModel.builder()
                .apiKey(cred.getApiKey())
                .baseUrl(cred.getBaseUrl())
                .modelName(cred.getModelName())
                .temperature(temperature)
                .build();
    }

    public Lc4jModelCredential resolveCredential()
    {
        return resolveCredential(null);
    }

    public Lc4jModelCredential resolveCredential(String modelCode)
    {
        if (StringUtils.isNotEmpty(modelCode))
        {
            LfAgentModel model = lfAgentModelService.resolveRuntimeByCode(modelCode);
            if (model == null || StringUtils.isEmpty(model.getApiKey()))
            {
                throw new ServiceException("模型不存在、已停用或未配置 Key：" + modelCode);
            }
            return toCredential(model);
        }
        LfAgentModel model = lfAgentModelService.resolveRuntimeDefault();
        if (model != null && StringUtils.isNotEmpty(model.getApiKey()))
        {
            return toCredential(model);
        }
        if (StringUtils.isEmpty(ymlApiKey))
        {
            throw new ServiceException(
                    "未配置 LLM API Key：请在「AI能力 → 模型管理」新增默认模型，或设置环境变量 DEEPSEEK_API_KEY");
        }
        return new Lc4jModelCredential(ymlApiKey, ymlBaseUrl, ymlModel);
    }

    private Lc4jModelCredential toCredential(LfAgentModel model)
    {
        String baseUrl = StringUtils.isNotEmpty(model.getBaseUrl()) ? model.getBaseUrl() : ymlBaseUrl;
        String modelName = StringUtils.isNotEmpty(model.getModel()) ? model.getModel() : ymlModel;
        return new Lc4jModelCredential(model.getApiKey(), baseUrl, modelName);
    }
}
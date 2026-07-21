package com.ruoyiliteflow.aicore.spi;

import java.util.ArrayList;
import java.util.List;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.model.AiModelInfo;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 默认凭证：从 yml / 环境变量读取（独立 MCP / Agent 进程可用）。
 */
public class YmlAiModelCredentialProvider implements AiModelCredentialProvider
{
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public YmlAiModelCredentialProvider(String apiKey, String baseUrl, String model)
    {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public AiModelCredential resolveCredential()
    {
        if (StringUtils.isEmpty(apiKey))
        {
            throw new ServiceException(
                    "未配置 LLM API Key：请设置 ruoyi.ai.openai.api-key 或环境变量 DEEPSEEK_API_KEY");
        }
        return new AiModelCredential(apiKey, baseUrl, model);
    }

    @Override
    public List<AiModelInfo> listModels()
    {
        List<AiModelInfo> list = new ArrayList<>();
        if (StringUtils.isEmpty(apiKey))
        {
            return list;
        }
        AiModelInfo info = new AiModelInfo();
        info.setModelCode("yml-default");
        info.setModelName("YML Default");
        info.setBaseUrl(baseUrl);
        info.setModel(model);
        info.setDefaultModel(true);
        info.setEnabled(true);
        list.add(info);
        return list;
    }
}

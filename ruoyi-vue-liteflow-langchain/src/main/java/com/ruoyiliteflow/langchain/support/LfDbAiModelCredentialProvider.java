package com.ruoyiliteflow.langchain.support;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.model.AiModelInfo;
import com.ruoyiliteflow.aicore.spi.AiModelCredentialProvider;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 管理端：优先使用「模型配置」表作为 ai-core 凭证来源。
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LfDbAiModelCredentialProvider implements AiModelCredentialProvider
{
    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @Value("${liteflow.agent.openai-compatible.deepseek.api-key:}")
    private String ymlApiKey;

    @Value("${liteflow.agent.openai-compatible.deepseek.base-url:https://api.deepseek.com/v1}")
    private String ymlBaseUrl;

    @Value("${liteflow.agent.demo.model:deepseek-chat}")
    private String ymlModel;

    @Override
    public AiModelCredential resolveCredential()
    {
        LfAgentModel model = lfAgentModelService.resolveRuntimeDefault();
        if (model != null && StringUtils.isNotEmpty(model.getApiKey()))
        {
            String baseUrl = StringUtils.isNotEmpty(model.getBaseUrl()) ? model.getBaseUrl() : ymlBaseUrl;
            String modelName = StringUtils.isNotEmpty(model.getModel()) ? model.getModel() : ymlModel;
            return new AiModelCredential(model.getApiKey(), baseUrl, modelName);
        }
        if (StringUtils.isEmpty(ymlApiKey))
        {
            throw new ServiceException(
                    "未配置 LLM API Key：请在「模型配置」页新增默认模型，或设置环境变量 DEEPSEEK_API_KEY");
        }
        return new AiModelCredential(ymlApiKey, ymlBaseUrl, ymlModel);
    }

    @Override
    public List<AiModelInfo> listModels()
    {
        List<AiModelInfo> result = new ArrayList<>();
        List<LfAgentModel> list = lfAgentModelService.selectLfAgentModelList(new LfAgentModel());
        for (LfAgentModel m : list)
        {
            AiModelInfo info = new AiModelInfo();
            info.setModelCode(m.getModelCode());
            info.setModelName(m.getModelName());
            info.setBaseUrl(m.getBaseUrl());
            info.setModel(m.getModel());
            info.setDefaultModel("1".equals(m.getIsDefault()));
            // status: 0=正常 1=停用
            info.setEnabled(m.getStatus() == null || !"1".equals(m.getStatus()));
            result.add(info);
        }
        return result;
    }
}

package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aikit.platform.domain.AiModel;
import com.ruoyiliteflow.aikit.platform.mapper.AiModelMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiModelService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.sign.AesEncryptUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Service
public class AiModelServiceImpl implements IAiModelService
{
    @Autowired
    private AiModelMapper aiModelMapper;

    @Value("${ruoyi.ai.crypto.secret:${liteflow.agent.crypto.secret:ruoyi-liteflow-aes}}")
    private String cryptoSecret;

    @Value("${ruoyi.ai.openai.api-key:}")
    private String ymlApiKey;

    @Value("${ruoyi.ai.openai.base-url:https://api.deepseek.com/v1}")
    private String defaultBaseUrl;

    @Value("${ruoyi.ai.openai.model:deepseek-chat}")
    private String ymlModel;

    @Override
    public List<AiModel> selectAiModelList(AiModel query)
    {
        List<AiModel> list = aiModelMapper.selectAiModelList(query);
        list.forEach(this::maskForApi);
        return list;
    }

    @Override
    public AiModel selectAiModelById(Long id)
    {
        AiModel model = aiModelMapper.selectAiModelById(id);
        if (model != null)
        {
            maskForApi(model);
        }
        return model;
    }

    @Override
    public AiModelCredential resolveRuntimeDefault()
    {
        return toCredential(aiModelMapper.selectDefaultEnabled());
    }

    @Override
    public AiModelCredential resolveRuntimeById(Long id)
    {
        if (id == null)
        {
            return null;
        }
        return toCredential(aiModelMapper.selectAiModelById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAiModel(AiModel model)
    {
        normalize(model);
        if (aiModelMapper.selectAiModelByCode(model.getModelCode()) != null)
        {
            throw new ServiceException("模型编码已存在: " + model.getModelCode());
        }
        if (StringUtils.isEmpty(model.getApiKey()))
        {
            throw new ServiceException("新增时必须填写 API Key");
        }
        model.setApiKeyEnc(AesEncryptUtils.encrypt(model.getApiKey().trim(), cryptoSecret));
        model.setApiKey(null);
        if ("1".equals(model.getIsDefault()))
        {
            aiModelMapper.clearDefaultFlag(null);
        }
        return aiModelMapper.insertAiModel(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAiModel(AiModel model)
    {
        if (model.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        normalize(model);
        AiModel db = aiModelMapper.selectAiModelById(model.getId());
        if (db == null)
        {
            throw new ServiceException("模型配置不存在");
        }
        if (StringUtils.isNotEmpty(model.getModelCode()) && !model.getModelCode().equals(db.getModelCode()))
        {
            AiModel exist = aiModelMapper.selectAiModelByCode(model.getModelCode());
            if (exist != null && !exist.getId().equals(model.getId()))
            {
                throw new ServiceException("模型编码已存在: " + model.getModelCode());
            }
        }
        if (StringUtils.isNotEmpty(model.getApiKey()))
        {
            model.setApiKeyEnc(AesEncryptUtils.encrypt(model.getApiKey().trim(), cryptoSecret));
        }
        else
        {
            model.setApiKeyEnc(null);
        }
        model.setApiKey(null);
        if ("1".equals(model.getIsDefault()))
        {
            aiModelMapper.clearDefaultFlag(model.getId());
        }
        return aiModelMapper.updateAiModel(model);
    }

    @Override
    public int deleteAiModelByIds(Long[] ids)
    {
        return aiModelMapper.deleteAiModelByIds(ids);
    }

    @Override
    public String testConnectivity(AiModel input)
    {
        AiModelCredential cred;
        if (input != null && input.getId() != null)
        {
            AiModel db = aiModelMapper.selectAiModelById(input.getId());
            if (db == null)
            {
                throw new ServiceException("模型不存在");
            }
            if (StringUtils.isNotEmpty(input.getApiKey()))
            {
                cred = new AiModelCredential(input.getApiKey().trim(),
                        firstNonEmpty(input.getBaseUrl(), db.getBaseUrl(), defaultBaseUrl),
                        firstNonEmpty(input.getModel(), db.getModel(), "deepseek-chat"));
            }
            else
            {
                cred = toCredential(db);
            }
        }
        else if (input != null && StringUtils.isNotEmpty(input.getApiKey()))
        {
            cred = new AiModelCredential(input.getApiKey().trim(),
                    firstNonEmpty(input.getBaseUrl(), defaultBaseUrl),
                    firstNonEmpty(input.getModel(), "deepseek-chat"));
        }
        else
        {
            cred = resolveRuntimeDefault();
            if (cred == null && StringUtils.isNotEmpty(ymlApiKey))
            {
                cred = new AiModelCredential(ymlApiKey, defaultBaseUrl, ymlModel);
            }
        }
        if (cred == null || StringUtils.isEmpty(cred.getApiKey()))
        {
            throw new ServiceException("无可用 API Key，无法测试连通");
        }
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(cred.getApiKey())
                .baseUrl(cred.getBaseUrl())
                .modelName(cred.getModelName())
                .temperature(0.1)
                .build();
        String reply = model.chat("请只回复：ok");
        return StringUtils.isEmpty(reply) ? "(empty)" : reply.trim();
    }

    private AiModelCredential toCredential(AiModel model)
    {
        if (model == null || !"0".equals(model.getStatus()))
        {
            return null;
        }
        if (StringUtils.isEmpty(model.getApiKeyEnc()))
        {
            return null;
        }
        String key = AesEncryptUtils.decrypt(model.getApiKeyEnc(), cryptoSecret);
        String baseUrl = StringUtils.isNotEmpty(model.getBaseUrl()) ? model.getBaseUrl() : defaultBaseUrl;
        return new AiModelCredential(key, baseUrl, model.getModel());
    }

    private void normalize(AiModel model)
    {
        if (StringUtils.isEmpty(model.getProvider()))
        {
            model.setProvider("deepseek");
        }
        if (StringUtils.isEmpty(model.getModel()))
        {
            model.setModel("deepseek-chat");
        }
        if (StringUtils.isEmpty(model.getStatus()))
        {
            model.setStatus("0");
        }
        if (StringUtils.isEmpty(model.getIsDefault()))
        {
            model.setIsDefault("0");
        }
        if (StringUtils.isEmpty(model.getBaseUrl()))
        {
            model.setBaseUrl(defaultBaseUrl);
        }
    }

    private void maskForApi(AiModel model)
    {
        boolean has = StringUtils.isNotEmpty(model.getApiKeyEnc());
        model.setHasApiKey(has);
        model.setApiKeyMasked(has ? "******（已配置）" : "（未配置）");
        model.setApiKeyEnc(null);
        model.setApiKey(null);
    }

    private static String firstNonEmpty(String... values)
    {
        if (values == null)
        {
            return null;
        }
        for (String v : values)
        {
            if (StringUtils.isNotEmpty(v))
            {
                return v;
            }
        }
        return null;
    }
}

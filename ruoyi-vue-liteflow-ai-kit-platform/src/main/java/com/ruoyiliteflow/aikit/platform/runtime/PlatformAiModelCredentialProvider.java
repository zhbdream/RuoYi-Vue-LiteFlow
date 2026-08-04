package com.ruoyiliteflow.aikit.platform.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.model.AiModelInfo;
import com.ruoyiliteflow.aicore.spi.AiModelCredentialProvider;
import com.ruoyiliteflow.aikit.platform.domain.AiModel;
import com.ruoyiliteflow.aikit.platform.mapper.AiModelMapper;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.sign.AesEncryptUtils;

/**
 * 优先 ai_model，其次 lf_agent_model，最后 yml。
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformAiModelCredentialProvider implements AiModelCredentialProvider
{
    @Autowired
    private AiModelMapper aiModelMapper;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Value("${ruoyi.ai.openai.api-key:${liteflow.agent.openai-compatible.deepseek.api-key:}}")
    private String ymlApiKey;

    @Value("${ruoyi.ai.openai.base-url:${liteflow.agent.openai-compatible.deepseek.base-url:https://api.deepseek.com/v1}}")
    private String ymlBaseUrl;

    @Value("${ruoyi.ai.openai.model:${liteflow.agent.demo.model:deepseek-chat}}")
    private String ymlModel;

    @Value("${ruoyi.ai.crypto.secret:${liteflow.agent.crypto.secret:ruoyi-liteflow-aes}}")
    private String cryptoSecret;

    @Override
    public AiModelCredential resolveCredential()
    {
        AiModelCredential fromAi = toCredential(aiModelMapper.selectDefaultEnabled());
        if (fromAi != null && StringUtils.isNotEmpty(fromAi.getApiKey()))
        {
            return fromAi;
        }
        AiModelCredential fromLf = resolveFromLfAgentModel();
        if (fromLf != null && StringUtils.isNotEmpty(fromLf.getApiKey()))
        {
            return fromLf;
        }
        if (StringUtils.isEmpty(ymlApiKey))
        {
            throw new IllegalStateException("未配置模型 API Key：请写入 ai_model 或设置 DEEPSEEK_API_KEY / ruoyi.ai.openai.api-key");
        }
        return new AiModelCredential(ymlApiKey, ymlBaseUrl, ymlModel);
    }

    @Override
    public List<AiModelInfo> listModels()
    {
        List<AiModel> list = aiModelMapper.selectAiModelList(new AiModel());
        List<AiModelInfo> result = new ArrayList<>();
        for (AiModel m : list)
        {
            AiModelInfo info = new AiModelInfo();
            info.setModelCode(m.getModelCode());
            info.setModelName(m.getModelName());
            info.setBaseUrl(m.getBaseUrl());
            info.setModel(m.getModel());
            info.setDefaultModel("1".equals(m.getIsDefault()));
            info.setEnabled("0".equals(m.getStatus()));
            result.add(info);
        }
        return result;
    }

    private AiModelCredential toCredential(AiModel model)
    {
        if (model == null || !"0".equals(model.getStatus()) || StringUtils.isEmpty(model.getApiKeyEnc()))
        {
            return null;
        }
        String key = AesEncryptUtils.decrypt(model.getApiKeyEnc(), cryptoSecret);
        String baseUrl = StringUtils.isNotEmpty(model.getBaseUrl()) ? model.getBaseUrl() : ymlBaseUrl;
        return new AiModelCredential(key, baseUrl, model.getModel());
    }

    private AiModelCredential resolveFromLfAgentModel()
    {
        if (jdbcTemplate == null)
        {
            return null;
        }
        try
        {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select base_url, model, api_key_enc from lf_agent_model "
                            + "where status = '0' and is_default = '1' order by id desc limit 1");
            if (rows.isEmpty())
            {
                return null;
            }
            Map<String, Object> row = rows.get(0);
            String enc = row.get("api_key_enc") == null ? null : String.valueOf(row.get("api_key_enc"));
            if (StringUtils.isEmpty(enc))
            {
                return null;
            }
            String key = AesEncryptUtils.decrypt(enc, cryptoSecret);
            String baseUrl = row.get("base_url") == null ? ymlBaseUrl : String.valueOf(row.get("base_url"));
            String model = row.get("model") == null ? ymlModel : String.valueOf(row.get("model"));
            return new AiModelCredential(key, baseUrl, model);
        }
        catch (DataAccessException ex)
        {
            return null;
        }
    }
}

package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aikit.platform.domain.AiModel;
import com.ruoyiliteflow.aikit.platform.knowledge.KitEmbeddingModelFactory;
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
    private static final Logger log = LoggerFactory.getLogger(AiModelServiceImpl.class);

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

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private KitEmbeddingModelFactory embeddingModelFactory;

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
        int rows = aiModelMapper.insertAiModel(model);
        AiModel persisted = model.getId() == null ? model : aiModelMapper.selectAiModelById(model.getId());
        syncToLfAgentModel(persisted != null ? persisted : model);
        return rows;
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
        int rows = aiModelMapper.updateAiModel(model);
        syncToLfAgentModel(aiModelMapper.selectAiModelById(model.getId()));
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiModelByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        List<String> codes = new ArrayList<>();
        for (Long id : ids)
        {
            AiModel existing = aiModelMapper.selectAiModelById(id);
            if (existing != null && StringUtils.isNotEmpty(existing.getModelCode()))
            {
                codes.add(existing.getModelCode());
            }
        }
        int rows = aiModelMapper.deleteAiModelByIds(ids);
        deleteLfByCodes(codes);
        return rows;
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

    @Override
    public Map<String, Object> describeSources()
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("order", List.of("ai_model", "lf_agent_model", "yml"));
        out.put("note", "本页为唯一入口。保存后同步 lf_agent_model（LiteFlow 链路 / AI助手 / 配额）。解析顺序：ai_model 默认 → lf_agent_model 默认 → yml/环境变量。");
        AiModel def = aiModelMapper.selectDefaultEnabled();
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("configured", def != null && StringUtils.isNotEmpty(def.getApiKeyEnc()));
        ai.put("modelCode", def == null ? "" : nvl(def.getModelCode()));
        ai.put("model", def == null ? "" : nvl(def.getModel()));
        out.put("aiModel", ai);
        out.put("lfAgentModel", describeLf());
        Map<String, Object> yml = new LinkedHashMap<>();
        yml.put("configured", StringUtils.isNotEmpty(ymlApiKey));
        yml.put("model", ymlModel);
        out.put("yml", yml);
        if (embeddingModelFactory != null)
        {
            out.put("embedding", embeddingModelFactory.source());
        }
        return out;
    }

    private Map<String, Object> describeLf()
    {
        Map<String, Object> lf = new LinkedHashMap<>();
        lf.put("configured", false);
        lf.put("model", "");
        if (jdbcTemplate == null)
        {
            return lf;
        }
        try
        {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select model, api_key_enc from lf_agent_model "
                            + "where status = '0' and is_default = '1' order by id desc limit 1");
            if (!rows.isEmpty())
            {
                Map<String, Object> row = rows.get(0);
                String enc = row.get("api_key_enc") == null ? "" : String.valueOf(row.get("api_key_enc"));
                lf.put("configured", StringUtils.isNotEmpty(enc));
                lf.put("model", row.get("model") == null ? "" : String.valueOf(row.get("model")));
            }
        }
        catch (DataAccessException ignored)
        {
            // 表不存在时忽略
        }
        return lf;
    }

    /** 影子同步：助手 / 配额 / LangChain 仍读 lf_agent_model，避免 Kit 依赖 LiteFlow 模块。 */
    private void syncToLfAgentModel(AiModel model)
    {
        if (jdbcTemplate == null || model == null || StringUtils.isEmpty(model.getModelCode()))
        {
            return;
        }
        String configKey = "deepseek";
        try
        {
            Integer n = jdbcTemplate.queryForObject(
                    "select count(1) from lf_agent_model where model_code = ?",
                    Integer.class, model.getModelCode());
            if (n != null && n > 0)
            {
                jdbcTemplate.update(
                        "update lf_agent_model set model_name=?, provider=?, config_key=?, base_url=?, model=?, "
                                + "api_key_enc=?, status=?, is_default=?, daily_call_limit=?, daily_token_limit=?, "
                                + "update_time=sysdate() where model_code=?",
                        model.getModelName(), model.getProvider(), configKey, model.getBaseUrl(), model.getModel(),
                        model.getApiKeyEnc(), model.getStatus(), model.getIsDefault(),
                        model.getDailyCallLimit(), model.getDailyTokenLimit(), model.getModelCode());
            }
            else
            {
                jdbcTemplate.update(
                        "insert into lf_agent_model (model_code, model_name, provider, config_key, base_url, model, "
                                + "api_key_enc, status, is_default, daily_call_limit, daily_token_limit, "
                                + "create_by, create_time, remark) values (?,?,?,?,?,?,?,?,?,?,?,?,sysdate(),?)",
                        model.getModelCode(), model.getModelName(), model.getProvider(), configKey,
                        model.getBaseUrl(), model.getModel(), model.getApiKeyEnc(),
                        model.getStatus(), model.getIsDefault(), model.getDailyCallLimit(), model.getDailyTokenLimit(),
                        nvl(model.getCreateBy()), "synced from ai_model");
            }
            if ("1".equals(model.getIsDefault()))
            {
                jdbcTemplate.update("update lf_agent_model set is_default='0' where model_code <> ?",
                        model.getModelCode());
            }
        }
        catch (DataAccessException ex)
        {
            log.warn("sync lf_agent_model skipped: {}", ex.getMessage());
        }
    }

    private void deleteLfByCodes(List<String> codes)
    {
        if (jdbcTemplate == null || codes == null || codes.isEmpty())
        {
            return;
        }
        try
        {
            for (String code : codes)
            {
                jdbcTemplate.update("delete from lf_agent_model where model_code = ?", code);
            }
        }
        catch (DataAccessException ex)
        {
            log.warn("delete lf_agent_model skipped: {}", ex.getMessage());
        }
    }

    private static String nvl(String s)
    {
        return s == null ? "" : s;
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

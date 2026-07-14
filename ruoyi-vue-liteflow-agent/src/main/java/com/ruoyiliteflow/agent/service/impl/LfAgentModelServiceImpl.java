package com.ruoyiliteflow.agent.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.mapper.LfAgentModelMapper;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.sign.AesEncryptUtils;

@Service
public class LfAgentModelServiceImpl implements ILfAgentModelService
{
    @Autowired
    private LfAgentModelMapper lfAgentModelMapper;

    @Value("${liteflow.agent.crypto.secret:ruoyi-liteflow-aes}")
    private String cryptoSecret;

    @Override
    public List<LfAgentModel> selectLfAgentModelList(LfAgentModel query)
    {
        List<LfAgentModel> list = lfAgentModelMapper.selectLfAgentModelList(query);
        for (LfAgentModel item : list)
        {
            maskForApi(item);
        }
        return list;
    }

    @Override
    public LfAgentModel selectLfAgentModelById(Long id)
    {
        LfAgentModel model = lfAgentModelMapper.selectLfAgentModelById(id);
        if (model != null)
        {
            maskForApi(model);
        }
        return model;
    }

    @Override
    public LfAgentModel resolveRuntimeDefault()
    {
        LfAgentModel model = lfAgentModelMapper.selectDefaultEnabled();
        if (model == null)
        {
            return null;
        }
        if (StringUtils.isNotEmpty(model.getApiKeyEnc()))
        {
            model.setApiKey(AesEncryptUtils.decrypt(model.getApiKeyEnc(), cryptoSecret));
        }
        return model;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertLfAgentModel(LfAgentModel model)
    {
        normalize(model);
        if (lfAgentModelMapper.selectLfAgentModelByCode(model.getModelCode()) != null)
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
            lfAgentModelMapper.clearDefaultFlag(null);
        }
        return lfAgentModelMapper.insertLfAgentModel(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateLfAgentModel(LfAgentModel model)
    {
        if (model.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        normalize(model);
        LfAgentModel db = lfAgentModelMapper.selectLfAgentModelById(model.getId());
        if (db == null)
        {
            throw new ServiceException("模型配置不存在");
        }
        if (StringUtils.isNotEmpty(model.getModelCode()) && !model.getModelCode().equals(db.getModelCode()))
        {
            LfAgentModel exist = lfAgentModelMapper.selectLfAgentModelByCode(model.getModelCode());
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
            model.setApiKeyEnc(null); // 不更新密钥列（mapper if test）
        }
        model.setApiKey(null);
        if ("1".equals(model.getIsDefault()))
        {
            lfAgentModelMapper.clearDefaultFlag(model.getId());
        }
        return lfAgentModelMapper.updateLfAgentModel(model);
    }

    @Override
    public int deleteLfAgentModelByIds(Long[] ids)
    {
        return lfAgentModelMapper.deleteLfAgentModelByIds(ids);
    }

    private void normalize(LfAgentModel model)
    {
        if (StringUtils.isEmpty(model.getProvider()))
        {
            model.setProvider("deepseek");
        }
        if (StringUtils.isEmpty(model.getConfigKey()))
        {
            model.setConfigKey("deepseek");
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
    }

    private void maskForApi(LfAgentModel model)
    {
        boolean has = StringUtils.isNotEmpty(model.getApiKeyEnc());
        model.setHasApiKey(has);
        model.setApiKeyMasked(has ? "******（已配置）" : "（未配置）");
        model.setApiKeyEnc(null);
        model.setApiKey(null);
    }
}

package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.aikit.platform.domain.AiContextPolicy;
import com.ruoyiliteflow.aikit.platform.mapper.AiContextPolicyMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiContextPolicyService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

@Service
public class AiContextPolicyServiceImpl implements IAiContextPolicyService
{
    @Autowired
    private AiContextPolicyMapper aiContextPolicyMapper;

    @Override
    public List<AiContextPolicy> selectAiContextPolicyList(AiContextPolicy query)
    {
        return aiContextPolicyMapper.selectAiContextPolicyList(query);
    }

    @Override
    public AiContextPolicy selectAiContextPolicyById(Long id)
    {
        return aiContextPolicyMapper.selectAiContextPolicyById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAiContextPolicy(AiContextPolicy policy)
    {
        normalize(policy);
        if (aiContextPolicyMapper.selectAiContextPolicyByCode(policy.getPolicyCode()) != null)
        {
            throw new ServiceException("策略编码已存在: " + policy.getPolicyCode());
        }
        if ("1".equals(policy.getIsDefault()))
        {
            aiContextPolicyMapper.clearDefaultFlag(null);
        }
        return aiContextPolicyMapper.insertAiContextPolicy(policy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAiContextPolicy(AiContextPolicy policy)
    {
        if (policy.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        normalize(policy);
        AiContextPolicy db = aiContextPolicyMapper.selectAiContextPolicyById(policy.getId());
        if (db == null)
        {
            throw new ServiceException("上下文策略不存在");
        }
        if (StringUtils.isNotEmpty(policy.getPolicyCode()) && !policy.getPolicyCode().equals(db.getPolicyCode()))
        {
            AiContextPolicy exist = aiContextPolicyMapper.selectAiContextPolicyByCode(policy.getPolicyCode());
            if (exist != null && !exist.getId().equals(policy.getId()))
            {
                throw new ServiceException("策略编码已存在: " + policy.getPolicyCode());
            }
        }
        if ("1".equals(policy.getIsDefault()))
        {
            aiContextPolicyMapper.clearDefaultFlag(policy.getId());
        }
        return aiContextPolicyMapper.updateAiContextPolicy(policy);
    }

    @Override
    public int deleteAiContextPolicyByIds(Long[] ids)
    {
        return aiContextPolicyMapper.deleteAiContextPolicyByIds(ids);
    }

    private void normalize(AiContextPolicy policy)
    {
        if (policy.getWindowSize() == null || policy.getWindowSize() <= 0)
        {
            policy.setWindowSize(10);
        }
        if (StringUtils.isEmpty(policy.getEnableSummary()))
        {
            policy.setEnableSummary("0");
        }
        if (StringUtils.isEmpty(policy.getIsDefault()))
        {
            policy.setIsDefault("0");
        }
        if (StringUtils.isEmpty(policy.getEnabled()))
        {
            policy.setEnabled("1");
        }
        if (policy.getTokenBudget() == null || policy.getTokenBudget() < 0)
        {
            policy.setTokenBudget(0);
        }
    }
}

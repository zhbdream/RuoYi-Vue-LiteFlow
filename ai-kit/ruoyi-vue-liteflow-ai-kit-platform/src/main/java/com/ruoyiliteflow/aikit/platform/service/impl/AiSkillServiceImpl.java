package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.skill.SkillRenderer;
import com.ruoyiliteflow.aikit.platform.domain.AiSkill;
import com.ruoyiliteflow.aikit.platform.mapper.AiSkillMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiSkillService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

@Service
public class AiSkillServiceImpl implements IAiSkillService
{
    @Autowired
    private AiSkillMapper aiSkillMapper;

    @Override
    public List<AiSkill> selectAiSkillList(AiSkill query)
    {
        return aiSkillMapper.selectAiSkillList(query);
    }

    @Override
    public AiSkill selectAiSkillById(Long id)
    {
        return aiSkillMapper.selectAiSkillById(id);
    }

    @Override
    public int insertAiSkill(AiSkill skill)
    {
        normalize(skill);
        if (aiSkillMapper.selectAiSkillByCode(skill.getSkillCode()) != null)
        {
            throw new ServiceException("技能编码已存在: " + skill.getSkillCode());
        }
        return aiSkillMapper.insertAiSkill(skill);
    }

    @Override
    public int updateAiSkill(AiSkill skill)
    {
        if (skill.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        normalize(skill);
        AiSkill db = aiSkillMapper.selectAiSkillById(skill.getId());
        if (db == null)
        {
            throw new ServiceException("技能不存在");
        }
        if (StringUtils.isNotEmpty(skill.getSkillCode()) && !skill.getSkillCode().equals(db.getSkillCode()))
        {
            AiSkill exist = aiSkillMapper.selectAiSkillByCode(skill.getSkillCode());
            if (exist != null && !exist.getId().equals(skill.getId()))
            {
                throw new ServiceException("技能编码已存在: " + skill.getSkillCode());
            }
        }
        return aiSkillMapper.updateAiSkill(skill);
    }

    @Override
    public int deleteAiSkillByIds(Long[] ids)
    {
        return aiSkillMapper.deleteAiSkillByIds(ids);
    }

    @Override
    public Map<String, Object> tryRun(String skillCode, AgentRunRequest request)
    {
        AiSkill skill = aiSkillMapper.selectAiSkillByCode(skillCode);
        if (skill == null)
        {
            throw new ServiceException("技能不存在: " + skillCode);
        }
        if (!"1".equals(skill.getEnabled()))
        {
            throw new ServiceException("技能已停用: " + skillCode);
        }
        AgentRunRequest req = request == null ? new AgentRunRequest() : request;
        Map<String, String> vars = SkillRenderer.vars(req.getPrincipal(), skillCode, req.getSessionId(),
                req.getMessage(), req.getVariables());
        String rendered = SkillRenderer.render(skill.getContent(), vars);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("skillCode", skill.getSkillCode());
        out.put("skillType", skill.getSkillType());
        out.put("rendered", rendered);
        if ("http".equalsIgnoreCase(skill.getSkillType()))
        {
            SkillRenderer.HttpCall call = SkillRenderer.parseHttp(rendered);
            out.put("method", call.method);
            out.put("url", call.url);
            long t0 = System.currentTimeMillis();
            String result = SkillRenderer.invokeHttp(rendered);
            out.put("result", result == null ? "" : result);
            out.put("costMs", System.currentTimeMillis() - t0);
        }
        else
        {
            out.put("result", rendered);
        }
        return out;
    }

    private void normalize(AiSkill skill)
    {
        if (StringUtils.isEmpty(skill.getSkillType()))
        {
            skill.setSkillType("prompt");
        }
        if (StringUtils.isEmpty(skill.getEnabled()))
        {
            skill.setEnabled("1");
        }
    }
}

package com.ruoyiliteflow.aikit.platform.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.runtime.AgentRunResult;
import com.ruoyiliteflow.aicore.runtime.AgentRuntime;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;
import com.ruoyiliteflow.aikit.platform.mapper.AiAgentMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiAgentService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

@Service
public class AiAgentServiceImpl implements IAiAgentService
{
    @Autowired
    private AiAgentMapper aiAgentMapper;

    @Autowired
    private AgentRuntime agentRuntime;

    @Override
    public List<AiAgent> selectAiAgentList(AiAgent query)
    {
        List<AiAgent> list = aiAgentMapper.selectAiAgentList(query);
        for (AiAgent agent : list)
        {
            fillBindings(agent);
        }
        return list;
    }

    @Override
    public AiAgent selectAiAgentById(Long id)
    {
        AiAgent agent = aiAgentMapper.selectAiAgentById(id);
        fillBindings(agent);
        return agent;
    }

    @Override
    public AiAgent selectAiAgentByCode(String agentCode)
    {
        AiAgent agent = aiAgentMapper.selectAiAgentByCode(agentCode);
        fillBindings(agent);
        return agent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAiAgent(AiAgent agent)
    {
        normalize(agent);
        if (aiAgentMapper.selectAiAgentByCode(agent.getAgentCode()) != null)
        {
            throw new ServiceException("智能体编码已存在: " + agent.getAgentCode());
        }
        int rows = aiAgentMapper.insertAiAgent(agent);
        replaceTools(agent.getId(), agent.getToolIds());
        replaceKnowledge(agent.getId(), agent.getKbIds());
        replaceSkills(agent.getId(), agent.getSkillIds());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAiAgent(AiAgent agent)
    {
        if (agent.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        normalize(agent);
        AiAgent db = aiAgentMapper.selectAiAgentById(agent.getId());
        if (db == null)
        {
            throw new ServiceException("智能体不存在");
        }
        if (StringUtils.isNotEmpty(agent.getAgentCode()) && !agent.getAgentCode().equals(db.getAgentCode()))
        {
            AiAgent exist = aiAgentMapper.selectAiAgentByCode(agent.getAgentCode());
            if (exist != null && !exist.getId().equals(agent.getId()))
            {
                throw new ServiceException("智能体编码已存在: " + agent.getAgentCode());
            }
        }
        int rows = aiAgentMapper.updateAiAgent(agent);
        if (agent.getToolIds() != null)
        {
            replaceTools(agent.getId(), agent.getToolIds());
        }
        if (agent.getKbIds() != null)
        {
            replaceKnowledge(agent.getId(), agent.getKbIds());
        }
        if (agent.getSkillIds() != null)
        {
            replaceSkills(agent.getId(), agent.getSkillIds());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAiAgentByIds(Long[] ids)
    {
        if (ids != null)
        {
            for (Long id : ids)
            {
                aiAgentMapper.deleteAgentTools(id);
                aiAgentMapper.deleteAgentKnowledge(id);
                aiAgentMapper.deleteAgentSkills(id);
            }
        }
        return aiAgentMapper.deleteAiAgentByIds(ids);
    }

    @Override
    public AgentRunResult run(String agentCode, AgentRunRequest request)
    {
        return agentRuntime.invoke(agentCode, request);
    }

    private void fillBindings(AiAgent agent)
    {
        if (agent == null || agent.getId() == null)
        {
            return;
        }
        agent.setToolIds(aiAgentMapper.selectToolIdsByAgentId(agent.getId()));
        agent.setToolCodes(aiAgentMapper.selectToolCodesByAgentId(agent.getId()));
        agent.setKbIds(aiAgentMapper.selectKbIdsByAgentId(agent.getId()));
        agent.setKnowledgeCodes(aiAgentMapper.selectKnowledgeCodesByAgentId(agent.getId()));
        agent.setSkillIds(aiAgentMapper.selectSkillIdsByAgentId(agent.getId()));
        agent.setSkillCodes(aiAgentMapper.selectSkillCodesByAgentId(agent.getId()));
    }

    private void replaceTools(Long agentId, List<Long> toolIds)
    {
        if (agentId == null)
        {
            return;
        }
        aiAgentMapper.deleteAgentTools(agentId);
        if (toolIds == null || toolIds.isEmpty())
        {
            return;
        }
        int sort = 0;
        for (Long toolId : toolIds)
        {
            if (toolId != null)
            {
                aiAgentMapper.insertAgentTool(agentId, toolId, sort++);
            }
        }
    }

    private void replaceKnowledge(Long agentId, List<Long> kbIds)
    {
        if (agentId == null)
        {
            return;
        }
        aiAgentMapper.deleteAgentKnowledge(agentId);
        if (kbIds == null || kbIds.isEmpty())
        {
            return;
        }
        int sort = 0;
        for (Long kbId : kbIds)
        {
            if (kbId != null)
            {
                aiAgentMapper.insertAgentKnowledge(agentId, kbId, sort++);
            }
        }
    }

    private void replaceSkills(Long agentId, List<Long> skillIds)
    {
        if (agentId == null)
        {
            return;
        }
        aiAgentMapper.deleteAgentSkills(agentId);
        if (skillIds == null || skillIds.isEmpty())
        {
            return;
        }
        int sort = 0;
        for (Long skillId : skillIds)
        {
            if (skillId != null)
            {
                aiAgentMapper.insertAgentSkill(agentId, skillId, sort++);
            }
        }
    }

    private void normalize(AiAgent agent)
    {
        if (StringUtils.isEmpty(agent.getEnabled()))
        {
            agent.setEnabled("1");
        }
        if (agent.getTemperature() == null)
        {
            agent.setTemperature(new BigDecimal("0.30"));
        }
    }
}

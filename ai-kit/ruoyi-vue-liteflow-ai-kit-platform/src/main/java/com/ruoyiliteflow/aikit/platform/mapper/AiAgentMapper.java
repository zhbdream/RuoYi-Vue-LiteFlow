package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;

public interface AiAgentMapper
{
    List<AiAgent> selectAiAgentList(AiAgent query);

    AiAgent selectAiAgentById(Long id);

    AiAgent selectAiAgentByCode(String agentCode);

    int insertAiAgent(AiAgent agent);

    int updateAiAgent(AiAgent agent);

    int deleteAiAgentByIds(Long[] ids);

    List<Long> selectToolIdsByAgentId(Long agentId);

    List<String> selectToolCodesByAgentId(Long agentId);

    int deleteAgentTools(Long agentId);

    int insertAgentTool(@Param("agentId") Long agentId, @Param("toolId") Long toolId, @Param("sort") int sort);

    List<Long> selectKbIdsByAgentId(Long agentId);

    List<String> selectKnowledgeCodesByAgentId(Long agentId);

    int deleteAgentKnowledge(Long agentId);

    int insertAgentKnowledge(@Param("agentId") Long agentId, @Param("kbId") Long kbId, @Param("sort") int sort);

    List<Long> selectSkillIdsByAgentId(Long agentId);

    List<String> selectSkillCodesByAgentId(Long agentId);

    int deleteAgentSkills(Long agentId);

    int insertAgentSkill(@Param("agentId") Long agentId, @Param("skillId") Long skillId, @Param("sort") int sort);
}

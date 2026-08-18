package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiSkill;

public interface IAiSkillService
{
    List<AiSkill> selectAiSkillList(AiSkill query);

    AiSkill selectAiSkillById(Long id);

    int insertAiSkill(AiSkill skill);

    int updateAiSkill(AiSkill skill);

    int deleteAiSkillByIds(Long[] ids);

    java.util.Map<String, Object> tryRun(String skillCode, com.ruoyiliteflow.aicore.runtime.AgentRunRequest request);
}

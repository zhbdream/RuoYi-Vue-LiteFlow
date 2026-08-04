package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.aikit.platform.domain.AiSkill;

public interface AiSkillMapper
{
    List<AiSkill> selectAiSkillList(AiSkill query);

    AiSkill selectAiSkillById(Long id);

    AiSkill selectAiSkillByCode(String skillCode);

    List<AiSkill> selectEnabledByCodes(@Param("codes") List<String> codes);

    int insertAiSkill(AiSkill skill);

    int updateAiSkill(AiSkill skill);

    int deleteAiSkillByIds(Long[] ids);
}

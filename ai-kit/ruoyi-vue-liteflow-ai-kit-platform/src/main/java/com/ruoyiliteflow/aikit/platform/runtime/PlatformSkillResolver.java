package com.ruoyiliteflow.aikit.platform.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.spi.SkillResolver;
import com.ruoyiliteflow.aicore.spi.SkillSpec;
import com.ruoyiliteflow.aikit.platform.domain.AiSkill;
import com.ruoyiliteflow.aikit.platform.mapper.AiSkillMapper;
import com.ruoyiliteflow.common.utils.StringUtils;

@Primary
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformSkillResolver implements SkillResolver
{
    @Autowired
    private AiSkillMapper aiSkillMapper;

    @Override
    public List<SkillSpec> resolve(List<String> skillCodes)
    {
        if (skillCodes == null || skillCodes.isEmpty())
        {
            return Collections.emptyList();
        }
        List<AiSkill> skills = aiSkillMapper.selectEnabledByCodes(skillCodes);
        if (skills == null || skills.isEmpty())
        {
            return Collections.emptyList();
        }
        List<SkillSpec> specs = new ArrayList<>(skills.size());
        for (AiSkill skill : skills)
        {
            if (skill == null || StringUtils.isEmpty(skill.getContent()))
            {
                continue;
            }
            SkillSpec spec = new SkillSpec();
            spec.setSkillCode(skill.getSkillCode());
            spec.setSkillName(skill.getSkillName());
            spec.setSkillType(skill.getSkillType());
            spec.setContent(skill.getContent());
            specs.add(spec);
        }
        return specs;
    }
}

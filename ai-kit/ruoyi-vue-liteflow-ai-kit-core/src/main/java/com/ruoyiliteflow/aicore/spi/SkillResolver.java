package com.ruoyiliteflow.aicore.spi;

import java.util.Collections;
import java.util.List;

public interface SkillResolver
{
    default List<SkillSpec> resolve(List<String> skillCodes)
    {
        return Collections.emptyList();
    }
}

package com.ruoyiliteflow.aicore.spi;

import java.util.Collections;
import java.util.List;

/** 技能规格（prompt 拼入 / http 预取） */
public class SkillSpec
{
    private String skillCode;
    private String skillName;
    private String skillType;
    private String content;

    public String getSkillCode()
    {
        return skillCode;
    }

    public void setSkillCode(String skillCode)
    {
        this.skillCode = skillCode;
    }

    public String getSkillName()
    {
        return skillName;
    }

    public void setSkillName(String skillName)
    {
        this.skillName = skillName;
    }

    public String getSkillType()
    {
        return skillType;
    }

    public void setSkillType(String skillType)
    {
        this.skillType = skillType;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }
}

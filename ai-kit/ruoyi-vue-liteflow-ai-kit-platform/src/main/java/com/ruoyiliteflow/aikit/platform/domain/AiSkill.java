package com.ruoyiliteflow.aikit.platform.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiSkill extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String skillCode;
    private String skillName;
    private String skillType;
    private String content;
    private String description;
    private String enabled;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "技能编码不能为空")
    @Size(max = 64)
    public String getSkillCode()
    {
        return skillCode;
    }

    public void setSkillCode(String skillCode)
    {
        this.skillCode = skillCode;
    }

    @Size(max = 128)
    public String getSkillName()
    {
        return skillName;
    }

    public void setSkillName(String skillName)
    {
        this.skillName = skillName;
    }

    @NotBlank(message = "技能类型不能为空")
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }
}

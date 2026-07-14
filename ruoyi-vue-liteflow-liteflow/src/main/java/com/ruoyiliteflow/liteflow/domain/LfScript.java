package com.ruoyiliteflow.liteflow.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyiliteflow.common.annotation.Excel;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 脚本 lf_script
 */
public class LfScript extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "应用名")
    private String applicationName;

    @Excel(name = "脚本ID")
    private String scriptId;

    @Excel(name = "脚本名称")
    private String scriptName;

    private String scriptData;

    /** script / switch_script / boolean_script / for_script */
    private String scriptType;

    /** groovy / qlexpress 等 */
    private String scriptLanguage;

    private Integer enable;

    /** 当前版本号，更新脚本时递增并写入 lf_script_version */
    private Integer version;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "应用名不能为空")
    @Size(max = 64, message = "应用名长度不能超过64个字符")
    public String getApplicationName()
    {
        return applicationName;
    }

    public void setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
    }

    @NotBlank(message = "脚本ID不能为空")
    @Size(max = 64, message = "脚本ID长度不能超过64个字符")
    public String getScriptId()
    {
        return scriptId;
    }

    public void setScriptId(String scriptId)
    {
        this.scriptId = scriptId;
    }

    @Size(max = 128, message = "脚本名称长度不能超过128个字符")
    public String getScriptName()
    {
        return scriptName;
    }

    public void setScriptName(String scriptName)
    {
        this.scriptName = scriptName;
    }

    @NotBlank(message = "脚本内容不能为空")
    public String getScriptData()
    {
        return scriptData;
    }

    public void setScriptData(String scriptData)
    {
        this.scriptData = scriptData;
    }

    public String getScriptType()
    {
        return scriptType;
    }

    public void setScriptType(String scriptType)
    {
        this.scriptType = scriptType;
    }

    public String getScriptLanguage()
    {
        return scriptLanguage;
    }

    public void setScriptLanguage(String scriptLanguage)
    {
        this.scriptLanguage = scriptLanguage;
    }

    public Integer getEnable()
    {
        return enable;
    }

    public void setEnable(Integer enable)
    {
        this.enable = enable;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("applicationName", getApplicationName())
            .append("scriptId", getScriptId())
            .append("scriptName", getScriptName())
            .append("scriptType", getScriptType())
            .append("scriptLanguage", getScriptLanguage())
            .append("enable", getEnable())
            .append("version", getVersion())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

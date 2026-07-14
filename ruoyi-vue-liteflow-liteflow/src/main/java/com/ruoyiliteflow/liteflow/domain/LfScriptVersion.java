package com.ruoyiliteflow.liteflow.domain;

import java.util.Date;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * 脚本版本快照 lf_script_version
 */
public class LfScriptVersion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long scriptPk;
    private String scriptId;
    private Integer version;
    private String scriptData;
    private String scriptType;
    private String scriptLanguage;
    private String publishBy;
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getScriptPk()
    {
        return scriptPk;
    }

    public void setScriptPk(Long scriptPk)
    {
        this.scriptPk = scriptPk;
    }

    public String getScriptId()
    {
        return scriptId;
    }

    public void setScriptId(String scriptId)
    {
        this.scriptId = scriptId;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

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

    public String getPublishBy()
    {
        return publishBy;
    }

    public void setPublishBy(String publishBy)
    {
        this.publishBy = publishBy;
    }

    @Override
    public Date getCreateTime()
    {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}

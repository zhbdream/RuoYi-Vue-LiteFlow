package com.ruoyiliteflow.liteflow.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyiliteflow.common.annotation.Excel;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 链路规则变更审计 lf_chain_audit
 */
public class LfChainAudit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long chainId;

    @Excel(name = "链路ID")
    private String chainName;

    @Excel(name = "操作类型")
    private String actionType;

    private String elBefore;

    private String elAfter;

    @Excel(name = "草稿状态")
    private String draftFlag;

    @Excel(name = "版本")
    private Integer version;

    @Excel(name = "操作人")
    private String operateBy;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getChainId()
    {
        return chainId;
    }

    public void setChainId(Long chainId)
    {
        this.chainId = chainId;
    }

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public String getActionType()
    {
        return actionType;
    }

    public void setActionType(String actionType)
    {
        this.actionType = actionType;
    }

    public String getElBefore()
    {
        return elBefore;
    }

    public void setElBefore(String elBefore)
    {
        this.elBefore = elBefore;
    }

    public String getElAfter()
    {
        return elAfter;
    }

    public void setElAfter(String elAfter)
    {
        this.elAfter = elAfter;
    }

    public String getDraftFlag()
    {
        return draftFlag;
    }

    public void setDraftFlag(String draftFlag)
    {
        this.draftFlag = draftFlag;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getOperateBy()
    {
        return operateBy;
    }

    public void setOperateBy(String operateBy)
    {
        this.operateBy = operateBy;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("chainName", getChainName())
            .append("actionType", getActionType())
            .append("operateBy", getOperateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}

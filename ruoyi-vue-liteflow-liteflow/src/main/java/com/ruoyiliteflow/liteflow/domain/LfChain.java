package com.ruoyiliteflow.liteflow.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyiliteflow.common.annotation.Excel;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 链路 lf_chain
 */
public class LfChain extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "应用名")
    private String applicationName;

    @Excel(name = "链路ID")
    private String chainName;

    @Excel(name = "链路描述")
    private String chainDesc;

    @NotBlank(message = "EL 表达式不能为空")
    private String elData;

    private String graphJson;

    /** LiteFlow 是否生效 1是 0否 */
    private Integer enable;

    /** 状态 0正常 1停用 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 0已发布 1草稿 */
    private String draftFlag;

    private Integer version;

    /** 执行时上下文 Class 全限定名，可选 */
    private String contextClass;

    /** 决策路由 EL（可选） */
    private String routeEl;

    /** 决策路由 namespace（可选） */
    private String namespace;

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

    @NotBlank(message = "链路ID不能为空")
    @Size(max = 64, message = "链路ID长度不能超过64个字符")
    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    @Size(max = 255, message = "链路描述长度不能超过255个字符")
    public String getChainDesc()
    {
        return chainDesc;
    }

    public void setChainDesc(String chainDesc)
    {
        this.chainDesc = chainDesc;
    }

    public String getElData()
    {
        return elData;
    }

    public void setElData(String elData)
    {
        this.elData = elData;
    }

    public String getGraphJson()
    {
        return graphJson;
    }

    public void setGraphJson(String graphJson)
    {
        this.graphJson = graphJson;
    }

    public Integer getEnable()
    {
        return enable;
    }

    public void setEnable(Integer enable)
    {
        this.enable = enable;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    @Size(max = 255, message = "上下文类名长度不能超过255个字符")
    public String getContextClass()
    {
        return contextClass;
    }

    public void setContextClass(String contextClass)
    {
        this.contextClass = contextClass;
    }

    public String getRouteEl()
    {
        return routeEl;
    }

    public void setRouteEl(String routeEl)
    {
        this.routeEl = routeEl;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("applicationName", getApplicationName())
            .append("chainName", getChainName())
            .append("chainDesc", getChainDesc())
            .append("elData", getElData())
            .append("enable", getEnable())
            .append("status", getStatus())
            .append("draftFlag", getDraftFlag())
            .append("version", getVersion())
            .append("contextClass", getContextClass())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

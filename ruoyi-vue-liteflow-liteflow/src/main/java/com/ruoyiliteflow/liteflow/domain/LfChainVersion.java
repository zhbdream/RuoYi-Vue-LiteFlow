package com.ruoyiliteflow.liteflow.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 链路版本快照 lf_chain_version
 */
public class LfChainVersion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long chainId;

    private String chainName;

    private Integer version;

    private String elData;

    private String graphJson;

    private String publishBy;

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

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
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

    public String getPublishBy()
    {
        return publishBy;
    }

    public void setPublishBy(String publishBy)
    {
        this.publishBy = publishBy;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("chainId", getChainId())
            .append("chainName", getChainName())
            .append("version", getVersion())
            .append("publishBy", getPublishBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}

package com.ruoyiliteflow.liteflow.domain.vo;

/**
 * 链路导出 JSON
 */
public class LfChainExportVo
{
    private String applicationName;

    private String chainName;

    private String chainDesc;

    private String elData;

    private String graphJson;

    private String contextClass;

    private Integer version;

    private String remark;

    private String exportTime;

    public String getApplicationName()
    {
        return applicationName;
    }

    public void setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
    }

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

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

    public String getContextClass()
    {
        return contextClass;
    }

    public void setContextClass(String contextClass)
    {
        this.contextClass = contextClass;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getExportTime()
    {
        return exportTime;
    }

    public void setExportTime(String exportTime)
    {
        this.exportTime = exportTime;
    }
}

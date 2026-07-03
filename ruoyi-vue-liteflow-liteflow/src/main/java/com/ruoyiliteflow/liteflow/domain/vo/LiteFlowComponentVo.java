package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.List;

/**
 * LiteFlow 组件注册信息
 */
public class LiteFlowComponentVo
{
    private String nodeId;

    private String name;

    /** common / boolean / switch / for / iterator / script */
    private String nodeType;

    private String className;

    private List<String> refChains;

    private Integer refCount;

    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getNodeType()
    {
        return nodeType;
    }

    public void setNodeType(String nodeType)
    {
        this.nodeType = nodeType;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public List<String> getRefChains()
    {
        return refChains;
    }

    public void setRefChains(List<String> refChains)
    {
        this.refChains = refChains;
    }

    public Integer getRefCount()
    {
        return refCount;
    }

    public void setRefCount(Integer refCount)
    {
        this.refCount = refCount;
    }
}

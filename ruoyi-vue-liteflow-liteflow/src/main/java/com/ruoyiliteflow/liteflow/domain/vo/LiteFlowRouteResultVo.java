package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.List;

/**
 * 决策路由执行结果（多条命中规则）
 */
public class LiteFlowRouteResultVo
{
    private String namespace;
    private int hitCount;
    private List<LiteFlowExecuteResultVo> results;

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public int getHitCount()
    {
        return hitCount;
    }

    public void setHitCount(int hitCount)
    {
        this.hitCount = hitCount;
    }

    public List<LiteFlowExecuteResultVo> getResults()
    {
        return results;
    }

    public void setResults(List<LiteFlowExecuteResultVo> results)
    {
        this.results = results;
    }
}

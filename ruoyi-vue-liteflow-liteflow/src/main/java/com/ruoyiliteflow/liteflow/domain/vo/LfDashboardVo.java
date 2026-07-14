package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.List;

/**
 * 监控仪表盘聚合数据
 */
public class LfDashboardVo
{
    private long totalCalls;
    private long successCount;
    private long failCount;
    private double successRate;
    private long avgDurationMs;
    private List<LfDashboardTrendItem> trend;
    private List<LfDashboardChainStat> chainStats;
    private List<LfDashboardFailTop> failTop;
    private List<LfDashboardSlowTop> slowTop;
    /** 慢节点 Top（按 nodeId 聚合） */
    private List<LfDashboardSlowNodeTop> slowNodeTop;

    public long getTotalCalls()
    {
        return totalCalls;
    }

    public void setTotalCalls(long totalCalls)
    {
        this.totalCalls = totalCalls;
    }

    public long getSuccessCount()
    {
        return successCount;
    }

    public void setSuccessCount(long successCount)
    {
        this.successCount = successCount;
    }

    public long getFailCount()
    {
        return failCount;
    }

    public void setFailCount(long failCount)
    {
        this.failCount = failCount;
    }

    public double getSuccessRate()
    {
        return successRate;
    }

    public void setSuccessRate(double successRate)
    {
        this.successRate = successRate;
    }

    public long getAvgDurationMs()
    {
        return avgDurationMs;
    }

    public void setAvgDurationMs(long avgDurationMs)
    {
        this.avgDurationMs = avgDurationMs;
    }

    public List<LfDashboardTrendItem> getTrend()
    {
        return trend;
    }

    public void setTrend(List<LfDashboardTrendItem> trend)
    {
        this.trend = trend;
    }

    public List<LfDashboardChainStat> getChainStats()
    {
        return chainStats;
    }

    public void setChainStats(List<LfDashboardChainStat> chainStats)
    {
        this.chainStats = chainStats;
    }

    public List<LfDashboardFailTop> getFailTop()
    {
        return failTop;
    }

    public void setFailTop(List<LfDashboardFailTop> failTop)
    {
        this.failTop = failTop;
    }

    public List<LfDashboardSlowTop> getSlowTop()
    {
        return slowTop;
    }

    public void setSlowTop(List<LfDashboardSlowTop> slowTop)
    {
        this.slowTop = slowTop;
    }

    public List<LfDashboardSlowNodeTop> getSlowNodeTop()
    {
        return slowNodeTop;
    }

    public void setSlowNodeTop(List<LfDashboardSlowNodeTop> slowNodeTop)
    {
        this.slowNodeTop = slowNodeTop;
    }

    public static class LfDashboardTrendItem
    {
        private String statDate;
        private long total;
        private long successCount;
        private long failCount;

        public String getStatDate()
        {
            return statDate;
        }

        public void setStatDate(String statDate)
        {
            this.statDate = statDate;
        }

        public long getTotal()
        {
            return total;
        }

        public void setTotal(long total)
        {
            this.total = total;
        }

        public long getSuccessCount()
        {
            return successCount;
        }

        public void setSuccessCount(long successCount)
        {
            this.successCount = successCount;
        }

        public long getFailCount()
        {
            return failCount;
        }

        public void setFailCount(long failCount)
        {
            this.failCount = failCount;
        }
    }

    public static class LfDashboardChainStat
    {
        private String chainName;
        private long total;
        private long successCount;
        private long failCount;
        private long avgDurationMs;

        public String getChainName()
        {
            return chainName;
        }

        public void setChainName(String chainName)
        {
            this.chainName = chainName;
        }

        public long getTotal()
        {
            return total;
        }

        public void setTotal(long total)
        {
            this.total = total;
        }

        public long getSuccessCount()
        {
            return successCount;
        }

        public void setSuccessCount(long successCount)
        {
            this.successCount = successCount;
        }

        public long getFailCount()
        {
            return failCount;
        }

        public void setFailCount(long failCount)
        {
            this.failCount = failCount;
        }

        public long getAvgDurationMs()
        {
            return avgDurationMs;
        }

        public void setAvgDurationMs(long avgDurationMs)
        {
            this.avgDurationMs = avgDurationMs;
        }
    }

    public static class LfDashboardFailTop
    {
        private String chainName;
        private String errorMessage;
        private long count;

        public String getChainName()
        {
            return chainName;
        }

        public void setChainName(String chainName)
        {
            this.chainName = chainName;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
        }

        public long getCount()
        {
            return count;
        }

        public void setCount(long count)
        {
            this.count = count;
        }
    }

    public static class LfDashboardSlowTop
    {
        private Long id;
        private String chainName;
        private String requestId;
        private Long durationMs;
        private String createTime;

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getChainName()
        {
            return chainName;
        }

        public void setChainName(String chainName)
        {
            this.chainName = chainName;
        }

        public String getRequestId()
        {
            return requestId;
        }

        public void setRequestId(String requestId)
        {
            this.requestId = requestId;
        }

        public Long getDurationMs()
        {
            return durationMs;
        }

        public void setDurationMs(Long durationMs)
        {
            this.durationMs = durationMs;
        }

        public String getCreateTime()
        {
            return createTime;
        }

        public void setCreateTime(String createTime)
        {
            this.createTime = createTime;
        }
    }

    public static class LfDashboardSlowNodeTop
    {
        private String nodeId;
        private String chainName;
        private long callCount;
        private long avgDurationMs;
        private long maxDurationMs;

        public String getNodeId()
        {
            return nodeId;
        }

        public void setNodeId(String nodeId)
        {
            this.nodeId = nodeId;
        }

        public String getChainName()
        {
            return chainName;
        }

        public void setChainName(String chainName)
        {
            this.chainName = chainName;
        }

        public long getCallCount()
        {
            return callCount;
        }

        public void setCallCount(long callCount)
        {
            this.callCount = callCount;
        }

        public long getAvgDurationMs()
        {
            return avgDurationMs;
        }

        public void setAvgDurationMs(long avgDurationMs)
        {
            this.avgDurationMs = avgDurationMs;
        }

        public long getMaxDurationMs()
        {
            return maxDurationMs;
        }

        public void setMaxDurationMs(long maxDurationMs)
        {
            this.maxDurationMs = maxDurationMs;
        }
    }
}

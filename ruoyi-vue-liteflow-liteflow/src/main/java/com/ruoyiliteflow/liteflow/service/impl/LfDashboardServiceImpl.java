package com.ruoyiliteflow.liteflow.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.domain.vo.LfDashboardVo;
import com.ruoyiliteflow.liteflow.mapper.LfExecLogMapper;
import com.ruoyiliteflow.liteflow.service.ILfDashboardService;
import com.ruoyiliteflow.liteflow.util.ExecuteStepTimeParser;

@Service
public class LfDashboardServiceImpl implements ILfDashboardService
{
    private static final int SLOW_NODE_TOP_LIMIT = 10;

    @Autowired
    private LfExecLogMapper lfExecLogMapper;

    @Override
    public LfDashboardVo getDashboard(int days)
    {
        if (days <= 0)
        {
            days = 7;
        }
        if (days > 90)
        {
            days = 90;
        }
        LfDashboardVo vo = new LfDashboardVo();
        Map<String, Object> overview = lfExecLogMapper.selectDashboardOverview(days);
        if (overview != null)
        {
            long total = toLong(overview.get("totalCalls"));
            long success = toLong(overview.get("successCount"));
            long fail = toLong(overview.get("failCount"));
            vo.setTotalCalls(total);
            vo.setSuccessCount(success);
            vo.setFailCount(fail);
            vo.setSuccessRate(total > 0 ? Math.round(success * 10000.0 / total) / 100.0 : 0);
            vo.setAvgDurationMs(toLong(overview.get("avgDurationMs")));
        }
        vo.setTrend(lfExecLogMapper.selectDashboardTrend(days));
        vo.setChainStats(lfExecLogMapper.selectDashboardChainStats(days));
        vo.setFailTop(lfExecLogMapper.selectDashboardFailTop(days));
        vo.setSlowTop(lfExecLogMapper.selectDashboardSlowTop(days));
        vo.setSlowNodeTop(buildSlowNodeTop(days));
        return vo;
    }

    private List<LfDashboardVo.LfDashboardSlowNodeTop> buildSlowNodeTop(int days)
    {
        List<LfExecLog> logs = lfExecLogMapper.selectRecentStepTimeLogs(days);
        Map<String, NodeAgg> aggMap = new HashMap<>();
        if (logs != null)
        {
            for (LfExecLog log : logs)
            {
                List<ExecuteStepTimeParser.NodeStepTime> steps = resolveStepTimes(log);
                for (ExecuteStepTimeParser.NodeStepTime step : steps)
                {
                    if (step.getTimeMs() <= 0)
                    {
                        continue;
                    }
                    NodeAgg agg = aggMap.computeIfAbsent(step.getNodeId(), k -> new NodeAgg(k));
                    agg.add(step.getTimeMs(), log.getChainName());
                }
            }
        }
        List<LfDashboardVo.LfDashboardSlowNodeTop> result = new ArrayList<>();
        for (NodeAgg agg : aggMap.values())
        {
            LfDashboardVo.LfDashboardSlowNodeTop item = new LfDashboardVo.LfDashboardSlowNodeTop();
            item.setNodeId(agg.nodeId);
            item.setChainName(agg.topChainName());
            item.setCallCount(agg.count);
            item.setAvgDurationMs(Math.round(agg.sumMs * 1.0 / agg.count));
            item.setMaxDurationMs(agg.maxMs);
            result.add(item);
        }
        result.sort(Comparator.comparingLong(LfDashboardVo.LfDashboardSlowNodeTop::getAvgDurationMs).reversed());
        if (result.size() > SLOW_NODE_TOP_LIMIT)
        {
            return result.subList(0, SLOW_NODE_TOP_LIMIT);
        }
        return result;
    }

    private List<ExecuteStepTimeParser.NodeStepTime> resolveStepTimes(LfExecLog log)
    {
        if (log == null)
        {
            return List.of();
        }
        if (StringUtils.isNotEmpty(log.getStepsJson()))
        {
            try
            {
                JSONObject root = JSON.parseObject(log.getStepsJson());
                if (root != null)
                {
                    JSONArray arr = root.getJSONArray("nodeSteps");
                    if (arr != null && !arr.isEmpty())
                    {
                        List<ExecuteStepTimeParser.NodeStepTime> list = new ArrayList<>(arr.size());
                        for (int i = 0; i < arr.size(); i++)
                        {
                            JSONObject item = arr.getJSONObject(i);
                            if (item == null)
                            {
                                continue;
                            }
                            String nodeId = item.getString("nodeId");
                            Long timeMs = item.getLong("timeMs");
                            if (StringUtils.isNotEmpty(nodeId) && timeMs != null)
                            {
                                list.add(new ExecuteStepTimeParser.NodeStepTime(nodeId, timeMs));
                            }
                        }
                        if (!list.isEmpty())
                        {
                            return list;
                        }
                    }
                }
            }
            catch (Exception ignored)
            {
                // fall through to string parse
            }
        }
        return ExecuteStepTimeParser.parse(log.getExecuteStepStrWithTime());
    }

    private long toLong(Object value)
    {
        if (value == null)
        {
            return 0L;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static final class NodeAgg
    {
        private final String nodeId;
        private long count;
        private long sumMs;
        private long maxMs;
        private final Map<String, Long> chainCounts = new HashMap<>();

        private NodeAgg(String nodeId)
        {
            this.nodeId = nodeId;
        }

        private void add(long timeMs, String chainName)
        {
            count++;
            sumMs += timeMs;
            if (timeMs > maxMs)
            {
                maxMs = timeMs;
            }
            if (StringUtils.isNotEmpty(chainName))
            {
                chainCounts.merge(chainName, 1L, Long::sum);
            }
        }

        private String topChainName()
        {
            String best = null;
            long bestCount = -1;
            for (Map.Entry<String, Long> e : chainCounts.entrySet())
            {
                if (e.getValue() > bestCount)
                {
                    bestCount = e.getValue();
                    best = e.getKey();
                }
            }
            return best;
        }
    }
}

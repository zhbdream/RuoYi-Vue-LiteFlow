package com.ruoyiliteflow.liteflow.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.liteflow.domain.vo.LfDashboardVo;
import com.ruoyiliteflow.liteflow.mapper.LfExecLogMapper;
import com.ruoyiliteflow.liteflow.service.ILfDashboardService;

@Service
public class LfDashboardServiceImpl implements ILfDashboardService
{
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
        return vo;
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
}

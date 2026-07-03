package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.domain.vo.LfDashboardVo;

public interface LfExecLogMapper
{
    LfExecLog selectLfExecLogById(Long id);

    LfExecLog selectLfExecLogByRequestId(String requestId);

    List<LfExecLog> selectLfExecLogList(LfExecLog lfExecLog);

    int insertLfExecLog(LfExecLog lfExecLog);

    int deleteLfExecLogByIds(Long[] ids);

    int cleanLfExecLog();

    Map<String, Object> selectDashboardOverview(@Param("days") int days);

    List<LfDashboardVo.LfDashboardTrendItem> selectDashboardTrend(@Param("days") int days);

    List<LfDashboardVo.LfDashboardChainStat> selectDashboardChainStats(@Param("days") int days);

    List<LfDashboardVo.LfDashboardFailTop> selectDashboardFailTop(@Param("days") int days);

    List<LfDashboardVo.LfDashboardSlowTop> selectDashboardSlowTop(@Param("days") int days);
}

package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;

public interface ILfExecLogService
{
    LfExecLog selectLfExecLogById(Long id);

    LfExecLog selectLfExecLogByRequestId(String requestId);

    List<LfExecLog> selectLfExecLogList(LfExecLog lfExecLog);

    void saveExecuteLog(String chainName, Object param, LiteFlowExecuteResultVo result, long durationMs, String createBy);

    int deleteLfExecLogByIds(Long[] ids);

    int cleanLfExecLog();
}

package com.ruoyiliteflow.liteflow.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.mapper.LfExecLogMapper;
import com.ruoyiliteflow.liteflow.service.ILfExecLogService;
import com.ruoyiliteflow.liteflow.util.ExecuteStepTimeParser;

@Service
public class LfExecLogServiceImpl implements ILfExecLogService
{
    @Autowired
    private LfExecLogMapper lfExecLogMapper;

    @Override
    public LfExecLog selectLfExecLogById(Long id)
    {
        return lfExecLogMapper.selectLfExecLogById(id);
    }

    @Override
    public LfExecLog selectLfExecLogByRequestId(String requestId)
    {
        return lfExecLogMapper.selectLfExecLogByRequestId(requestId);
    }

    @Override
    public List<LfExecLog> selectLfExecLogList(LfExecLog lfExecLog)
    {
        return lfExecLogMapper.selectLfExecLogList(lfExecLog);
    }

    @Override
    public void saveExecuteLog(String chainName, Object param, LiteFlowExecuteResultVo result, long durationMs, String createBy)
    {
        if (result == null)
        {
            return;
        }
        LfExecLog log = new LfExecLog();
        log.setRequestId(result.getRequestId());
        log.setChainName(chainName);
        log.setSuccess(result.isSuccess() ? 1 : 0);
        log.setCode(result.getCode());
        log.setMessage(result.getMessage());
        log.setExecuteStepStr(result.getExecuteStepStr());
        log.setExecuteStepStrWithTime(result.getExecuteStepStrWithTime());
        log.setFailedNodeId(result.getFailedNodeId());
        log.setDurationMs(durationMs);
        log.setCreateBy(createBy);

        Map<String, Object> steps = new HashMap<>();
        steps.put("executeStepStr", result.getExecuteStepStr());
        steps.put("executeStepStrWithTime", result.getExecuteStepStrWithTime());
        steps.put("failedNodeId", result.getFailedNodeId());
        List<ExecuteStepTimeParser.NodeStepTime> nodeSteps = ExecuteStepTimeParser.parse(result.getExecuteStepStrWithTime());
        if (!nodeSteps.isEmpty())
        {
            List<Map<String, Object>> nodeStepList = new ArrayList<>(nodeSteps.size());
            for (ExecuteStepTimeParser.NodeStepTime item : nodeSteps)
            {
                Map<String, Object> row = new HashMap<>(2);
                row.put("nodeId", item.getNodeId());
                row.put("timeMs", item.getTimeMs());
                nodeStepList.add(row);
            }
            steps.put("nodeSteps", nodeStepList);
        }
        log.setStepsJson(JSON.toJSONString(steps));

        if (param != null)
        {
            log.setParamJson(JSON.toJSONString(param));
        }
        if (result.getContextData() != null)
        {
            log.setContextJson(JSON.toJSONString(result.getContextData()));
        }
        if (!result.isSuccess() && StringUtils.isNotEmpty(result.getMessage()))
        {
            log.setErrorMessage(result.getMessage());
        }
        lfExecLogMapper.insertLfExecLog(log);
        result.setLogId(log.getId());
    }

    @Override
    public int deleteLfExecLogByIds(Long[] ids)
    {
        return lfExecLogMapper.deleteLfExecLogByIds(ids);
    }

    @Override
    public int cleanLfExecLog()
    {
        return lfExecLogMapper.cleanLfExecLog();
    }
}

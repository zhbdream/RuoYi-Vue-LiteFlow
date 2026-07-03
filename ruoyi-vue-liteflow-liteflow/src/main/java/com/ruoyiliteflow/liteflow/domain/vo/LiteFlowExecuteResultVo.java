package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.Map;

/**
 * 链路执行结果
 */
public class LiteFlowExecuteResultVo
{
    private boolean success;

    private String code;

    private String message;

    private String chainId;

    private String requestId;

    private String executeStepStr;

    private String executeStepStrWithTime;

    private Map<String, Object> contextData;

    private Long logId;

    /** 失败时最后执行的节点 ID */
    private String failedNodeId;

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getChainId()
    {
        return chainId;
    }

    public void setChainId(String chainId)
    {
        this.chainId = chainId;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getExecuteStepStr()
    {
        return executeStepStr;
    }

    public void setExecuteStepStr(String executeStepStr)
    {
        this.executeStepStr = executeStepStr;
    }

    public String getExecuteStepStrWithTime()
    {
        return executeStepStrWithTime;
    }

    public void setExecuteStepStrWithTime(String executeStepStrWithTime)
    {
        this.executeStepStrWithTime = executeStepStrWithTime;
    }

    public Map<String, Object> getContextData()
    {
        return contextData;
    }

    public void setContextData(Map<String, Object> contextData)
    {
        this.contextData = contextData;
    }

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public String getFailedNodeId()
    {
        return failedNodeId;
    }

    public void setFailedNodeId(String failedNodeId)
    {
        this.failedNodeId = failedNodeId;
    }
}

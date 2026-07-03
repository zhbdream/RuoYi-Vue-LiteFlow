package com.ruoyiliteflow.liteflow.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyiliteflow.common.annotation.Excel;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 执行日志 lf_exec_log
 */
public class LfExecLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "请求ID")
    private String requestId;

    @Excel(name = "链路ID")
    private String chainName;

    private Integer success;

    private String code;

    private String message;

    private String executeStepStr;

    private String executeStepStrWithTime;

    private String stepsJson;

    private String paramJson;

    private String contextJson;

    private Long durationMs;

    private String errorMessage;

    private String failedNodeId;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public Integer getSuccess()
    {
        return success;
    }

    public void setSuccess(Integer success)
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

    public String getStepsJson()
    {
        return stepsJson;
    }

    public void setStepsJson(String stepsJson)
    {
        this.stepsJson = stepsJson;
    }

    public String getParamJson()
    {
        return paramJson;
    }

    public void setParamJson(String paramJson)
    {
        this.paramJson = paramJson;
    }

    public String getContextJson()
    {
        return contextJson;
    }

    public void setContextJson(String contextJson)
    {
        this.contextJson = contextJson;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getFailedNodeId()
    {
        return failedNodeId;
    }

    public void setFailedNodeId(String failedNodeId)
    {
        this.failedNodeId = failedNodeId;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("requestId", getRequestId())
            .append("chainName", getChainName())
            .append("success", getSuccess())
            .append("durationMs", getDurationMs())
            .append("createTime", getCreateTime())
            .toString();
    }
}

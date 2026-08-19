package com.ruoyiliteflow.liteflow.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    private String webhookUrl;

    /** 空未投递 0投递中 1成功 2失败 3跳过 */
    private String webhookStatus;

    private Integer webhookAttempts;

    private Integer webhookHttpStatus;

    private String webhookMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date webhookTime;

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

    public String getWebhookUrl()
    {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl)
    {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookStatus()
    {
        return webhookStatus;
    }

    public void setWebhookStatus(String webhookStatus)
    {
        this.webhookStatus = webhookStatus;
    }

    public Integer getWebhookAttempts()
    {
        return webhookAttempts;
    }

    public void setWebhookAttempts(Integer webhookAttempts)
    {
        this.webhookAttempts = webhookAttempts;
    }

    public Integer getWebhookHttpStatus()
    {
        return webhookHttpStatus;
    }

    public void setWebhookHttpStatus(Integer webhookHttpStatus)
    {
        this.webhookHttpStatus = webhookHttpStatus;
    }

    public String getWebhookMessage()
    {
        return webhookMessage;
    }

    public void setWebhookMessage(String webhookMessage)
    {
        this.webhookMessage = webhookMessage;
    }

    public Date getWebhookTime()
    {
        return webhookTime;
    }

    public void setWebhookTime(Date webhookTime)
    {
        this.webhookTime = webhookTime;
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

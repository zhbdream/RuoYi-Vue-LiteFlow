package com.ruoyiliteflow.liteflow.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 链路试跑用例 lf_chain_case
 */
public class LfChainCase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private String chainName;

    private String caseName;

    /** 请求参数 JSON（与试跑 body 相同） */
    private String paramJson;

    /** 期望成功 0否 1是 */
    private String expectSuccess;

    /** 可选：executeStepStr 应包含 */
    private String expectStepContains;

    private Integer sortOrder;

    /** 0正常 1停用 */
    private String status;

    /** 最近回归 0失败 1通过 */
    private String lastRunSuccess;

    private Long lastRunLogId;

    private String lastRunMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastRunTime;

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

    public String getCaseName()
    {
        return caseName;
    }

    public void setCaseName(String caseName)
    {
        this.caseName = caseName;
    }

    public String getParamJson()
    {
        return paramJson;
    }

    public void setParamJson(String paramJson)
    {
        this.paramJson = paramJson;
    }

    public String getExpectSuccess()
    {
        return expectSuccess;
    }

    public void setExpectSuccess(String expectSuccess)
    {
        this.expectSuccess = expectSuccess;
    }

    public String getExpectStepContains()
    {
        return expectStepContains;
    }

    public void setExpectStepContains(String expectStepContains)
    {
        this.expectStepContains = expectStepContains;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getLastRunSuccess()
    {
        return lastRunSuccess;
    }

    public void setLastRunSuccess(String lastRunSuccess)
    {
        this.lastRunSuccess = lastRunSuccess;
    }

    public Long getLastRunLogId()
    {
        return lastRunLogId;
    }

    public void setLastRunLogId(Long lastRunLogId)
    {
        this.lastRunLogId = lastRunLogId;
    }

    public String getLastRunMessage()
    {
        return lastRunMessage;
    }

    public void setLastRunMessage(String lastRunMessage)
    {
        this.lastRunMessage = lastRunMessage;
    }

    public Date getLastRunTime()
    {
        return lastRunTime;
    }

    public void setLastRunTime(Date lastRunTime)
    {
        this.lastRunTime = lastRunTime;
    }
}

package com.ruoyiliteflow.liteflow.domain.vo;

/**
 * 单条用例回归结果
 */
public class LfChainCaseRunVo
{
    private Long caseId;

    private String caseName;

    /** 断言是否通过 */
    private boolean passed;

    private String message;

    private Long logId;

    private boolean executeSuccess;

    private String executeStepStr;

    public Long getCaseId()
    {
        return caseId;
    }

    public void setCaseId(Long caseId)
    {
        this.caseId = caseId;
    }

    public String getCaseName()
    {
        return caseName;
    }

    public void setCaseName(String caseName)
    {
        this.caseName = caseName;
    }

    public boolean isPassed()
    {
        return passed;
    }

    public void setPassed(boolean passed)
    {
        this.passed = passed;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public boolean isExecuteSuccess()
    {
        return executeSuccess;
    }

    public void setExecuteSuccess(boolean executeSuccess)
    {
        this.executeSuccess = executeSuccess;
    }

    public String getExecuteStepStr()
    {
        return executeStepStr;
    }

    public void setExecuteStepStr(String executeStepStr)
    {
        this.executeStepStr = executeStepStr;
    }
}

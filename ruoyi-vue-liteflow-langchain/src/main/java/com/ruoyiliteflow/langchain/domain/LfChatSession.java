package com.ruoyiliteflow.langchain.domain;

import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * AI 内部助手会话 lf_chat_session
 */
public class LfChatSession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    /** 归属用户 username */
    private String userName;

    private String modelCode;

    private String modelName;

    /** 绑定的智能体编码；空表示轻量模型对话 */
    private String agentCode;

    /** 0正常 1删除 */
    private String status;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getModelCode()
    {
        return modelCode;
    }

    public void setModelCode(String modelCode)
    {
        this.modelCode = modelCode;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}

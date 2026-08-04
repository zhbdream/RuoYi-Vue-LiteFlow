package com.ruoyiliteflow.aikit.platform.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiMemoryItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String agentCode;
    private String sessionId;
    private String principal;
    private String memoryType;
    private String role;
    private String content;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "智能体编码不能为空")
    @Size(max = 64)
    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    @Size(max = 64)
    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

    @Size(max = 64)
    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal(String principal)
    {
        this.principal = principal;
    }

    public String getMemoryType()
    {
        return memoryType;
    }

    public void setMemoryType(String memoryType)
    {
        this.memoryType = memoryType;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    @NotBlank(message = "记忆内容不能为空")
    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }
}

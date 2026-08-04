package com.ruoyiliteflow.aikit.platform.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

public class AiKnowledgeBase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String kbCode;
    private String kbName;
    private String description;
    private String status;
    private Integer chunkCount;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "知识库编码不能为空")
    @Size(max = 64)
    public String getKbCode()
    {
        return kbCode;
    }

    public void setKbCode(String kbCode)
    {
        this.kbCode = kbCode;
    }

    @Size(max = 128)
    public String getKbName()
    {
        return kbName;
    }

    public void setKbName(String kbName)
    {
        this.kbName = kbName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getChunkCount()
    {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount)
    {
        this.chunkCount = chunkCount;
    }
}

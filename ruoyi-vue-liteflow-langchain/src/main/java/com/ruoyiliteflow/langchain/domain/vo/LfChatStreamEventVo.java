package com.ruoyiliteflow.langchain.domain.vo;

import java.util.List;

/**
 * 内部助手 SSE 事件载荷
 */
public class LfChatStreamEventVo
{
    private String text;

    private Long sessionId;

    private Long messageId;

    private String content;

    private String model;

    private String title;

    private String agentCode;

    private Object tool;

    private List<Object> tools;

    public static LfChatStreamEventVo delta(String text)
    {
        LfChatStreamEventVo vo = new LfChatStreamEventVo();
        vo.setText(text);
        return vo;
    }

    public static LfChatStreamEventVo toolEvent(Object tool)
    {
        LfChatStreamEventVo vo = new LfChatStreamEventVo();
        vo.setTool(tool);
        return vo;
    }

    public static LfChatStreamEventVo done(Long sessionId, Long messageId, String content, String model, String title)
    {
        return done(sessionId, messageId, content, model, title, null);
    }

    public static LfChatStreamEventVo done(Long sessionId, Long messageId, String content, String model, String title,
            String agentCode)
    {
        LfChatStreamEventVo vo = new LfChatStreamEventVo();
        vo.setSessionId(sessionId);
        vo.setMessageId(messageId);
        vo.setContent(content);
        vo.setModel(model);
        vo.setTitle(title);
        vo.setAgentCode(agentCode);
        return vo;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    public Long getMessageId()
    {
        return messageId;
    }

    public void setMessageId(Long messageId)
    {
        this.messageId = messageId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAgentCode()
    {
        return agentCode;
    }

    public void setAgentCode(String agentCode)
    {
        this.agentCode = agentCode;
    }

    public Object getTool()
    {
        return tool;
    }

    public void setTool(Object tool)
    {
        this.tool = tool;
    }

    public List<Object> getTools()
    {
        return tools;
    }

    public void setTools(List<Object> tools)
    {
        this.tools = tools;
    }
}

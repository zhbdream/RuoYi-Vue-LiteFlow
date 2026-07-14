package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.Map;

/**
 * SSE 推送的 Agent / 流程事件
 */
public class LiteFlowStreamEventVo
{
    private String type;
    private String chainId;
    private String nodeId;
    private String requestId;
    private String conversationId;
    private String text;
    private boolean last;
    private long timestamp;
    private Map<String, Object> data;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getChainId()
    {
        return chainId;
    }

    public void setChainId(String chainId)
    {
        this.chainId = chainId;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId = conversationId;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public boolean isLast()
    {
        return last;
    }

    public void setLast(boolean last)
    {
        this.last = last;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData()
    {
        return data;
    }

    public void setData(Map<String, Object> data)
    {
        this.data = data;
    }
}

package com.ruoyiliteflow.langchain.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

/**
 * LangGraph4j 共享状态：消息轨迹 + 事实/结论字段
 */
public class RiskGraphState extends AgentState
{
    public static final String MESSAGES_KEY = "messages";
    public static final String FACTS_KEY = "facts";
    public static final String ANALYSIS_KEY = "analysis";
    public static final String LEVEL_KEY = "riskLevel";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            MESSAGES_KEY, Channels.appender(ArrayList::new),
            FACTS_KEY, Channels.base(() -> ""),
            ANALYSIS_KEY, Channels.base(() -> ""),
            LEVEL_KEY, Channels.base(() -> "")
    );

    public RiskGraphState(Map<String, Object> initData)
    {
        super(initData);
    }

    public List<String> messages()
    {
        return this.<List<String>>value(MESSAGES_KEY).orElse(List.of());
    }

    public String facts()
    {
        return this.<String>value(FACTS_KEY).orElse("");
    }

    public String analysis()
    {
        return this.<String>value(ANALYSIS_KEY).orElse("");
    }

    public String riskLevel()
    {
        return this.<String>value(LEVEL_KEY).orElse("");
    }

    public Optional<String> optional(String key)
    {
        return this.value(key);
    }
}
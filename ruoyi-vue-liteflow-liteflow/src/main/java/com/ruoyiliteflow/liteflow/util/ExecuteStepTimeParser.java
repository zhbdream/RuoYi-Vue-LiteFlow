package com.ruoyiliteflow.liteflow.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 解析 LiteFlow {@code executeStepStrWithTime}，提取 nodeId 与耗时。
 * <p>示例：{@code helloA<5>==>helloB[描述]<12>}
 */
public final class ExecuteStepTimeParser
{
    private static final Pattern STEP_PATTERN = Pattern.compile("([A-Za-z0-9_$#]+)(?:\\[[^\\]]*\\])?<(\\d+)>");

    private ExecuteStepTimeParser()
    {
    }

    public static List<NodeStepTime> parse(String executeStepStrWithTime)
    {
        if (StringUtils.isEmpty(executeStepStrWithTime))
        {
            return Collections.emptyList();
        }
        List<NodeStepTime> list = new ArrayList<>();
        Matcher matcher = STEP_PATTERN.matcher(executeStepStrWithTime);
        while (matcher.find())
        {
            String nodeId = matcher.group(1);
            long timeMs = Long.parseLong(matcher.group(2));
            list.add(new NodeStepTime(nodeId, timeMs));
        }
        return list;
    }

    public static final class NodeStepTime
    {
        private final String nodeId;
        private final long timeMs;

        public NodeStepTime(String nodeId, long timeMs)
        {
            this.nodeId = nodeId;
            this.timeMs = timeMs;
        }

        public String getNodeId()
        {
            return nodeId;
        }

        public long getTimeMs()
        {
            return timeMs;
        }
    }
}

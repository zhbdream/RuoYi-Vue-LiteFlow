package com.ruoyiliteflow.aicore.spi;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.http.HttpUtils;

/**
 * 默认工具执行：echo / HTTP。MCP 由 platform 的 Primary 实现覆盖。
 */
@Component
public class DefaultToolExecutor implements ToolExecutor
{
    @Override
    public String execute(ToolDescriptor tool, String argumentsJson)
    {
        String args = StringUtils.isEmpty(argumentsJson) ? "{}" : argumentsJson.trim();
        String invokeKey = tool == null ? "" : tool.getInvokeKey();
        String code = tool == null ? "" : tool.getToolCode();
        if (StringUtils.isEmpty(invokeKey) || "echo".equalsIgnoreCase(invokeKey))
        {
            return "{\"ok\":true,\"tool\":\"" + escape(code) + "\",\"echo\":" + asJsonValue(args) + "}";
        }
        if (invokeKey.startsWith("http://") || invokeKey.startsWith("https://"))
        {
            String result = HttpUtils.sendPost(invokeKey, args, MediaType.APPLICATION_JSON_VALUE);
            return result == null ? "" : result;
        }
        return "{\"ok\":true,\"tool\":\"" + escape(code) + "\",\"invokeKey\":\"" + escape(invokeKey)
                + "\",\"note\":\"no local handler, echoed\",\"args\":" + asJsonValue(args) + "}";
    }

    private static String asJsonValue(String args)
    {
        if (StringUtils.isEmpty(args))
        {
            return "{}";
        }
        String t = args.trim();
        if (t.startsWith("{") || t.startsWith("["))
        {
            return t;
        }
        return "\"" + escape(t) + "\"";
    }

    private static String escape(String s)
    {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.ruoyiliteflow.mcp.stdio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.mcp.McpServerApplication;
import com.ruoyiliteflow.mcp.tool.GovernanceMcpTools;
import com.ruoyiliteflow.mcp.tool.McpToolRegistry;

/**
 * 简化版 MCP stdio：tools/list 与 tools/call（JSON-RPC 行协议）。
 */
public final class McpStdioLauncher
{
    private McpStdioLauncher()
    {
    }

    public static void main(String[] args) throws Exception
    {
        System.setProperty("spring.main.web-application-type", "none");
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(McpServerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        McpToolRegistry registry = ctx.getBean(McpToolRegistry.class);
        GovernanceMcpTools governance = ctx.getBean(GovernanceMcpTools.class);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }
            try
            {
                JSONObject req = JSON.parseObject(line);
                Object id = req.get("id");
                String method = req.getString("method");
                JSONObject params = req.getJSONObject("params");
                Object result;
                if ("tools/list".equals(method) || "list_tools".equals(method))
                {
                    result = Map.of("tools", registry.listAllTools());
                }
                else if ("tools/call".equals(method) || "call_tool".equals(method))
                {
                    String name = params == null ? null : params.getString("name");
                    JSONObject arguments = params == null ? null : params.getJSONObject("arguments");
                    Object data = dispatch(registry, governance, name, arguments);
                    result = Map.of("content", List.of(Map.of("type", "text", "text", JSON.toJSONString(data))));
                }
                else if ("initialize".equals(method))
                {
                    Map<String, Object> serverInfo = new LinkedHashMap<>();
                    serverInfo.put("name", "ruoyi-ai-kit-mcp");
                    serverInfo.put("version", "3.9.2");
                    result = Map.of(
                            "protocolVersion", "2024-11-05",
                            "capabilities", Map.of("tools", Map.of()),
                            "serverInfo", serverInfo);
                }
                else if ("notifications/initialized".equals(method) || "ping".equals(method))
                {
                    write(Map.of("jsonrpc", "2.0", "id", id, "result", Map.of()));
                    continue;
                }
                else
                {
                    write(error(id, -32601, "Method not found: " + method));
                    continue;
                }
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("jsonrpc", "2.0");
                resp.put("id", id);
                resp.put("result", result);
                write(resp);
            }
            catch (Exception e)
            {
                write(error(null, -32000, e.getMessage()));
            }
        }
        ctx.close();
    }

    private static Object dispatch(McpToolRegistry registry, GovernanceMcpTools governance, String name,
            JSONObject args)
    {
        for (Map<String, Object> t : registry.listAiCoreTools())
        {
            if (name != null && name.equals(t.get("name")))
            {
                return registry.callAiCore(name, args);
            }
        }
        if (governance.enabled())
        {
            for (Map<String, Object> t : governance.listTools())
            {
                if (name != null && name.equals(t.get("name")))
                {
                    return governance.call(name, args);
                }
            }
        }
        // 兜底：动态工具可能挂在其它 server key
        return registry.callDynamic(name, args);
    }

    private static Map<String, Object> error(Object id, int code, String message)
    {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("id", id);
        resp.put("error", Map.of("code", code, "message", message == null ? "error" : message));
        return resp;
    }

    private static void write(Object obj)
    {
        System.out.println(JSON.toJSONString(obj));
        System.out.flush();
    }
}

package com.ruoyiliteflow.mcp.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.http.HttpUtils;

/**
 * 合并静态 Tool + 动态注册（DB / 内存）的 MCP Tool 注册表。
 */
@Component
public class McpToolRegistry
{
    private final AiCoreMcpTools aiCoreMcpTools;
    private final GovernanceMcpTools governanceMcpTools;
    private final ConcurrentHashMap<String, DynamicTool> memoryTools = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private DynamicToolSource dynamicToolSource;

    @Autowired(required = false)
    private LiteFlowOpenExecuteClient liteFlowOpenExecuteClient;

    public McpToolRegistry(AiCoreMcpTools aiCoreMcpTools, GovernanceMcpTools governanceMcpTools)
    {
        this.aiCoreMcpTools = aiCoreMcpTools;
        this.governanceMcpTools = governanceMcpTools;
    }

    public List<Map<String, Object>> listAllTools()
    {
        List<Map<String, Object>> all = new ArrayList<>();
        if (aiCoreMcpTools.enabled())
        {
            all.addAll(aiCoreMcpTools.listTools());
        }
        if (governanceMcpTools.enabled())
        {
            all.addAll(governanceMcpTools.listTools());
        }
        for (DynamicTool t : memoryTools.values())
        {
            all.add(t.toMap());
        }
        if (dynamicToolSource != null)
        {
            for (DynamicTool t : dynamicToolSource.loadTools())
            {
                if (t != null && StringUtils.isNotEmpty(t.name()) && !containsName(all, t.name()))
                {
                    all.add(t.toMap());
                }
            }
        }
        return all;
    }

    public List<Map<String, Object>> listAiCoreTools()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        if (aiCoreMcpTools.enabled())
        {
            list.addAll(aiCoreMcpTools.listTools());
        }
        for (DynamicTool t : memoryTools.values())
        {
            if ("ai-core".equals(t.server()))
            {
                list.add(t.toMap());
            }
        }
        if (dynamicToolSource != null)
        {
            for (DynamicTool t : dynamicToolSource.loadTools())
            {
                if (t != null && "ai-core".equals(t.server()) && !containsName(list, t.name()))
                {
                    list.add(t.toMap());
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> listByServer(String server)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        if (StringUtils.isEmpty(server))
        {
            return list;
        }
        for (DynamicTool t : memoryTools.values())
        {
            if (server.equals(t.server()))
            {
                list.add(t.toMap());
            }
        }
        if (dynamicToolSource != null)
        {
            for (DynamicTool t : dynamicToolSource.loadTools())
            {
                if (t != null && server.equals(t.server()) && !containsName(list, t.name()))
                {
                    list.add(t.toMap());
                }
            }
        }
        return list;
    }

    public Object callAiCore(String toolName, JSONObject args)
    {
        try
        {
            return aiCoreMcpTools.call(toolName, args);
        }
        catch (ServiceException ex)
        {
            if (ex.getMessage() != null && ex.getMessage().startsWith("未知 Tool"))
            {
                return callDynamic(toolName, args);
            }
            throw ex;
        }
    }

    public Object callDynamic(String toolName, JSONObject args)
    {
        DynamicTool tool = memoryTools.get(toolName);
        if (tool == null && dynamicToolSource != null)
        {
            for (DynamicTool t : dynamicToolSource.loadTools())
            {
                if (t != null && toolName.equals(t.name()))
                {
                    tool = t;
                    break;
                }
            }
        }
        if (tool == null)
        {
            throw new ServiceException("未知 Tool: " + toolName);
        }
        String invokeKey = tool.invokeKey();
        if ("liteflow".equals(tool.server()))
        {
            String chainName = StringUtils.isNotEmpty(invokeKey) ? invokeKey : stripLfPrefix(toolName);
            if (liteFlowOpenExecuteClient == null)
            {
                return Map.of("ok", false, "error", "未配置 LiteFlow 开放执行客户端");
            }
            return liteFlowOpenExecuteClient.execute(chainName, args);
        }
        if (StringUtils.isEmpty(invokeKey) || "echo".equalsIgnoreCase(invokeKey))
        {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            resp.put("tool", toolName);
            resp.put("dynamic", true);
            resp.put("echo", args == null ? Map.of() : args);
            return resp;
        }
        if (invokeKey.startsWith("http://") || invokeKey.startsWith("https://"))
        {
            String body = args == null ? "{}" : args.toJSONString();
            String result = HttpUtils.sendPost(invokeKey, body, MediaType.APPLICATION_JSON_VALUE);
            return Map.of("ok", true, "tool", toolName, "httpResult", result == null ? "" : result);
        }
        // invoke_key 映射到静态 tool 名
        try
        {
            return aiCoreMcpTools.call(invokeKey, args);
        }
        catch (ServiceException ignored)
        {
            return Map.of("ok", true, "tool", toolName, "invokeKey", invokeKey, "note", "dynamic tool registered, no handler");
        }
    }

    public void register(DynamicTool tool)
    {
        if (tool != null && StringUtils.isNotEmpty(tool.name()))
        {
            memoryTools.put(tool.name(), tool);
        }
    }

    public void unregister(String name)
    {
        if (StringUtils.isNotEmpty(name))
        {
            memoryTools.remove(name);
        }
    }

    private static String stripLfPrefix(String toolName)
    {
        if (toolName != null && toolName.startsWith("lf_"))
        {
            return toolName.substring(3);
        }
        return toolName;
    }

    private static boolean containsName(List<Map<String, Object>> list, String name)
    {
        for (Map<String, Object> m : list)
        {
            if (name.equals(m.get("name")))
            {
                return true;
            }
        }
        return false;
    }

    public record DynamicTool(String name, String description, String server, String invokeKey)
    {
        public Map<String, Object> toMap()
        {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("description", description == null ? "" : description);
            m.put("server", server == null ? "ai-core" : server);
            m.put("dynamic", true);
            m.put("invokeKey", invokeKey == null ? "" : invokeKey);
            return m;
        }
    }

    public interface DynamicToolSource
    {
        List<DynamicTool> loadTools();
    }
}

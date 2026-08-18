package com.ruoyiliteflow.mcp.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.mcp.tool.McpToolRegistry;
import com.ruoyiliteflow.mcp.tool.McpToolRegistry.DynamicTool;

/**
 * 运行时动态注册 / 注销 MCP Tool（内存），并可与 JDBC 源并存。
 */
@RestController
@RequestMapping("/mcp/dynamic-tools")
public class McpDynamicToolController
{
    private final McpToolRegistry registry;

    public McpDynamicToolController(McpToolRegistry registry)
    {
        this.registry = registry;
    }

    @PostMapping
    public AjaxResult register(@RequestBody Map<String, Object> body)
    {
        String name = str(body.get("name"));
        if (StringUtils.isEmpty(name))
        {
            name = str(body.get("toolCode"));
        }
        if (StringUtils.isEmpty(name))
        {
            return AjaxResult.error("name/toolCode required");
        }
        String desc = str(body.get("description"));
        if (StringUtils.isEmpty(desc))
        {
            desc = str(body.get("toolName"));
        }
        String server = str(body.get("server"));
        if (StringUtils.isEmpty(server))
        {
            server = str(body.get("mcpServerKey"));
        }
        if (StringUtils.isEmpty(server))
        {
            server = "ai-core";
        }
        String invokeKey = str(body.get("invokeKey"));
        registry.register(new DynamicTool(name, desc, server, invokeKey));
        return AjaxResult.success(Map.of("registered", name));
    }

    @DeleteMapping("/{name}")
    public AjaxResult unregister(@PathVariable("name") String name)
    {
        registry.unregister(name);
        return AjaxResult.success(Map.of("unregistered", name));
    }

    private static String str(Object o)
    {
        return o == null ? "" : String.valueOf(o);
    }
}

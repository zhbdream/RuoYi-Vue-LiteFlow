package com.ruoyiliteflow.mcp.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.mcp.config.McpServerProperties;
import com.ruoyiliteflow.mcp.tool.AiCoreMcpTools;
import com.ruoyiliteflow.mcp.tool.GovernanceMcpTools;
import com.ruoyiliteflow.mcp.tool.McpToolRegistry;

/**
 * HTTP/SSE 形态的 MCP 能力面
 */
@RestController
@RequestMapping("/mcp")
public class McpAiCoreController
{
    private final AiCoreMcpTools aiCoreMcpTools;
    private final GovernanceMcpTools governanceMcpTools;
    private final McpToolRegistry toolRegistry;
    private final McpServerProperties properties;

    public McpAiCoreController(AiCoreMcpTools aiCoreMcpTools, GovernanceMcpTools governanceMcpTools,
            McpToolRegistry toolRegistry, McpServerProperties properties)
    {
        this.aiCoreMcpTools = aiCoreMcpTools;
        this.governanceMcpTools = governanceMcpTools;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

    @GetMapping("/info")
    public AjaxResult info()
    {
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", properties.isEnabled());
        data.put("transport", properties.getTransport());
        data.put("headerName", properties.getAuth().getHeaderName());
        Map<String, Object> servers = new HashMap<>();
        servers.put("ai-core", properties.getServers().isAiCore());
        servers.put("lf-governance", properties.getServers().isLfGovernance());
        servers.put("lf-runtime", properties.getServers().isLfRuntime());
        servers.put("sys", properties.getServers().isSys());
        servers.put("liteflow", true);
        data.put("servers", servers);
        data.put("toolsPath", "GET /mcp/tools 或 /mcp/{server}/tools");
        data.put("callPath", "POST /mcp/{server}/tools/{toolName}");
        data.put("dynamicToolsPath", "POST|DELETE /mcp/dynamic-tools");
        data.put("ssePath", "GET /mcp/ai-core/sse");
        data.put("playground", "GET /  （浏览器调试页）");
        return AjaxResult.success(data);
    }

    @GetMapping("/tools")
    public AjaxResult listAllTools()
    {
        return AjaxResult.success(toolRegistry.listAllTools());
    }

    @GetMapping("/ai-core/tools")
    public AjaxResult listAiCoreTools()
    {
        ensureAiCore();
        return AjaxResult.success(toolRegistry.listAiCoreTools());
    }

    @PostMapping("/ai-core/tools/{toolName}")
    public AjaxResult callAiCore(@PathVariable String toolName, @RequestBody(required = false) JSONObject body)
    {
        ensureAiCore();
        return AjaxResult.success(toolRegistry.callAiCore(toolName, body));
    }

    @GetMapping("/lf-governance/tools")
    public AjaxResult listGovernanceTools()
    {
        ensureGovernance();
        return AjaxResult.success(governanceMcpTools.listTools());
    }

    @PostMapping("/lf-governance/tools/{toolName}")
    public AjaxResult callGovernance(@PathVariable String toolName, @RequestBody(required = false) JSONObject body)
    {
        ensureGovernance();
        return AjaxResult.success(governanceMcpTools.call(toolName, body));
    }

    @GetMapping("/liteflow/tools")
    public AjaxResult listLiteflowTools()
    {
        return AjaxResult.success(toolRegistry.listByServer("liteflow"));
    }

    @PostMapping("/liteflow/tools/{toolName}")
    public AjaxResult callLiteflow(@PathVariable String toolName, @RequestBody(required = false) JSONObject body)
    {
        return AjaxResult.success(toolRegistry.callDynamic(toolName, body));
    }

    @GetMapping(value = "/ai-core/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse()
    {
        ensureAiCore();
        SseEmitter emitter = new SseEmitter(0L);
        try
        {
            emitter.send(SseEmitter.event().name("tools").data(toolRegistry.listAiCoreTools()));
            emitter.send(SseEmitter.event().name("ready").data(Map.of("server", "ai-core", "ok", true)));
        }
        catch (Exception e)
        {
            emitter.completeWithError(e);
            return emitter;
        }
        return emitter;
    }

    private void ensureAiCore()
    {
        if (!aiCoreMcpTools.enabled())
        {
            throw new ServiceException("mcp-ai-core 未启用");
        }
    }

    private void ensureGovernance()
    {
        if (!governanceMcpTools.enabled())
        {
            throw new ServiceException("mcp-lf-governance 未启用");
        }
    }
}

package com.ruoyiliteflow.aikit.platform.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 将 ai_tool(mcp) 变更推送到独立 MCP 进程的内存动态注册表。
 */
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.mcp", name = "register-url")
public class McpDynamicToolSyncClient
{
    private static final Logger log = LoggerFactory.getLogger(McpDynamicToolSyncClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ruoyi.ai-kit.mcp.register-url}")
    private String registerUrl;

    @Value("${ruoyi.ai-kit.mcp.api-key:}")
    private String apiKey;

    @Value("${ruoyi.ai-kit.mcp.api-key-header:X-MCP-Api-Key}")
    private String apiKeyHeader;

    public void register(AiTool tool)
    {
        if (tool == null || !"1".equals(tool.getEnabled()) || !isMcpSyncable(tool))
        {
            return;
        }
        if (StringUtils.isEmpty(registerUrl))
        {
            return;
        }
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", tool.getToolCode());
            body.put("description", StringUtils.isEmpty(tool.getDescription()) ? tool.getToolName() : tool.getDescription());
            body.put("server", resolveServer(tool));
            body.put("invokeKey", tool.getInvokeKey());
            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.postForEntity(trimSlash(registerUrl),
                    new HttpEntity<>(body, headers), String.class);
            log.info("MCP dynamic register {}: {}", tool.getToolCode(), resp.getStatusCode());
        }
        catch (Exception ex)
        {
            log.warn("MCP dynamic register failed for {}: {}", tool.getToolCode(), ex.getMessage());
        }
    }

    public void unregister(String toolCode)
    {
        if (StringUtils.isEmpty(registerUrl) || StringUtils.isEmpty(toolCode))
        {
            return;
        }
        try
        {
            String url = trimSlash(registerUrl) + "/" + toolCode;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(authHeaders()), String.class);
            log.info("MCP dynamic unregister {}", toolCode);
        }
        catch (Exception ex)
        {
            log.warn("MCP dynamic unregister failed for {}: {}", toolCode, ex.getMessage());
        }
    }

    private static boolean isMcpSyncable(AiTool tool)
    {
        if ("mcp".equalsIgnoreCase(tool.getToolType()))
        {
            return true;
        }
        return "liteflow-chain".equalsIgnoreCase(tool.getToolType())
                && StringUtils.isNotEmpty(tool.getMcpServerKey());
    }

    private static String resolveServer(AiTool tool)
    {
        if ("liteflow-chain".equalsIgnoreCase(tool.getToolType()))
        {
            return "liteflow";
        }
        return StringUtils.isEmpty(tool.getMcpServerKey()) ? "ai-core" : tool.getMcpServerKey();
    }

    private HttpHeaders authHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotEmpty(apiKey))
        {
            headers.set(apiKeyHeader, apiKey);
        }
        return headers;
    }

    private static String trimSlash(String url)
    {
        if (url.endsWith("/"))
        {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

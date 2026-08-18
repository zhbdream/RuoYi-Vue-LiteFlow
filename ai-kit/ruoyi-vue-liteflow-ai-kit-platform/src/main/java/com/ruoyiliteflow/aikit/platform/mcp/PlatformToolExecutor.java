package com.ruoyiliteflow.aikit.platform.mcp;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.ruoyiliteflow.aicore.spi.DefaultToolExecutor;
import com.ruoyiliteflow.aicore.spi.LocalToolHandler;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.aicore.spi.ToolExecutor;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 优先走宿主 {@link LocalToolHandler}；mcp 类型再打独立 MCP HTTP；其余委托 {@link DefaultToolExecutor}。
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformToolExecutor implements ToolExecutor
{
    private static final Logger log = LoggerFactory.getLogger(PlatformToolExecutor.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private DefaultToolExecutor defaultToolExecutor;

    @Autowired(required = false)
    private List<LocalToolHandler> localHandlers;

    @Value("${ruoyi.ai-kit.mcp.base-url:http://127.0.0.1:8090}")
    private String mcpBaseUrl;

    @Value("${ruoyi.ai-kit.mcp.api-key:}")
    private String apiKey;

    @Value("${ruoyi.ai-kit.mcp.api-key-header:X-MCP-Api-Key}")
    private String apiKeyHeader;

    @Override
    public String execute(ToolDescriptor tool, String argumentsJson)
    {
        if (localHandlers != null)
        {
            for (LocalToolHandler handler : localHandlers)
            {
                if (handler != null && handler.supports(tool))
                {
                    return handler.execute(tool, argumentsJson);
                }
            }
        }
        if (tool != null && "mcp".equalsIgnoreCase(tool.getToolType())
                && StringUtils.isNotEmpty(tool.getInvokeKey())
                && !"echo".equalsIgnoreCase(tool.getInvokeKey())
                && !tool.getInvokeKey().startsWith("http://")
                && !tool.getInvokeKey().startsWith("https://"))
        {
            return callMcp(tool, argumentsJson);
        }
        return defaultToolExecutor.execute(tool, argumentsJson);
    }

    private String callMcp(ToolDescriptor tool, String argumentsJson)
    {
        String server = StringUtils.isEmpty(tool.getMcpServerKey()) ? "ai-core" : tool.getMcpServerKey();
        String name = StringUtils.isEmpty(tool.getInvokeKey()) ? tool.getToolCode() : tool.getInvokeKey();
        String url = trimSlash(mcpBaseUrl) + "/mcp/" + server + "/tools/" + name;
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.isNotEmpty(apiKey))
            {
                headers.set(apiKeyHeader, apiKey);
            }
            String body = StringUtils.isEmpty(argumentsJson) ? "{}" : argumentsJson;
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return resp.getBody() == null ? "" : resp.getBody();
        }
        catch (Exception ex)
        {
            log.warn("MCP tool call failed {} {}: {}", url, name, ex.getMessage());
            return "{\"ok\":false,\"tool\":\"" + tool.getToolCode() + "\",\"error\":\""
                    + escape(ex.getMessage()) + "\"}";
        }
    }

    private static String trimSlash(String url)
    {
        if (url != null && url.endsWith("/"))
        {
            return url.substring(0, url.length() - 1);
        }
        return url == null ? "" : url;
    }

    private static String escape(String s)
    {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

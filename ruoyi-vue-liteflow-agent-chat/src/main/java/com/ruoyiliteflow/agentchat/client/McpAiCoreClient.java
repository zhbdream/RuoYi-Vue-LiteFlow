package com.ruoyiliteflow.agentchat.client;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.agentchat.config.AgentChatProperties;
import com.ruoyiliteflow.common.exception.ServiceException;

/**
 * 通过 HTTP 调用系统 MCP Server（mcp-ai-core）
 */
@Component
public class McpAiCoreClient
{
    private final AgentChatProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public McpAiCoreClient(AgentChatProperties properties)
    {
        this.properties = properties;
    }

    public List<Map> listTools()
    {
        JSONObject body = exchange(HttpMethod.GET, "/mcp/ai-core/tools", null);
        return body.getList("data", Map.class);
    }

    public JSONObject callTool(String toolName, Map<String, Object> arguments)
    {
        JSONObject body = exchange(HttpMethod.POST, "/mcp/ai-core/tools/" + toolName, arguments);
        return body.getJSONObject("data");
    }

    private JSONObject exchange(HttpMethod method, String path, Object body)
    {
        String url = trimSlash(properties.getMcpBaseUrl()) + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(properties.getMcpHeaderName(), properties.getMcpApiKey());
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, method, entity, String.class);
        JSONObject json = JSON.parseObject(resp.getBody());
        if (json == null)
        {
            throw new ServiceException("MCP 响应为空");
        }
        Integer code = json.getInteger("code");
        if (code != null && code != 200)
        {
            throw new ServiceException("MCP 调用失败: " + json.getString("msg"));
        }
        return json;
    }

    private static String trimSlash(String base)
    {
        if (base == null || base.isEmpty())
        {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}

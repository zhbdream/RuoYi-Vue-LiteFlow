package com.ruoyiliteflow.aikit.boot.client;

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
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.aikit.boot.config.AiKitBootSecurityConfig.AiKitBootProperties;
import com.ruoyiliteflow.common.exception.ServiceException;

/**
 * 统一 MCP HTTP Client（ai-core / lf-governance）
 */
@Component
public class McpToolClient
{
    private final AiKitBootProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public McpToolClient(AiKitBootProperties properties)
    {
        this.properties = properties;
    }

    public List<Map> listAiCoreTools()
    {
        JSONObject body = exchange(HttpMethod.GET, "/mcp/ai-core/tools", null);
        return body.getList("data", Map.class);
    }

    public JSONObject callAiCore(String toolName, Map<String, ?> args)
    {
        Object data = exchange(HttpMethod.POST, "/mcp/ai-core/tools/" + toolName, args).get("data");
        return data instanceof JSONObject ? (JSONObject) data : JSON.parseObject(JSON.toJSONString(data));
    }

    public JSONArray listChains()
    {
        Object data = exchange(HttpMethod.POST, "/mcp/lf-governance/tools/list_chains", Map.of()).get("data");
        return data instanceof JSONArray ? (JSONArray) data : JSONArray.parseArray(JSON.toJSONString(data));
    }

    public JSONObject dashboard()
    {
        Object data = exchange(HttpMethod.POST, "/mcp/lf-governance/tools/dashboard_summary", Map.of()).get("data");
        return data instanceof JSONObject ? (JSONObject) data : JSON.parseObject(JSON.toJSONString(data));
    }

    private JSONObject exchange(HttpMethod method, String path, Object body)
    {
        String url = trimSlash(properties.getMcpBaseUrl()) + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(properties.getMcpHeaderName(), properties.getMcpApiKey());
        ResponseEntity<String> resp = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
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

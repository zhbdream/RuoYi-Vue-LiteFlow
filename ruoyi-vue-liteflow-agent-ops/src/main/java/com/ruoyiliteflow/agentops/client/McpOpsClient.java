package com.ruoyiliteflow.agentops.client;

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
import com.ruoyiliteflow.agentops.config.AgentOpsSecurityConfig.AgentOpsProperties;
import com.ruoyiliteflow.common.exception.ServiceException;

@Component
public class McpOpsClient
{
    private final AgentOpsProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public McpOpsClient(AgentOpsProperties properties)
    {
        this.properties = properties;
    }

    public JSONArray listChains()
    {
        Object data = callGetData("lf-governance", "list_chains", Map.of());
        return data instanceof JSONArray ? (JSONArray) data : JSONArray.parseArray(JSON.toJSONString(data));
    }

    public JSONObject dashboard()
    {
        Object data = callGetData("lf-governance", "dashboard_summary", Map.of());
        return data instanceof JSONObject ? (JSONObject) data : JSON.parseObject(JSON.toJSONString(data));
    }

    public JSONObject chatCompletion(String systemPrompt, String userMessage)
    {
        Object data = callGetData("ai-core", "chat_completion", Map.of(
                "systemPrompt", systemPrompt,
                "userMessage", userMessage,
                "principal", "agent-ops"));
        return data instanceof JSONObject ? (JSONObject) data : JSON.parseObject(JSON.toJSONString(data));
    }

    private Object callGetData(String server, String tool, Map<String, ?> args)
    {
        String base = properties.getMcpBaseUrl();
        if (base != null && base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/mcp/" + server + "/tools/" + tool;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(properties.getMcpHeaderName(), properties.getMcpApiKey());
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(args, headers), String.class);
        JSONObject json = JSON.parseObject(resp.getBody());
        if (json == null || (json.getInteger("code") != null && json.getInteger("code") != 200))
        {
            throw new ServiceException("MCP 调用失败: " + (json == null ? "empty" : json.getString("msg")));
        }
        return json.get("data");
    }
}

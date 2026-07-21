package com.ruoyiliteflow.agentrisk.client;

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
import com.ruoyiliteflow.agentrisk.config.AgentRiskSecurityConfig.AgentRiskProperties;
import com.ruoyiliteflow.common.exception.ServiceException;

@Component
public class McpRiskClient
{
    private final AgentRiskProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public McpRiskClient(AgentRiskProperties properties)
    {
        this.properties = properties;
    }

    public JSONObject riskAnalyze(Map<String, Object> args)
    {
        return call("ai-core", "risk_analyze", args);
    }

    private JSONObject call(String server, String tool, Map<String, Object> args)
    {
        String url = trim(properties.getMcpBaseUrl()) + "/mcp/" + server + "/tools/" + tool;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(properties.getMcpHeaderName(), properties.getMcpApiKey());
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(args, headers), String.class);
        JSONObject json = JSON.parseObject(resp.getBody());
        if (json == null || (json.getInteger("code") != null && json.getInteger("code") != 200))
        {
            throw new ServiceException("MCP 调用失败: " + (json == null ? "empty" : json.getString("msg")));
        }
        return json.getJSONObject("data");
    }

    private static String trim(String base)
    {
        return base != null && base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}

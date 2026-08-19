package com.ruoyiliteflow.mcp.tool;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * MCP 侧通过开放 API 执行已发布链路（含 Agent 的链路由开放 API 默认拦截）。
 */
@Component
public class LiteFlowOpenExecuteClient
{
    private static final Logger log = LoggerFactory.getLogger(LiteFlowOpenExecuteClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ruoyi.mcp.liteflow-open.base-url:http://127.0.0.1:8080}")
    private String baseUrl;

    @Value("${ruoyi.mcp.liteflow-open.api-key:ruoyi-liteflow-open-key-change-me}")
    private String apiKey;

    @Value("${ruoyi.mcp.liteflow-open.header-name:X-LiteFlow-Api-Key}")
    private String headerName;

    public Map<String, Object> execute(String chainName, JSONObject args)
    {
        Map<String, Object> wrap = new LinkedHashMap<>();
        wrap.put("source", "mcp-open-api");
        wrap.put("chainName", chainName);
        if (StringUtils.isEmpty(baseUrl))
        {
            wrap.put("ok", false);
            wrap.put("error", "未配置 ruoyi.mcp.liteflow-open.base-url");
            return wrap;
        }
        if (StringUtils.isEmpty(chainName))
        {
            wrap.put("ok", false);
            wrap.put("error", "缺少链路名");
            return wrap;
        }
        URI uri = UriComponentsBuilder.fromUriString(trimSlash(baseUrl))
                .path("/liteflow/open/execute/{chainName}")
                .buildAndExpand(chainName)
                .encode()
                .toUri();
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.isNotEmpty(apiKey))
            {
                headers.set(headerName, apiKey);
            }
            String body = args == null ? "{}" : args.toJSONString();
            ResponseEntity<String> resp = restTemplate.postForEntity(uri, new HttpEntity<>(body, headers), String.class);
            JSONObject json = StringUtils.isEmpty(resp.getBody()) ? new JSONObject() : JSON.parseObject(resp.getBody());
            Integer code = json.getInteger("code");
            if (code != null && code != 200)
            {
                wrap.put("ok", false);
                wrap.put("error", json.getString("msg"));
                return wrap;
            }
            Object data = json.get("data");
            wrap.put("ok", true);
            wrap.put("data", data);
            if (data instanceof JSONObject result)
            {
                wrap.put("ok", Boolean.TRUE.equals(result.getBoolean("success")));
                wrap.put("executeStepStr", result.getString("executeStepStr"));
                wrap.put("requestId", result.getString("requestId"));
                wrap.put("message", result.getString("message"));
            }
            return wrap;
        }
        catch (Exception ex)
        {
            log.warn("liteflow open execute failed {}: {}", chainName, ex.getMessage());
            wrap.put("ok", false);
            wrap.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return wrap;
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
}

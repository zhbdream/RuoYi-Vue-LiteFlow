package com.ruoyiliteflow.liteflow.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.config.LiteFlowWebhookProperties;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.service.ILiteFlowWebhookService;

@Service
public class LiteFlowWebhookServiceImpl implements ILiteFlowWebhookService
{
    private static final Logger log = LoggerFactory.getLogger(LiteFlowWebhookServiceImpl.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "liteflow-webhook");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    private LiteFlowWebhookProperties webhookProperties;

    @Autowired
    private LfChainMapper lfChainMapper;

    @Override
    public void notifyAsync(String chainName, Object param, LiteFlowExecuteResultVo result, long durationMs, String createBy)
    {
        if (result == null || StringUtils.isEmpty(chainName) || "(EL调试)".equals(chainName))
        {
            return;
        }
        String targetUrl = resolveUrl(chainName);
        if (StringUtils.isEmpty(targetUrl))
        {
            return;
        }
        if (webhookProperties.isOnlyOnFailure() && result.isSuccess())
        {
            return;
        }
        Map<String, Object> payload = new HashMap<>(12);
        payload.put("event", "liteflow.execute");
        payload.put("chainName", chainName);
        payload.put("requestId", result.getRequestId());
        payload.put("success", result.isSuccess());
        payload.put("code", result.getCode());
        payload.put("message", result.getMessage());
        payload.put("durationMs", durationMs);
        payload.put("executeStepStr", result.getExecuteStepStr());
        payload.put("failedNodeId", result.getFailedNodeId());
        payload.put("createBy", createBy);
        payload.put("logId", result.getLogId());
        if (param != null)
        {
            payload.put("param", param);
        }
        String body = JSON.toJSONString(payload);
        executor.execute(() -> postJson(targetUrl, body, chainName, result.getRequestId()));
    }

    private String resolveUrl(String chainName)
    {
        try
        {
            LfChain chain = lfChainMapper.selectLfChainByName(chainName);
            if (chain != null && StringUtils.isNotEmpty(chain.getWebhookUrl()))
            {
                return chain.getWebhookUrl().trim();
            }
        }
        catch (Exception e)
        {
            log.debug("读取链路 webhookUrl 失败: {}", e.getMessage());
        }
        if (webhookProperties.isEnabled() && StringUtils.isNotEmpty(webhookProperties.getUrl()))
        {
            return webhookProperties.getUrl().trim();
        }
        return null;
    }

    private void postJson(String url, String body, String chainName, String requestId)
    {
        try
        {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(500, webhookProperties.getConnectTimeoutMs())))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(500, webhookProperties.getReadTimeoutMs())))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("X-LiteFlow-Event", "execute")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300)
            {
                log.info("Webhook 回调成功: chain={}, requestId={}, status={}", chainName, requestId, response.statusCode());
            }
            else
            {
                log.warn("Webhook 回调非 2xx: chain={}, requestId={}, status={}, body={}",
                        chainName, requestId, response.statusCode(), truncate(response.body()));
            }
        }
        catch (Exception e)
        {
            log.warn("Webhook 回调失败: chain={}, requestId={}, err={}", chainName, requestId, e.getMessage());
        }
    }

    private String truncate(String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    @PreDestroy
    public void shutdown()
    {
        executor.shutdownNow();
    }
}

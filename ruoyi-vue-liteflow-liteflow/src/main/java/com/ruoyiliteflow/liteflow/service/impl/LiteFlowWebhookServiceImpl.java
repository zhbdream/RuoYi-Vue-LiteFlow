package com.ruoyiliteflow.liteflow.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.config.LiteFlowWebhookProperties;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.service.ILfExecLogService;
import com.ruoyiliteflow.liteflow.service.ILiteFlowWebhookService;

@Service
public class LiteFlowWebhookServiceImpl implements ILiteFlowWebhookService
{
    private static final Logger log = LoggerFactory.getLogger(LiteFlowWebhookServiceImpl.class);

    public static final String STATUS_PENDING = "0";
    public static final String STATUS_SUCCESS = "1";
    public static final String STATUS_FAILED = "2";
    public static final String STATUS_SKIPPED = "3";

    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "liteflow-webhook");
        t.setDaemon(true);
        return t;
    });

    private HttpClient httpClient;

    @Autowired
    private LiteFlowWebhookProperties webhookProperties;

    @Autowired
    private LfChainMapper lfChainMapper;

    @Autowired
    private ILfExecLogService lfExecLogService;

    @PostConstruct
    public void initClient()
    {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, webhookProperties.getConnectTimeoutMs())))
                .build();
    }

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
            persist(result.getLogId(), targetUrl, STATUS_SKIPPED, 0, null, "仅失败回调，本次成功已跳过");
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
        Long logId = result.getLogId();
        String requestId = result.getRequestId();
        persist(logId, targetUrl, STATUS_PENDING, 0, null, "投递中");
        executor.execute(() -> deliver(targetUrl, body, chainName, requestId, logId));
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

    private void deliver(String url, String body, String chainName, String requestId, Long logId)
    {
        int maxAttempts = Math.min(8, Math.max(1, webhookProperties.getMaxAttempts()));
        int backoffMs = Math.min(10_000, Math.max(200, webhookProperties.getRetryBackoffMs()));
        Integer lastHttp = null;
        String lastMsg = "投递中";
        for (int attempt = 1; attempt <= maxAttempts; attempt++)
        {
            persist(logId, url, STATUS_PENDING, attempt, lastHttp, lastMsg);
            try
            {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMillis(Math.max(500, webhookProperties.getReadTimeoutMs())))
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("X-LiteFlow-Event", "execute");
                String signature = sign(body);
                if (signature != null)
                {
                    builder.header("X-LiteFlow-Signature", signature);
                }
                HttpResponse<String> response = httpClient.send(
                        builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                        HttpResponse.BodyHandlers.ofString());
                lastHttp = response.statusCode();
                if (lastHttp >= 200 && lastHttp < 300)
                {
                    persist(logId, url, STATUS_SUCCESS, attempt, lastHttp, "成功");
                    log.info("Webhook 回调成功: chain={}, requestId={}, status={}, attempt={}",
                            chainName, requestId, lastHttp, attempt);
                    return;
                }
                lastMsg = "HTTP " + lastHttp + " " + truncate(response.body());
                log.warn("Webhook 回调非 2xx: chain={}, requestId={}, status={}, attempt={}, body={}",
                        chainName, requestId, lastHttp, attempt, truncate(response.body()));
                if (!shouldRetry(lastHttp) || attempt == maxAttempts)
                {
                    persist(logId, url, STATUS_FAILED, attempt, lastHttp, lastMsg);
                    return;
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                persist(logId, url, STATUS_FAILED, attempt, lastHttp, "投递中断");
                return;
            }
            catch (Exception e)
            {
                lastMsg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                log.warn("Webhook 回调失败: chain={}, requestId={}, attempt={}, err={}",
                        chainName, requestId, attempt, lastMsg);
                if (attempt == maxAttempts)
                {
                    persist(logId, url, STATUS_FAILED, attempt, lastHttp, lastMsg);
                    return;
                }
            }
            sleepBackoff(backoffMs, attempt);
            if (Thread.currentThread().isInterrupted())
            {
                persist(logId, url, STATUS_FAILED, attempt, lastHttp, "投递中断");
                return;
            }
        }
    }

    private boolean shouldRetry(Integer httpStatus)
    {
        if (httpStatus == null)
        {
            return true;
        }
        return httpStatus == 408 || httpStatus == 429 || httpStatus >= 500;
    }

    private void sleepBackoff(int backoffMs, int attempt)
    {
        long wait = (long) backoffMs * (1L << Math.min(attempt - 1, 6));
        try
        {
            Thread.sleep(wait);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private String sign(String body)
    {
        String secret = webhookProperties.getSecret();
        if (StringUtils.isEmpty(secret))
        {
            return null;
        }
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash)
            {
                hex.append(String.format("%02x", b));
            }
            return "sha256=" + hex;
        }
        catch (Exception e)
        {
            log.warn("Webhook 签名失败: {}", e.getMessage());
            return null;
        }
    }

    private void persist(Long logId, String url, String status, int attempts, Integer httpStatus, String message)
    {
        if (logId == null)
        {
            return;
        }
        try
        {
            LfExecLog patch = new LfExecLog();
            patch.setId(logId);
            patch.setWebhookUrl(url);
            patch.setWebhookStatus(status);
            patch.setWebhookAttempts(attempts);
            patch.setWebhookHttpStatus(httpStatus);
            patch.setWebhookMessage(truncate(message, 500));
            lfExecLogService.updateWebhook(patch);
        }
        catch (Exception e)
        {
            log.debug("写入 Webhook 投递状态失败: {}", e.getMessage());
        }
    }

    private String truncate(String s)
    {
        return truncate(s, 200);
    }

    private String truncate(String s, int max)
    {
        if (s == null)
        {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    @PreDestroy
    public void shutdown()
    {
        executor.shutdownNow();
    }
}

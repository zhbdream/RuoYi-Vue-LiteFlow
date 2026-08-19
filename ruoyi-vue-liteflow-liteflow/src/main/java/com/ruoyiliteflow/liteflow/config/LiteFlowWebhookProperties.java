package com.ruoyiliteflow.liteflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 执行完成后 HTTP Webhook 回调配置
 */
@Component
@ConfigurationProperties(prefix = "liteflow.webhook")
public class LiteFlowWebhookProperties
{
    /** 是否启用全局 Webhook */
    private boolean enabled = false;

    /** 全局回调地址；链路级 webhookUrl 优先 */
    private String url = "";

    private int connectTimeoutMs = 3000;

    private int readTimeoutMs = 5000;

    /** 仅失败时回调 */
    private boolean onlyOnFailure = false;

    /** HMAC-SHA256 密钥；空则不签名 */
    private String secret = "";

    /** 含首次在内的最大尝试次数 */
    private int maxAttempts = 3;

    /** 首次重试等待毫秒，之后按 2 倍退避 */
    private int retryBackoffMs = 1000;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public int getConnectTimeoutMs()
    {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs)
    {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs()
    {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs)
    {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isOnlyOnFailure()
    {
        return onlyOnFailure;
    }

    public void setOnlyOnFailure(boolean onlyOnFailure)
    {
        this.onlyOnFailure = onlyOnFailure;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    public int getMaxAttempts()
    {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts)
    {
        this.maxAttempts = maxAttempts;
    }

    public int getRetryBackoffMs()
    {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(int retryBackoffMs)
    {
        this.retryBackoffMs = retryBackoffMs;
    }
}

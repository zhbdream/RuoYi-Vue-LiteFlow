package com.ruoyiliteflow.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LiteFlow 开放执行 API 配置
 */
@Component
@ConfigurationProperties(prefix = "liteflow.open-api")
public class LiteFlowOpenApiProperties
{
    /** 是否启用开放 API */
    private boolean enabled = true;

    /** 外部系统调用的 API Key（请生产环境修改） */
    private String apiKey = "ruoyi-liteflow-open-key-change-me";

    /** API Key 请求头名称 */
    private String headerName = "X-LiteFlow-Api-Key";

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getHeaderName()
    {
        return headerName;
    }

    public void setHeaderName(String headerName)
    {
        this.headerName = headerName;
    }
}

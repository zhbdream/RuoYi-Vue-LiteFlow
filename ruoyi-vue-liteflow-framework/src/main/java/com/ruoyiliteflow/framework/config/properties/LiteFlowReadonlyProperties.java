package com.ruoyiliteflow.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LiteFlow 生产只读策略：开启后禁止规则/脚本等写操作，执行与查询仍可用。
 */
@Component
@ConfigurationProperties(prefix = "liteflow.readonly")
public class LiteFlowReadonlyProperties
{
    /** 是否开启只读（生产环境建议 true） */
    private boolean enabled = false;

    /** 拒绝写操作时的提示文案 */
    private String message = "当前环境为只读模式，禁止修改规则/脚本";

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}

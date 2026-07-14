package com.ruoyiliteflow.liteflow.service;

import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;

/**
 * 执行完成后的 Webhook 回调
 */
public interface ILiteFlowWebhookService
{
    /**
     * 异步推送执行结果；失败不影响主流程。
     */
    void notifyAsync(String chainName, Object param, LiteFlowExecuteResultVo result, long durationMs, String createBy);
}

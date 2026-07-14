package com.ruoyiliteflow.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

/**
 * 定时执行 LiteFlow 链路。
 * <p>调用示例（系统监控 → 定时任务）：
 * <ul>
 *   <li>{@code liteFlowTask.executeByName('helloChain')}</li>
 *   <li>{@code liteFlowTask.executeByName('orderProcess', '{"userId":1001,"skuId":"SKU-001","quantity":2,"payType":"wechat"}')}</li>
 * </ul>
 */
@Component("liteFlowTask")
public class LiteFlowTask
{
    private static final Logger log = LoggerFactory.getLogger(LiteFlowTask.class);

    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    /**
     * 按链路 ID 执行（无参 / 空参）
     */
    public void executeByName(String chainName)
    {
        executeByName(chainName, null);
    }

    /**
     * 按链路 ID 执行，paramJson 为 JSON 对象字符串；为空则传 null。
     */
    public void executeByName(String chainName, String paramJson)
    {
        if (StringUtils.isEmpty(chainName))
        {
            log.warn("LiteFlow 定时任务跳过：chainName 为空");
            return;
        }
        Object param = null;
        if (StringUtils.isNotEmpty(paramJson))
        {
            param = JSON.parse(paramJson);
        }
        log.info("LiteFlow 定时执行开始: chainName={}", chainName);
        LiteFlowExecuteResultVo result = liteFlowExecuteService.execute(chainName, param, "quartz", true);
        if (result == null)
        {
            log.warn("LiteFlow 定时执行无结果: chainName={}", chainName);
            return;
        }
        if (result.isSuccess())
        {
            log.info("LiteFlow 定时执行成功: chainName={}, requestId={}, duration相关见执行日志",
                    chainName, result.getRequestId());
        }
        else
        {
            log.error("LiteFlow 定时执行失败: chainName={}, requestId={}, message={}",
                    chainName, result.getRequestId(), result.getMessage());
        }
    }
}

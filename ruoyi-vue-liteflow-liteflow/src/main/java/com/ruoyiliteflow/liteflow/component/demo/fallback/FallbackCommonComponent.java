package com.ruoyiliteflow.liteflow.component.demo.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.yomahub.liteflow.annotation.FallbackCmp;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * 普通组件降级 Demo：当 EL 中 {@code node("不存在的id")} 找不到组件时自动执行本组件。
 * <p>需开启 {@code liteflow.fallback-cmp-enable=true}，且缺失节点必须用 {@code node("...")} 写法。
 */
@LiteflowComponent(value = "fallbackCommon", name = "普通降级组件")
@FallbackCmp
@Component
public class FallbackCommonComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(FallbackCommonComponent.class);

    @Override
    public void process()
    {
        log.info("fallbackCommon executed (missing node degraded), request={}", (Object) getRequestData());
    }
}

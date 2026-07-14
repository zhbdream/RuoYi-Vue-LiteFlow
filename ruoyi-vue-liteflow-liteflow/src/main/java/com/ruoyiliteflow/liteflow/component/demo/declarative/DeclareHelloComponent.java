package com.ruoyiliteflow.liteflow.component.demo.declarative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;

/**
 * 声明式组件 Demo：不继承 NodeComponent，用 @LiteflowMethod 定义逻辑。
 */
@LiteflowComponent(value = "declareHello", name = "声明式问好")
@Component
public class DeclareHelloComponent
{
    private static final Logger log = LoggerFactory.getLogger(DeclareHelloComponent.class);

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON)
    public void process(NodeComponent bindCmp)
    {
        log.info("declareHello executed, request={}", (Object) bindCmp.getRequestData());
    }
}

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
 * 声明式组件 Demo（收尾节点）
 */
@LiteflowComponent(value = "declareBye", name = "声明式收尾")
@Component
public class DeclareByeComponent
{
    private static final Logger log = LoggerFactory.getLogger(DeclareByeComponent.class);

    @LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON)
    public void process(NodeComponent bindCmp)
    {
        log.info("declareBye executed");
    }
}

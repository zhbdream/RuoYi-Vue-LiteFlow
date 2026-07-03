package com.ruoyiliteflow.liteflow.component.demo.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("helloA")
@Component
public class HelloAComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(HelloAComponent.class);

    @Override
    public void process()
    {
        log.info("helloA executed, request={}", (Object) getRequestData());
    }
}

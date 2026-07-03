package com.ruoyiliteflow.liteflow.component.demo.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("helloB")
@Component
public class HelloBComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(HelloBComponent.class);

    @Override
    public void process()
    {
        log.info("helloB executed");
    }
}

package com.ruoyiliteflow.liteflow.component.demo.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.RouteUserContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("newCustomerWelcome")
@Component
public class NewCustomerWelcomeComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(NewCustomerWelcomeComponent.class);

    @Override
    public void process()
    {
        RouteUserContext ctx = getContextBean(RouteUserContext.class);
        if (ctx != null)
        {
            ctx.setMessage("欢迎新客，首单专属礼遇已就绪");
        }
        log.info("newCustomerWelcome executed");
    }
}

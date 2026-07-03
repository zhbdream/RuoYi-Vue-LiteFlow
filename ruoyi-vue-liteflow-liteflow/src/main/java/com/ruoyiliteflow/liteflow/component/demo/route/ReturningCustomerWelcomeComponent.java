package com.ruoyiliteflow.liteflow.component.demo.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.RouteUserContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("returningCustomerWelcome")
@Component
public class ReturningCustomerWelcomeComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(ReturningCustomerWelcomeComponent.class);

    @Override
    public void process()
    {
        RouteUserContext ctx = getContextBean(RouteUserContext.class);
        if (ctx != null)
        {
            ctx.setMessage("欢迎回来，老客专属通道已开启");
        }
        log.info("returningCustomerWelcome executed");
    }
}

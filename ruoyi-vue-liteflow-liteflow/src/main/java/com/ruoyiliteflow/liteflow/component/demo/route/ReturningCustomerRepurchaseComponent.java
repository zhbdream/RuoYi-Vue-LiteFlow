package com.ruoyiliteflow.liteflow.component.demo.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.RouteUserContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("returningCustomerRepurchase")
@Component
public class ReturningCustomerRepurchaseComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(ReturningCustomerRepurchaseComponent.class);

    @Override
    public void process()
    {
        RouteUserContext ctx = getContextBean(RouteUserContext.class);
        if (ctx != null)
        {
            ctx.setMessage(ctx.getMessage() + "；复购满减活动已匹配");
        }
        log.info("returningCustomerRepurchase executed");
    }
}

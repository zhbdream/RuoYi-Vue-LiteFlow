package com.ruoyiliteflow.liteflow.component.demo.route;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.RouteUserContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("isReturningCustomer")
@Component
public class IsReturningCustomerComponent extends NodeBooleanComponent
{
    @Override
    public boolean processBoolean()
    {
        RouteUserContext ctx = getContextBean(RouteUserContext.class);
        return ctx != null && "RETURNING".equalsIgnoreCase(ctx.getUserType());
    }
}

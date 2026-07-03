package com.ruoyiliteflow.liteflow.component.demo.route;

import org.springframework.stereotype.Component;
import com.ruoyiliteflow.liteflow.domain.context.RouteUserContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;

@LiteflowComponent("isNewCustomer")
@Component
public class IsNewCustomerComponent extends NodeBooleanComponent
{
    @Override
    public boolean processBoolean()
    {
        RouteUserContext ctx = getContextBean(RouteUserContext.class);
        return ctx != null && "NEW".equalsIgnoreCase(ctx.getUserType());
    }
}

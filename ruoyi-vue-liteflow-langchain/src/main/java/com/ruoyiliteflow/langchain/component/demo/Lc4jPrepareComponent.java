package com.ruoyiliteflow.langchain.component.demo;

import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

/**
 * Demo：整理订单风控入参到 {@link Lc4jRiskContext}
 */
@LiteflowComponent(value = "lc4jPrepare", name = "LC4j风控准备")
@Component
public class Lc4jPrepareComponent extends NodeComponent
{
    private static final Logger log = LoggerFactory.getLogger(Lc4jPrepareComponent.class);

    @Override
    public void process()
    {
        Lc4jRiskContext ctx = this.getContextBean(Lc4jRiskContext.class);
        Object req = this.getRequestData();
        Map<String, Object> map = toMap(req);
        ctx.setOrderId(str(map.get("orderId"), "ORD-LC4J-001"));
        ctx.setUserId(toLong(map.get("userId"), 2001L));
        ctx.setUserType(str(map.get("userType"), "NEW"));
        ctx.setAmount(toDecimal(map.get("amount"), new BigDecimal("2599.00")));
        ctx.setScene(str(map.get("scene"), "checkout"));
        ctx.setPrepared(true);
        log.info("lc4jPrepare done: orderId={}, amount={}, userType={}",
                ctx.getOrderId(), ctx.getAmount(), ctx.getUserType());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object req)
    {
        if (req == null)
        {
            return Map.of();
        }
        if (req instanceof Map)
        {
            return (Map<String, Object>) req;
        }
        return JSON.parseObject(JSON.toJSONString(req));
    }

    private String str(Object v, String def)
    {
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }

    private Long toLong(Object v, Long def)
    {
        if (v == null)
        {
            return def;
        }
        try
        {
            return Long.valueOf(String.valueOf(v));
        }
        catch (Exception e)
        {
            return def;
        }
    }

    private BigDecimal toDecimal(Object v, BigDecimal def)
    {
        if (v == null)
        {
            return def;
        }
        try
        {
            return new BigDecimal(String.valueOf(v));
        }
        catch (Exception e)
        {
            return def;
        }
    }
}
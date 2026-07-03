package com.ruoyiliteflow.liteflow.domain.context;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单 Demo 上下文
 */
public class OrderContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String skuId;

    private Integer quantity;

    private String payType;

    private String couponCode;

    private boolean hasStock = true;

    private BigDecimal originalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private String payChannel;

    private boolean success;

    private String message;

    private List<String> steps = new ArrayList<>();

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getSkuId()
    {
        return skuId;
    }

    public void setSkuId(String skuId)
    {
        this.skuId = skuId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public String getPayType()
    {
        return payType;
    }

    public void setPayType(String payType)
    {
        this.payType = payType;
    }

    public String getCouponCode()
    {
        return couponCode;
    }

    public void setCouponCode(String couponCode)
    {
        this.couponCode = couponCode;
    }

    public boolean isHasStock()
    {
        return hasStock;
    }

    public void setHasStock(boolean hasStock)
    {
        this.hasStock = hasStock;
    }

    public BigDecimal getOriginalAmount()
    {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount)
    {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getDiscountAmount()
    {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount)
    {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPayAmount()
    {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount)
    {
        this.payAmount = payAmount;
    }

    public String getPayChannel()
    {
        return payChannel;
    }

    public void setPayChannel(String payChannel)
    {
        this.payChannel = payChannel;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public List<String> getSteps()
    {
        return steps;
    }

    public void setSteps(List<String> steps)
    {
        this.steps = steps;
    }

    public void addStep(String step)
    {
        this.steps.add(step);
    }
}

package com.ruoyiliteflow.liteflow.domain.context;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PricingContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String memberLevel;
    private BigDecimal originalPrice = new BigDecimal("100.00");
    private BigDecimal reductionAmount = BigDecimal.ZERO;
    private BigDecimal couponAmount = BigDecimal.ZERO;
    private BigDecimal finalPrice;
    private String couponCode;
    private List<String> steps = new ArrayList<>();

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMemberLevel() { return memberLevel; }
    public void setMemberLevel(String memberLevel) { this.memberLevel = memberLevel; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public BigDecimal getReductionAmount() { return reductionAmount; }
    public void setReductionAmount(BigDecimal reductionAmount) { this.reductionAmount = reductionAmount; }
    public BigDecimal getCouponAmount() { return couponAmount; }
    public void setCouponAmount(BigDecimal couponAmount) { this.couponAmount = couponAmount; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void addStep(String step) { this.steps.add(step); }
}

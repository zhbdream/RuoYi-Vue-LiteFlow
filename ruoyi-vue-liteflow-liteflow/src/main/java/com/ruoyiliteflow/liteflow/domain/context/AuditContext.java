package com.ruoyiliteflow.liteflow.domain.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AuditContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String orderId;
    private boolean inventoryOk;
    private boolean creditOk;
    private boolean riskOk;
    private boolean auditPassed;
    private String message;
    private List<String> steps = new ArrayList<>();

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public boolean isInventoryOk() { return inventoryOk; }
    public void setInventoryOk(boolean inventoryOk) { this.inventoryOk = inventoryOk; }
    public boolean isCreditOk() { return creditOk; }
    public void setCreditOk(boolean creditOk) { this.creditOk = creditOk; }
    public boolean isRiskOk() { return riskOk; }
    public void setRiskOk(boolean riskOk) { this.riskOk = riskOk; }
    public boolean isAuditPassed() { return auditPassed; }
    public void setAuditPassed(boolean auditPassed) { this.auditPassed = auditPassed; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void addStep(String step) { this.steps.add(step); }
}

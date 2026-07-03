package com.ruoyiliteflow.liteflow.domain.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BatchContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String orderId;
    private List<Map<String, Object>> items = new ArrayList<>();
    private int batchCount;
    private int currentIndex;
    private int processedCount;
    private List<String> itemResults = new ArrayList<>();
    private String summary;
    private List<String> steps = new ArrayList<>();

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public int getBatchCount() { return batchCount; }
    public void setBatchCount(int batchCount) { this.batchCount = batchCount; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    public List<String> getItemResults() { return itemResults; }
    public void setItemResults(List<String> itemResults) { this.itemResults = itemResults; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void addStep(String step) { this.steps.add(step); }
}

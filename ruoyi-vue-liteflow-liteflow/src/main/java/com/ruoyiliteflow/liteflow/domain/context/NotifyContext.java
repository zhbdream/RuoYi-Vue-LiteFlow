package com.ruoyiliteflow.liteflow.domain.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotifyContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String userId;
    private String channel;
    private boolean simulateFail = true;
    private int attemptCount;
    private boolean notified;
    private boolean fallbackUsed;
    private String message;
    private List<String> steps = new ArrayList<>();

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public boolean isSimulateFail() { return simulateFail; }
    public void setSimulateFail(boolean simulateFail) { this.simulateFail = simulateFail; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void addStep(String step) { this.steps.add(step); }
}

package com.ruoyiliteflow.agentrisk.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.agentrisk.client.McpRiskClient;
import com.ruoyiliteflow.agentrisk.config.AgentRiskSecurityConfig.AgentRiskProperties;
import com.ruoyiliteflow.aicore.facade.IAiRiskFacade;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeRequest;
import com.ruoyiliteflow.common.core.domain.AjaxResult;

@RestController
@RequestMapping("/agent/risk")
public class AgentRiskController
{
    private final AgentRiskProperties properties;
    private final McpRiskClient mcpClient;
    private final IAiRiskFacade riskFacade;

    public AgentRiskController(AgentRiskProperties properties, McpRiskClient mcpClient, IAiRiskFacade riskFacade)
    {
        this.properties = properties;
        this.mcpClient = mcpClient;
        this.riskFacade = riskFacade;
    }

    @GetMapping("/health")
    public AjaxResult health()
    {
        return AjaxResult.success(Map.of("service", "agent-risk", "useMcp", properties.isUseMcp(),
                "mcpBaseUrl", properties.getMcpBaseUrl()));
    }

    @PostMapping("/analyze")
    public AjaxResult analyze(@RequestBody JSONObject body)
    {
        if (body.getString("principal") == null)
        {
            body.put("principal", "agent-risk");
        }
        if (properties.isUseMcp())
        {
            Map<String, Object> args = new HashMap<>(body);
            return AjaxResult.success(mcpClient.riskAnalyze(args));
        }
        RiskAnalyzeRequest req = body.to(RiskAnalyzeRequest.class);
        return AjaxResult.success(riskFacade.analyze(req));
    }
}

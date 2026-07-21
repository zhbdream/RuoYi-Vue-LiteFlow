package com.ruoyiliteflow.agentrag.web;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.agentrag.client.McpRagClient;
import com.ruoyiliteflow.agentrag.config.AgentRagSecurityConfig.AgentRagProperties;
import com.ruoyiliteflow.aicore.facade.IAiRagFacade;
import com.ruoyiliteflow.aicore.model.RagAskRequest;
import com.ruoyiliteflow.common.core.domain.AjaxResult;

@RestController
@RequestMapping("/agent/rag")
public class AgentRagController
{
    private final AgentRagProperties properties;
    private final McpRagClient mcpClient;
    private final IAiRagFacade ragFacade;

    public AgentRagController(AgentRagProperties properties, McpRagClient mcpClient, IAiRagFacade ragFacade)
    {
        this.properties = properties;
        this.mcpClient = mcpClient;
        this.ragFacade = ragFacade;
    }

    @GetMapping("/health")
    public AjaxResult health()
    {
        return AjaxResult.success(Map.of("service", "agent-rag", "useMcp", properties.isUseMcp(),
                "mcpBaseUrl", properties.getMcpBaseUrl()));
    }

    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody JSONObject body)
    {
        if (body.getString("principal") == null)
        {
            body.put("principal", "agent-rag");
        }
        if (properties.isUseMcp())
        {
            return AjaxResult.success(mcpClient.ragAsk(new HashMap<>(body)));
        }
        return AjaxResult.success(ragFacade.ask(body.to(RagAskRequest.class)));
    }
}

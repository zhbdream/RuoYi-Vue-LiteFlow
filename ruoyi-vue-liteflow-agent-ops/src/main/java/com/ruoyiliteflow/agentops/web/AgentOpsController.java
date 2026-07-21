package com.ruoyiliteflow.agentops.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.agentops.client.McpOpsClient;
import com.ruoyiliteflow.agentops.config.AgentOpsSecurityConfig.AgentOpsProperties;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/agent/ops")
public class AgentOpsController
{
    private final AgentOpsProperties properties;
    private final McpOpsClient mcpClient;

    public AgentOpsController(AgentOpsProperties properties, McpOpsClient mcpClient)
    {
        this.properties = properties;
        this.mcpClient = mcpClient;
    }

    @GetMapping("/health")
    public AjaxResult health()
    {
        return AjaxResult.success(Map.of("service", "agent-ops", "useMcp", properties.isUseMcp(),
                "mcpBaseUrl", properties.getMcpBaseUrl()));
    }

    /**
     * 运维助手：先拉取治理事实，再让 LLM 结合事实回答。
     */
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody JSONObject body)
    {
        String question = body.getString("userMessage");
        if (StringUtils.isEmpty(question))
        {
            question = body.getString("message");
        }
        JSONArray chains = mcpClient.listChains();
        JSONObject dashboard = mcpClient.dashboard();
        String facts = "【链路列表】\n" + JSON.toJSONString(chains)
                + "\n\n【监控摘要】\n" + JSON.toJSONString(dashboard);
        String userMessage = facts + "\n\n【用户问题】\n" + question;
        JSONObject reply = mcpClient.chatCompletion(properties.getSystemPrompt(), userMessage);
        return AjaxResult.success(Map.of(
                "facts", Map.of("chains", chains, "dashboard", dashboard),
                "reply", reply));
    }
}

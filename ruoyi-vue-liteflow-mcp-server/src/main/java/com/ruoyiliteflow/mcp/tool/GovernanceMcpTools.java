package com.ruoyiliteflow.mcp.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.mcp.config.McpServerProperties;

/**
 * mcp-lf-governance：链路治理只读 Tools。
 * <p>默认返回内置 Demo 数据，便于无 MySQL 演示；配置 adminBaseUrl 后可改为代理真实 admin（后续扩展）。
 */
@Component
public class GovernanceMcpTools
{
    public static final String SERVER = "lf-governance";

    private final McpServerProperties properties;

    public GovernanceMcpTools(McpServerProperties properties)
    {
        this.properties = properties;
    }

    public boolean enabled()
    {
        return properties.isEnabled() && properties.getServers().isLfGovernance();
    }

    public List<Map<String, Object>> listTools()
    {
        return List.of(
                tool("list_chains", "列出链路（Demo 数据或后续对接 admin）"),
                tool("get_chain", "按 chainId 获取链路摘要。参数: chainId"),
                tool("list_scripts", "列出脚本摘要"),
                tool("query_exec_logs", "查询最近执行日志。参数: chainId?, limit?"),
                tool("dashboard_summary", "监控摘要（成功率 / Top）"));
    }

    public Object call(String toolName, JSONObject args)
    {
        if (!enabled())
        {
            throw new ServiceException("mcp-lf-governance 未启用");
        }
        if (args == null)
        {
            args = new JSONObject();
        }
        return switch (toolName)
        {
            case "list_chains" -> demoChains();
            case "get_chain" -> demoGetChain(args.getString("chainId"));
            case "list_scripts" -> demoScripts();
            case "query_exec_logs" -> demoLogs(args.getString("chainId"), args.getIntValue("limit", 5));
            case "dashboard_summary" -> demoDashboard();
            default -> throw new ServiceException("未知 Tool: " + toolName);
        };
    }

    private List<Map<String, Object>> demoChains()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(chain("helloChain", "入门 THEN", "published", "THEN(a, b, c)"));
        list.add(chain("agentRiskDemo", "Re-Act 风控 Demo", "published", "THEN(agentPrepare, riskAgent, agentNotify)"));
        list.add(chain("lc4jRagDemo", "RAG 售后问答", "published", "THEN(lc4jRagPrepare, lc4jRag, lc4jRagNotify)"));
        return list;
    }

    private Map<String, Object> demoGetChain(String chainId)
    {
        for (Map<String, Object> c : demoChains())
        {
            if (String.valueOf(c.get("chainId")).equals(chainId))
            {
                return c;
            }
        }
        throw new ServiceException("链路不存在: " + chainId);
    }

    private List<Map<String, Object>> demoScripts()
    {
        return List.of(
                Map.of("scriptId", "priceScript", "name", "动态定价", "language", "groovy"),
                Map.of("scriptId", "auditScript", "name", "并行审计片段", "language", "qlexpress"));
    }

    private List<Map<String, Object>> demoLogs(String chainId, int limit)
    {
        List<Map<String, Object>> all = new ArrayList<>();
        all.add(Map.of("logId", 1001, "chainId", "helloChain", "success", true, "costMs", 12, "message", "OK"));
        all.add(Map.of("logId", 1002, "chainId", "agentRiskDemo", "success", true, "costMs", 1820, "message", "风险等级：HIGH"));
        all.add(Map.of("logId", 1003, "chainId", "lc4jRagDemo", "success", false, "costMs", 90, "message", "配额超限示例"));
        return all.stream()
                .filter(m -> chainId == null || chainId.isEmpty() || chainId.equals(String.valueOf(m.get("chainId"))))
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    private Map<String, Object> demoDashboard()
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("successRate", 0.96);
        m.put("todayCalls", 128);
        m.put("topChains", List.of("helloChain", "lc4jChatDemo", "agentRiskDemo"));
        m.put("mode", "demo-mock");
        m.put("note", "开源 Demo 内置数据；对接真实 admin 后可替换为查询 lf_* 表");
        return m;
    }

    private static Map<String, Object> chain(String id, String name, String status, String el)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("chainId", id);
        m.put("chainName", name);
        m.put("status", status);
        m.put("elSummary", el);
        return m;
    }

    private static Map<String, Object> tool(String name, String description)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("server", SERVER);
        return m;
    }

    public String toJson(Object value)
    {
        return JSON.toJSONString(value);
    }
}

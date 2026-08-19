package com.ruoyiliteflow.mcp.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.mcp.tool.McpToolRegistry.DynamicTool;
import com.ruoyiliteflow.mcp.tool.McpToolRegistry.DynamicToolSource;

/**
 * 从 ai_tool 表加载启用的 mcp / liteflow-chain（勾选同步 MCP）工具（与 admin/platform 共享库）。
 */
@Component
@ConditionalOnProperty(prefix = "ruoyi.mcp.dynamic-tools", name = "enabled", havingValue = "true")
public class JdbcDynamicToolSource implements DynamicToolSource
{
    private static final Logger log = LoggerFactory.getLogger(JdbcDynamicToolSource.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcDynamicToolSource(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DynamicTool> loadTools()
    {
        List<DynamicTool> list = new ArrayList<>();
        try
        {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "select tool_code, tool_name, description, invoke_key, mcp_server_key, tool_type "
                            + "from ai_tool where enabled = '1' and (tool_type = 'mcp' "
                            + "or (tool_type = 'liteflow-chain' and mcp_server_key is not null and mcp_server_key <> ''))");
            for (Map<String, Object> row : rows)
            {
                String code = str(row.get("tool_code"));
                if (StringUtils.isEmpty(code))
                {
                    continue;
                }
                // 跳过已由静态 AiCore 提供的同名工具，避免覆盖
                if (isStaticReserved(code))
                {
                    continue;
                }
                String desc = str(row.get("description"));
                if (StringUtils.isEmpty(desc))
                {
                    desc = str(row.get("tool_name"));
                }
                String type = str(row.get("tool_type"));
                String server = str(row.get("mcp_server_key"));
                if ("liteflow-chain".equalsIgnoreCase(type))
                {
                    server = "liteflow";
                }
                else if (StringUtils.isEmpty(server))
                {
                    server = "ai-core";
                }
                list.add(new DynamicTool(code, desc, server, str(row.get("invoke_key"))));
            }
        }
        catch (DataAccessException ex)
        {
            log.warn("dynamic tools load skipped: {}", ex.getMessage());
        }
        return list;
    }

    private static boolean isStaticReserved(String code)
    {
        return switch (code)
        {
            case "list_models", "get_default_model", "chat_completion", "risk_analyze", "rag_ask", "quota_status",
                    "list_chains", "get_chain", "list_scripts", "query_exec_logs", "dashboard_summary" -> true;
            default -> false;
        };
    }

    private static String str(Object o)
    {
        return o == null ? "" : String.valueOf(o);
    }
}

package com.ruoyiliteflow.web.aikit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.aicore.spi.LocalToolHandler;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.domain.LfScript;
import com.ruoyiliteflow.liteflow.domain.vo.LfDashboardVo;
import com.ruoyiliteflow.liteflow.service.ILfChainService;
import com.ruoyiliteflow.liteflow.service.ILfDashboardService;
import com.ruoyiliteflow.liteflow.service.ILfExecLogService;
import com.ruoyiliteflow.liteflow.service.ILfScriptService;

/**
 * 在 admin 进程内执行 lf-governance 工具，不依赖独立 MCP :8090。
 */
@Component
public class LfGovernanceLocalToolHandler implements LocalToolHandler
{
    private static final String SERVER = "lf-governance";
    private static final Set<String> TOOLS = Set.of(
            "list_chains", "get_chain", "list_scripts", "query_exec_logs", "dashboard_summary");
    private static final int LIST_LIMIT = 20;

    private final ILfDashboardService dashboardService;
    private final ILfChainService chainService;
    private final ILfExecLogService execLogService;
    private final ILfScriptService scriptService;

    public LfGovernanceLocalToolHandler(ILfDashboardService dashboardService, ILfChainService chainService,
            ILfExecLogService execLogService, ILfScriptService scriptService)
    {
        this.dashboardService = dashboardService;
        this.chainService = chainService;
        this.execLogService = execLogService;
        this.scriptService = scriptService;
    }

    @Override
    public boolean supports(ToolDescriptor tool)
    {
        if (tool == null)
        {
            return false;
        }
        if (SERVER.equals(tool.getMcpServerKey()))
        {
            return true;
        }
        return TOOLS.contains(toolName(tool));
    }

    @Override
    public String execute(ToolDescriptor tool, String argumentsJson)
    {
        String name = toolName(tool);
        JSONObject args = parseArgs(argumentsJson);
        try
        {
            Object payload = switch (name)
            {
                case "list_chains" -> listChains();
                case "get_chain" -> getChain(firstNonEmpty(args, "chainId", "chainName"));
                case "list_scripts" -> listScripts();
                case "query_exec_logs" -> queryLogs(firstNonEmpty(args, "chainId", "chainName"),
                        args.getIntValue("limit", 5));
                case "dashboard_summary" -> dashboard(args.getIntValue("days", 7));
                default -> Map.of("ok", false, "error", "未知治理工具: " + name);
            };
            if (payload instanceof Map<?, ?> map && map.containsKey("ok"))
            {
                return JSON.toJSONString(payload);
            }
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("ok", true);
            wrap.put("source", "admin-local");
            wrap.put("data", payload);
            return JSON.toJSONString(wrap);
        }
        catch (Exception ex)
        {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false);
            err.put("source", "admin-local");
            err.put("tool", name);
            err.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return JSON.toJSONString(err);
        }
    }

    private List<Map<String, Object>> listChains()
    {
        List<LfChain> all = chainService.selectLfChainList(new LfChain());
        List<Map<String, Object>> out = new ArrayList<>();
        if (all == null)
        {
            return out;
        }
        for (LfChain c : all)
        {
            if (out.size() >= LIST_LIMIT)
            {
                break;
            }
            out.add(toChainMap(c, false));
        }
        return out;
    }

    private Map<String, Object> getChain(String chainId)
    {
        if (StringUtils.isEmpty(chainId))
        {
            return Map.of("ok", false, "error", "缺少 chainId");
        }
        LfChain chain = chainService.selectLfChainByName(chainId);
        if (chain == null && chainId.chars().allMatch(Character::isDigit))
        {
            chain = chainService.selectLfChainById(Long.parseLong(chainId));
        }
        if (chain == null)
        {
            return Map.of("ok", false, "error", "链路不存在: " + chainId);
        }
        return toChainMap(chain, true);
    }

    private List<Map<String, Object>> listScripts()
    {
        List<LfScript> all = scriptService.selectLfScriptList(new LfScript());
        List<Map<String, Object>> out = new ArrayList<>();
        if (all == null)
        {
            return out;
        }
        for (LfScript s : all)
        {
            if (out.size() >= LIST_LIMIT)
            {
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("scriptId", s.getScriptId());
            row.put("name", s.getScriptName());
            row.put("language", s.getScriptLanguage());
            row.put("type", s.getScriptType());
            row.put("enable", s.getEnable());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> queryLogs(String chainId, int limit)
    {
        int cap = Math.max(1, Math.min(limit, LIST_LIMIT));
        LfExecLog query = new LfExecLog();
        if (StringUtils.isNotEmpty(chainId))
        {
            query.setChainName(chainId);
        }
        List<LfExecLog> all = execLogService.selectLfExecLogList(query);
        List<Map<String, Object>> out = new ArrayList<>();
        if (all == null)
        {
            return out;
        }
        for (LfExecLog log : all)
        {
            if (out.size() >= cap)
            {
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("logId", log.getId());
            row.put("chainId", log.getChainName());
            row.put("success", Integer.valueOf(1).equals(log.getSuccess()));
            row.put("costMs", log.getDurationMs());
            row.put("message", StringUtils.isNotEmpty(log.getErrorMessage()) ? log.getErrorMessage() : log.getMessage());
            out.add(row);
        }
        return out;
    }

    private Map<String, Object> dashboard(int days)
    {
        int window = days > 0 ? days : 7;
        LfDashboardVo vo = dashboardService.getDashboard(window);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("successRate", vo == null ? 0 : vo.getSuccessRate());
        out.put("todayCalls", vo == null ? 0 : vo.getTotalCalls());
        out.put("failCount", vo == null ? 0 : vo.getFailCount());
        out.put("avgDurationMs", vo == null ? 0 : vo.getAvgDurationMs());
        out.put("days", window);
        List<String> top = new ArrayList<>();
        if (vo != null && vo.getChainStats() != null)
        {
            for (LfDashboardVo.LfDashboardChainStat stat : vo.getChainStats())
            {
                if (top.size() >= 5)
                {
                    break;
                }
                if (stat != null && StringUtils.isNotEmpty(stat.getChainName()))
                {
                    top.add(stat.getChainName());
                }
            }
        }
        out.put("topChains", top);
        return out;
    }

    private static Map<String, Object> toChainMap(LfChain chain, boolean detail)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("chainId", chain.getChainName());
        row.put("chainName", chain.getChainDesc());
        row.put("status", "1".equals(chain.getStatus()) ? "disabled" : "normal");
        row.put("draftFlag", chain.getDraftFlag());
        row.put("enable", chain.getEnable());
        if (detail)
        {
            String el = chain.getElData();
            if (el != null && el.length() > 240)
            {
                el = el.substring(0, 240) + "...";
            }
            row.put("elSummary", el);
        }
        return row;
    }

    private static String toolName(ToolDescriptor tool)
    {
        if (StringUtils.isNotEmpty(tool.getInvokeKey()))
        {
            return tool.getInvokeKey();
        }
        return tool.getToolCode() == null ? "" : tool.getToolCode();
    }

    private static JSONObject parseArgs(String argumentsJson)
    {
        if (StringUtils.isEmpty(argumentsJson))
        {
            return new JSONObject();
        }
        try
        {
            JSONObject obj = JSON.parseObject(argumentsJson);
            return obj == null ? new JSONObject() : obj;
        }
        catch (Exception ex)
        {
            return new JSONObject();
        }
    }

    private static String firstNonEmpty(JSONObject args, String... keys)
    {
        if (args == null)
        {
            return null;
        }
        for (String key : keys)
        {
            String v = args.getString(key);
            if (StringUtils.isNotEmpty(v))
            {
                return v;
            }
        }
        return null;
    }
}

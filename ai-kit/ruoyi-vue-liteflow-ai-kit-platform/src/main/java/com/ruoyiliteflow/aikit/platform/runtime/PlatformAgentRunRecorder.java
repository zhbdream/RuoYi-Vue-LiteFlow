package com.ruoyiliteflow.aikit.platform.runtime;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.aicore.spi.AgentRunLog;
import com.ruoyiliteflow.aicore.spi.AgentRunRecorder;
import com.ruoyiliteflow.aikit.platform.domain.AiAgentRunLog;
import com.ruoyiliteflow.aikit.platform.mapper.AiAgentRunLogMapper;
import com.ruoyiliteflow.common.utils.StringUtils;

@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformAgentRunRecorder implements AgentRunRecorder
{
    private static final Logger log = LoggerFactory.getLogger(PlatformAgentRunRecorder.class);

    @Autowired
    private AiAgentRunLogMapper runLogMapper;

    @Override
    public void record(AgentRunLog logRow)
    {
        if (logRow == null || StringUtils.isEmpty(logRow.getAgentCode()))
        {
            return;
        }
        try
        {
            AiAgentRunLog row = new AiAgentRunLog();
            row.setAgentCode(logRow.getAgentCode());
            row.setSessionId(StringUtils.isEmpty(logRow.getSessionId()) ? "default" : logRow.getSessionId());
            row.setPrincipal(StringUtils.isEmpty(logRow.getPrincipal()) ? "anonymous" : logRow.getPrincipal());
            row.setModel(logRow.getModel());
            row.setCostMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, logRow.getCostMs())));
            row.setKbHit(logRow.isKbHit() ? "1" : "0");
            List<Object> trace = logRow.getToolTrace();
            boolean toolHit = false;
            if (trace != null)
            {
                for (Object t : trace)
                {
                    if (t instanceof java.util.Map<?, ?> map && map.containsKey("tool"))
                    {
                        toolHit = true;
                        break;
                    }
                }
            }
            row.setToolHit(toolHit ? "1" : "0");
            String traceJson = trace == null || trace.isEmpty() ? null : JSON.toJSONString(trace);
            if (traceJson != null && traceJson.length() > 4000)
            {
                traceJson = traceJson.substring(0, 4000) + "...";
            }
            row.setToolTrace(traceJson);
            row.setErrorMsg(abbrev(logRow.getErrorMsg(), 1000));
            row.setUserMessage(abbrev(logRow.getUserMessage(), 500));
            runLogMapper.insertAiAgentRunLog(row);
        }
        catch (Exception e)
        {
            log.warn("insert ai_agent_run_log failed: {}", e.getMessage());
        }
    }

    private static String abbrev(String s, int max)
    {
        if (s == null)
        {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}

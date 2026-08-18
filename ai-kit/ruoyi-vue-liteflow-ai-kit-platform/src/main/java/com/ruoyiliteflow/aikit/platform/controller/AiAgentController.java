package com.ruoyiliteflow.aikit.platform.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aicore.spi.AgentStreamListener;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;
import com.ruoyiliteflow.aikit.platform.domain.AiAgentRunLog;
import com.ruoyiliteflow.aikit.platform.service.IAiAgentRunLogService;
import com.ruoyiliteflow.aikit.platform.service.IAiAgentService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/agent")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiAgentController extends BaseController
{
    @Autowired
    private IAiAgentService aiAgentService;

    @Autowired
    private IAiAgentRunLogService aiAgentRunLogService;

    @PreAuthorize("@ss.hasPermi('aikit:agent:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiAgent query)
    {
        startPage();
        List<AiAgent> list = aiAgentService.selectAiAgentList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiAgentService.selectAiAgentById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:add')")
    @Log(title = "AI智能体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiAgent agent)
    {
        agent.setCreateBy(currentUser());
        return toAjax(aiAgentService.insertAiAgent(agent));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:edit')")
    @Log(title = "AI智能体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiAgent agent)
    {
        agent.setUpdateBy(currentUser());
        return toAjax(aiAgentService.updateAiAgent(agent));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:remove')")
    @Log(title = "AI智能体", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiAgentService.deleteAiAgentByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:run')")
    @PostMapping("/{agentCode}/run")
    public AjaxResult run(@PathVariable String agentCode, @RequestBody AgentRunRequest request)
    {
        if (request != null && StringUtils.isEmpty(request.getPrincipal()))
        {
            request.setPrincipal(currentUser());
        }
        return success(aiAgentService.run(agentCode, request));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:query')")
    @GetMapping("/{agentCode}/logs")
    public TableDataInfo logs(@PathVariable String agentCode, AiAgentRunLog query)
    {
        if (query == null)
        {
            query = new AiAgentRunLog();
        }
        query.setAgentCode(agentCode);
        startPage();
        return getDataTable(aiAgentRunLogService.selectAiAgentRunLogList(query));
    }

    /**
     * 流式试跑。事件：delta / tool / done / error
     */
    @PreAuthorize("@ss.hasPermi('aikit:agent:run')")
    @PostMapping(value = "/{agentCode}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String agentCode, @RequestBody AgentRunRequest request)
    {
        if (request != null && StringUtils.isEmpty(request.getPrincipal()))
        {
            request.setPrincipal(currentUser());
        }
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try
            {
                aiAgentService.stream(agentCode, request, new AgentStreamListener()
                {
                    @Override
                    public void onDelta(String token)
                    {
                        send(emitter, "delta", Map.of("text", token == null ? "" : token));
                    }

                    @Override
                    public void onTool(Object trace)
                    {
                        send(emitter, "tool", trace);
                    }

                    @Override
                    public void onDone(com.ruoyiliteflow.aicore.runtime.AgentRunResult result)
                    {
                        send(emitter, "done", result);
                    }

                    @Override
                    public void onError(String message)
                    {
                        send(emitter, "error", Map.of("message", message == null ? "error" : message));
                    }

                    @Override
                    public boolean isCancelled()
                    {
                        return cancelled.get();
                    }
                });
                emitter.complete();
            }
            catch (Exception e)
            {
                send(emitter, "error", Map.of("message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                emitter.completeWithError(e);
            }
            finally
            {
                SecurityContextHolder.clearContext();
            }
        });
        return emitter;
    }

    private static void send(SseEmitter emitter, String name, Object data)
    {
        try
        {
            emitter.send(SseEmitter.event().name(name).data(data));
        }
        catch (IOException ignored)
        {
            // 客户端断开
        }
    }

    private static String currentUser()
    {
        try
        {
            String name = SecurityUtils.getUsername();
            return StringUtils.isEmpty(name) ? "aikit" : name;
        }
        catch (Exception e)
        {
            return "aikit";
        }
    }
}

package com.ruoyiliteflow.web.controller.liteflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;
import com.ruoyiliteflow.aikit.platform.domain.AiModel;
import com.ruoyiliteflow.aikit.platform.service.IAiAgentService;
import com.ruoyiliteflow.aikit.platform.service.IAiModelService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.LfChatSession;
import com.ruoyiliteflow.langchain.domain.vo.LfChatStreamEventVo;
import com.ruoyiliteflow.langchain.service.ILfChatService;

/**
 * 内部 AI 助手（轻量多轮对话 + SSE）
 */
@RestController
@RequestMapping("/liteflow/chat")
public class LfChatController extends BaseController
{
    private static final long SSE_TIMEOUT_MS = 180_000L;

    @Autowired
    private ILfChatService lfChatService;

    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @Autowired(required = false)
    private IAiModelService aiModelService;

    @Autowired(required = false)
    private IAiAgentService aiAgentService;

    @PreAuthorize("@ss.hasPermi('liteflow:chat:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("models", listModelOptions());
        out.put("agents", listAgentOptions());
        return success(out);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chat:list')")
    @GetMapping("/session/list")
    public AjaxResult listSessions()
    {
        return success(lfChatService.selectSessionList(getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chat:send')")
    @Log(title = "AI助手会话", businessType = BusinessType.INSERT)
    @PostMapping("/session")
    public AjaxResult createSession(@RequestBody(required = false) Map<String, String> body)
    {
        String title = body == null ? null : body.get("title");
        LfChatSession session = lfChatService.createSession(getUsername(), title);
        return success(session);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chat:query')")
    @GetMapping("/session/{id}/messages")
    public AjaxResult messages(@PathVariable Long id)
    {
        return success(lfChatService.selectMessages(id, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chat:remove')")
    @Log(title = "AI助手会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/session/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfChatService.deleteSessions(ids, getUsername()));
    }

    /**
     * 流式对话。事件：delta / tool / done / error
     */
    @PreAuthorize("@ss.hasPermi('liteflow:chat:send')")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatStreamBody body)
    {
        if (body == null || StringUtils.isEmpty(body.getContent()))
        {
            SseEmitter emitter = new SseEmitter(5_000L);
            try
            {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", "消息内容不能为空")));
            }
            catch (IOException ignored)
            {
                // ignore
            }
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String username = getUsername();
        Long sessionId = body.getSessionId();
        String content = body.getContent();
        String modelCode = body.getModelCode();
        String agentCode = body.getAgentCode();
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        CompletableFuture.runAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try
            {
                LfChatStreamEventVo done = lfChatService.streamChat(sessionId, content, username, modelCode, agentCode,
                        event -> {
                    try
                    {
                        if (event != null && event.getTool() != null)
                        {
                            emitter.send(SseEmitter.event().name("tool").data(event.getTool()));
                        }
                        else
                        {
                            emitter.send(SseEmitter.event().name("delta").data(event));
                        }
                    }
                    catch (IOException ignored)
                    {
                        // 客户端断开
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(done));
                emitter.complete();
            }
            catch (Exception e)
            {
                try
                {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
                }
                catch (Exception ignored)
                {
                    // ignore
                }
                emitter.completeWithError(e);
            }
            finally
            {
                SecurityContextHolder.clearContext();
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> emitter.complete());
        return emitter;
    }

    private List<Map<String, Object>> listModelOptions()
    {
        List<Map<String, Object>> models = new ArrayList<>();
        if (aiModelService != null)
        {
            AiModel query = new AiModel();
            query.setStatus("0");
            for (AiModel m : aiModelService.selectAiModelList(query))
            {
                models.add(modelOption(m.getModelCode(), m.getModelName(), m.getModel(), "1".equals(m.getIsDefault())));
            }
            return models;
        }
        LfAgentModel query = new LfAgentModel();
        query.setStatus("0");
        for (LfAgentModel m : lfAgentModelService.selectLfAgentModelList(query))
        {
            models.add(modelOption(m.getModelCode(), m.getModelName(), m.getModel(), "1".equals(m.getIsDefault())));
        }
        return models;
    }

    private List<Map<String, Object>> listAgentOptions()
    {
        List<Map<String, Object>> agents = new ArrayList<>();
        if (aiAgentService == null)
        {
            return agents;
        }
        AiAgent query = new AiAgent();
        query.setEnabled("1");
        for (AiAgent a : aiAgentService.selectAiAgentList(query))
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agentCode", a.getAgentCode());
            row.put("agentName", StringUtils.isNotEmpty(a.getAgentName()) ? a.getAgentName() : a.getAgentCode());
            agents.add(row);
        }
        return agents;
    }

    private static Map<String, Object> modelOption(String code, String name, String model, boolean isDefault)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("modelCode", code);
        row.put("modelName", StringUtils.isNotEmpty(name) ? name : model);
        row.put("model", model);
        row.put("isDefault", isDefault);
        return row;
    }

    public static class ChatStreamBody
    {
        private Long sessionId;
        private String content;
        private String modelCode;
        private String agentCode;

        public Long getSessionId()
        {
            return sessionId;
        }

        public void setSessionId(Long sessionId)
        {
            this.sessionId = sessionId;
        }

        public String getContent()
        {
            return content;
        }

        public void setContent(String content)
        {
            this.content = content;
        }

        public String getModelCode()
        {
            return modelCode;
        }

        public void setModelCode(String modelCode)
        {
            this.modelCode = modelCode;
        }

        public String getAgentCode()
        {
            return agentCode;
        }

        public void setAgentCode(String agentCode)
        {
            this.agentCode = agentCode;
        }
    }
}

package com.ruoyiliteflow.web.controller.liteflow;

import java.io.IOException;
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
     * 流式对话。事件：delta / done / error
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        CompletableFuture.runAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try
            {
                LfChatStreamEventVo done = lfChatService.streamChat(sessionId, content, username, event -> {
                    try
                    {
                        emitter.send(SseEmitter.event().name("delta").data(event));
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

    public static class ChatStreamBody
    {
        private Long sessionId;
        private String content;

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
    }
}

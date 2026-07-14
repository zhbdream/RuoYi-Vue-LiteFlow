package com.ruoyiliteflow.web.controller.liteflow;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

@RestController
@RequestMapping("/liteflow")
public class LiteFlowExecuteController extends BaseController
{
    private static final long SSE_TIMEOUT_MS = 180_000L;

    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow执行", businessType = BusinessType.OTHER)
    @PostMapping("/execute/{chainName}")
    public AjaxResult execute(@PathVariable String chainName, @RequestBody(required = false) Map<String, Object> param)
    {
        return success(liteFlowExecuteService.execute(chainName, param, getUsername()));
    }

    /**
     * Agent / 链路流式试跑（SSE）。事件名：agent.reasoning / agent.tool_result / agent.result / done / error
     */
    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @PostMapping(value = "/execute/stream/{chainName}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeStream(@PathVariable String chainName,
            @RequestBody(required = false) Map<String, Object> param)
    {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String username = getUsername();
        // 异步线程默认无 SecurityContext，权限校验会报「获取用户ID异常」
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        CompletableFuture.runAsync(() -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            try
            {
                LiteFlowExecuteResultVo result = liteFlowExecuteService.executeStream(chainName, param, username, event -> {
                    try
                    {
                        String name = event.getType() == null ? "message" : event.getType();
                        emitter.send(SseEmitter.event().name(name).data(event));
                    }
                    catch (IOException ignored)
                    {
                        // 客户端断开
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(result));
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

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow决策路由", businessType = BusinessType.OTHER)
    @PostMapping("/execute/route")
    public AjaxResult executeRoute(@RequestBody RouteExecuteBody body)
    {
        return success(liteFlowExecuteService.executeRouteChain(
            body.getNamespace(),
            body.getParam(),
            body.getContextClass(),
            getUsername()));
    }

    public static class RouteExecuteBody
    {
        private String namespace;
        private Map<String, Object> param;
        private String contextClass;

        public String getNamespace()
        {
            return namespace;
        }

        public void setNamespace(String namespace)
        {
            this.namespace = namespace;
        }

        public Map<String, Object> getParam()
        {
            return param;
        }

        public void setParam(Map<String, Object> param)
        {
            this.param = param;
        }

        public String getContextClass()
        {
            return contextClass;
        }

        public void setContextClass(String contextClass)
        {
            this.contextClass = contextClass;
        }
    }
}

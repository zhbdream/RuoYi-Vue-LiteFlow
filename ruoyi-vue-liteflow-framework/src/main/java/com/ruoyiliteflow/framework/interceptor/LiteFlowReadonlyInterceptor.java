package com.ruoyiliteflow.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.utils.ServletUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.framework.config.properties.LiteFlowReadonlyProperties;

/**
 * 生产只读：拦截 LiteFlow 写接口，放行执行/校验/查询。
 */
@Component
public class LiteFlowReadonlyInterceptor implements HandlerInterceptor
{
    @Autowired
    private LiteFlowReadonlyProperties readonlyProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        if (!readonlyProperties.isEnabled())
        {
            return true;
        }
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method))
        {
            return true;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotEmpty(contextPath) && path.startsWith(contextPath))
        {
            path = path.substring(contextPath.length());
        }
        if (isWriteAllowed(path))
        {
            return true;
        }
        if (isWriteBlocked(path))
        {
            AjaxResult ajaxResult = AjaxResult.error(readonlyProperties.getMessage());
            ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
            return false;
        }
        return true;
    }

    /** 只读模式下仍允许的写类接口（执行、校验、调试） */
    private boolean isWriteAllowed(String path)
    {
        return path.startsWith("/liteflow/execute")
                || path.startsWith("/liteflow/open/execute")
                || path.equals("/liteflow/el/validate")
                || path.equals("/liteflow/el/execute")
                || path.equals("/liteflow/script/validate");
    }

    /** 需要拦截的写路径前缀 */
    private boolean isWriteBlocked(String path)
    {
        return path.startsWith("/liteflow/chain")
                || path.startsWith("/liteflow/script")
                || path.startsWith("/liteflow/version")
                || path.startsWith("/liteflow/permission")
                || path.startsWith("/liteflow/audit")
                || path.startsWith("/liteflow/log");
    }
}

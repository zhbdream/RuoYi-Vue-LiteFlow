package com.ruoyiliteflow.framework.security.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.constant.HttpStatus;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.domain.model.LoginUser;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.ServletUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.framework.config.properties.LiteFlowOpenApiProperties;
import com.ruoyiliteflow.framework.web.service.PermissionService;
import org.springframework.security.core.Authentication;

/**
 * LiteFlow 开放 API 鉴权：支持 API Key 或若依 Token（liteflow:open:execute）
 */
@Component
public class LiteFlowOpenApiAuthFilter extends OncePerRequestFilter
{
    public static final String OPEN_API_AUTH_ATTR = "liteflowOpenApiAuth";

    private static final String OPEN_API_PATH = "/liteflow/open/";

    @Autowired
    private LiteFlowOpenApiProperties openApiProperties;

    @Autowired
    private PermissionService permissionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        return !isOpenApiRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (isOpenApiInfoRequest(request))
        {
            chain.doFilter(request, response);
            return;
        }
        if (!openApiProperties.isEnabled())
        {
            renderError(response, HttpStatus.FORBIDDEN, "LiteFlow 开放 API 未启用");
            return;
        }
        String headerName = StringUtils.isNotEmpty(openApiProperties.getHeaderName())
                ? openApiProperties.getHeaderName() : "X-LiteFlow-Api-Key";
        String apiKey = request.getHeader(headerName);
        if (StringUtils.isNotEmpty(openApiProperties.getApiKey())
                && StringUtils.isNotEmpty(apiKey)
                && openApiProperties.getApiKey().equals(apiKey))
        {
            request.setAttribute(OPEN_API_AUTH_ATTR, "api-key");
            chain.doFilter(request, response);
            return;
        }
        if (hasOpenApiTokenPermission())
        {
            request.setAttribute(OPEN_API_AUTH_ATTR, "token");
            chain.doFilter(request, response);
            return;
        }
        renderError(response, HttpStatus.UNAUTHORIZED,
                "未授权：请在请求头携带有效的 " + headerName + "，或使用具备 liteflow:open:execute 权限的 Bearer Token");
    }

    private boolean hasOpenApiTokenPermission()
    {
        Authentication authentication = SecurityUtils.getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser))
        {
            return false;
        }
        return permissionService.hasPermi("liteflow:open:execute");
    }

    private boolean isOpenApiRequest(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = StringUtils.isNotEmpty(contextPath) && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
        return path.startsWith(OPEN_API_PATH);
    }

    private boolean isOpenApiInfoRequest(HttpServletRequest request)
    {
        if (!"GET".equalsIgnoreCase(request.getMethod()))
        {
            return false;
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = StringUtils.isNotEmpty(contextPath) && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length()) : uri;
        return "/liteflow/open/info".equals(path);
    }

    private void renderError(HttpServletResponse response, int code, String msg) throws IOException
    {
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}

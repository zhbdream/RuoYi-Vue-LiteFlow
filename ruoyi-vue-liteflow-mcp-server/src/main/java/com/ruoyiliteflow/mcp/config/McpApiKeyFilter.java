package com.ruoyiliteflow.mcp.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyiliteflow.common.utils.StringUtils;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class McpApiKeyFilter extends OncePerRequestFilter
{
    private final McpServerProperties properties;

    public McpApiKeyFilter(McpServerProperties properties)
    {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/mcp/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        if ("/mcp/info".equals(request.getRequestURI()) || request.getRequestURI().endsWith("/mcp/info"))
        {
            filterChain.doFilter(request, response);
            return;
        }
        String expected = properties.getAuth().getApiKey();
        if (StringUtils.isEmpty(expected))
        {
            filterChain.doFilter(request, response);
            return;
        }
        String header = properties.getAuth().getHeaderName();
        String actual = request.getHeader(header);
        if (!expected.equals(actual))
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":401,\"msg\":\"Invalid MCP API Key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

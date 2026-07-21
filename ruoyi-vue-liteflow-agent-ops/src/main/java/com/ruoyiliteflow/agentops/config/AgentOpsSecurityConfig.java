package com.ruoyiliteflow.agentops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Configuration
public class AgentOpsSecurityConfig
{
    @Bean
    public SecurityFilterChain agentOpsSecurityFilterChain(HttpSecurity http) throws Exception
    {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Component
    @ConfigurationProperties(prefix = "ruoyi.agent.ops")
    public static class AgentOpsProperties
    {
        private boolean useMcp = true;
        private String mcpBaseUrl = "http://localhost:8090";
        private String mcpApiKey = "ruoyi-mcp-key-change-me";
        private String mcpHeaderName = "X-MCP-Api-Key";
        private String systemPrompt = "你是 LiteFlow 编排中台的运维助手。根据提供的链路/日志/监控事实回答，不要编造不存在的链路。用简洁中文。";

        public boolean isUseMcp() { return useMcp; }
        public void setUseMcp(boolean useMcp) { this.useMcp = useMcp; }
        public String getMcpBaseUrl() { return mcpBaseUrl; }
        public void setMcpBaseUrl(String mcpBaseUrl) { this.mcpBaseUrl = mcpBaseUrl; }
        public String getMcpApiKey() { return mcpApiKey; }
        public void setMcpApiKey(String mcpApiKey) { this.mcpApiKey = mcpApiKey; }
        public String getMcpHeaderName() { return mcpHeaderName; }
        public void setMcpHeaderName(String mcpHeaderName) { this.mcpHeaderName = mcpHeaderName; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    }
}

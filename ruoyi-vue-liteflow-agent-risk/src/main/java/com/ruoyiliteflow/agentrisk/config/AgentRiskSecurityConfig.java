package com.ruoyiliteflow.agentrisk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Configuration
public class AgentRiskSecurityConfig
{
    @Bean
    public SecurityFilterChain agentRiskSecurityFilterChain(HttpSecurity http) throws Exception
    {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Component
    @ConfigurationProperties(prefix = "ruoyi.agent.risk")
    public static class AgentRiskProperties
    {
        private boolean useMcp = true;
        private String mcpBaseUrl = "http://localhost:8090";
        private String mcpApiKey = "ruoyi-mcp-key-change-me";
        private String mcpHeaderName = "X-MCP-Api-Key";

        public boolean isUseMcp() { return useMcp; }
        public void setUseMcp(boolean useMcp) { this.useMcp = useMcp; }
        public String getMcpBaseUrl() { return mcpBaseUrl; }
        public void setMcpBaseUrl(String mcpBaseUrl) { this.mcpBaseUrl = mcpBaseUrl; }
        public String getMcpApiKey() { return mcpApiKey; }
        public void setMcpApiKey(String mcpApiKey) { this.mcpApiKey = mcpApiKey; }
        public String getMcpHeaderName() { return mcpHeaderName; }
        public void setMcpHeaderName(String mcpHeaderName) { this.mcpHeaderName = mcpHeaderName; }
    }
}

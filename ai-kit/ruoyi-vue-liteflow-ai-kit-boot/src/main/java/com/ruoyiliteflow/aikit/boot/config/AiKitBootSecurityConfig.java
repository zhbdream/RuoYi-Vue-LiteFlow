package com.ruoyiliteflow.aikit.boot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Configuration
public class AiKitBootSecurityConfig
{
    @Bean
    public SecurityFilterChain aiKitBootSecurityFilterChain(HttpSecurity http) throws Exception
    {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Component
    @ConfigurationProperties(prefix = "ruoyi.ai-kit")
    public static class AiKitBootProperties
    {
        /** 是否优先走 MCP HTTP；false 时 Chat/Risk/RAG 走本地 Facade */
        private boolean useMcp = true;
        private String mcpBaseUrl = "http://localhost:8090";
        private String mcpApiKey = "ruoyi-mcp-key-change-me";
        private String mcpHeaderName = "X-MCP-Api-Key";
        private String chatSystemPrompt = "你是 RuoYi-Vue-LiteFlow 内部助手，回答简洁、准确，使用中文。";
        private String opsSystemPrompt = "你是 LiteFlow 编排中台的运维助手。根据提供的链路/日志/监控事实回答，不要编造不存在的链路。用简洁中文。";

        public boolean isUseMcp()
        {
            return useMcp;
        }

        public void setUseMcp(boolean useMcp)
        {
            this.useMcp = useMcp;
        }

        public String getMcpBaseUrl()
        {
            return mcpBaseUrl;
        }

        public void setMcpBaseUrl(String mcpBaseUrl)
        {
            this.mcpBaseUrl = mcpBaseUrl;
        }

        public String getMcpApiKey()
        {
            return mcpApiKey;
        }

        public void setMcpApiKey(String mcpApiKey)
        {
            this.mcpApiKey = mcpApiKey;
        }

        public String getMcpHeaderName()
        {
            return mcpHeaderName;
        }

        public void setMcpHeaderName(String mcpHeaderName)
        {
            this.mcpHeaderName = mcpHeaderName;
        }

        public String getChatSystemPrompt()
        {
            return chatSystemPrompt;
        }

        public void setChatSystemPrompt(String chatSystemPrompt)
        {
            this.chatSystemPrompt = chatSystemPrompt;
        }

        public String getOpsSystemPrompt()
        {
            return opsSystemPrompt;
        }

        public void setOpsSystemPrompt(String opsSystemPrompt)
        {
            this.opsSystemPrompt = opsSystemPrompt;
        }
    }
}

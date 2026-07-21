package com.ruoyiliteflow.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ruoyi.mcp")
public class McpServerProperties
{
    private boolean enabled = true;
    /** http | stdio */
    private String transport = "http";
    private Auth auth = new Auth();
    private Servers servers = new Servers();

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getTransport()
    {
        return transport;
    }

    public void setTransport(String transport)
    {
        this.transport = transport;
    }

    public Auth getAuth()
    {
        return auth;
    }

    public void setAuth(Auth auth)
    {
        this.auth = auth;
    }

    public Servers getServers()
    {
        return servers;
    }

    public void setServers(Servers servers)
    {
        this.servers = servers;
    }

    public static class Auth
    {
        private String apiKey = "ruoyi-mcp-key-change-me";
        private String headerName = "X-MCP-Api-Key";

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }

        public String getHeaderName()
        {
            return headerName;
        }

        public void setHeaderName(String headerName)
        {
            this.headerName = headerName;
        }
    }

    public static class Servers
    {
        private boolean aiCore = true;
        private boolean lfGovernance = false;
        private boolean lfRuntime = false;
        private boolean sys = false;

        public boolean isAiCore()
        {
            return aiCore;
        }

        public void setAiCore(boolean aiCore)
        {
            this.aiCore = aiCore;
        }

        public boolean isLfGovernance()
        {
            return lfGovernance;
        }

        public void setLfGovernance(boolean lfGovernance)
        {
            this.lfGovernance = lfGovernance;
        }

        public boolean isLfRuntime()
        {
            return lfRuntime;
        }

        public void setLfRuntime(boolean lfRuntime)
        {
            this.lfRuntime = lfRuntime;
        }

        public boolean isSys()
        {
            return sys;
        }

        public void setSys(boolean sys)
        {
            this.sys = sys;
        }
    }
}

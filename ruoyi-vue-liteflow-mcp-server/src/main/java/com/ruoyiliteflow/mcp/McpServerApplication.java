package com.ruoyiliteflow.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立 MCP Server 进程（默认 HTTP/SSE）。stdio 模式见 {@link com.ruoyiliteflow.mcp.stdio.McpStdioLauncher}。
 * <p>不依赖 MySQL / Redis；凭证走 yml / 环境变量。
 */
@SpringBootApplication(
        scanBasePackages = "com.ruoyiliteflow.mcp",
        excludeName = {
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.RedisRepositoriesAutoConfiguration",
                "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
                "org.mybatis.spring.boot.autoconfigure.MybatisLanguageDriverAutoConfiguration",
                "com.alibaba.druid.spring.boot4.autoconfigure.DruidDataSourceAutoConfigure",
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
                "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
        })
public class McpServerApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(McpServerApplication.class, args);
    }
}

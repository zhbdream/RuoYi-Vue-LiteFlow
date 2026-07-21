package com.ruoyiliteflow.agentchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立 Chat Agent。不依赖 MySQL / Redis；默认经 MCP HTTP 调系统能力。
 */
@SpringBootApplication(
        scanBasePackages = "com.ruoyiliteflow.agentchat",
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
public class AgentChatApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AgentChatApplication.class, args);
    }
}

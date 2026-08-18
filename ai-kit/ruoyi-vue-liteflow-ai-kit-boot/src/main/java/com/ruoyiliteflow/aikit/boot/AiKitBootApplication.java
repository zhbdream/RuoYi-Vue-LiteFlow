package com.ruoyiliteflow.aikit.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Kit 示例启动器：Chat / Risk / RAG / Ops 合一进程（默认 :8091）。
 * <p>
 * 启用配置面：{@code --spring.profiles.active=platform}，并先导入
 * {@code sql/ry-vue.sql}。
 */
@SpringBootApplication(
        scanBasePackages = "com.ruoyiliteflow.aikit.boot",
        excludeName = {
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration",
                "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        })
public class AiKitBootApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AiKitBootApplication.class, args);
    }
}

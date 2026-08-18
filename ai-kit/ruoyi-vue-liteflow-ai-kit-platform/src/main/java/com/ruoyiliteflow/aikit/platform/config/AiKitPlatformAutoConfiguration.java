package com.ruoyiliteflow.aikit.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = "javax.sql.DataSource")
@ComponentScan("com.ruoyiliteflow.aikit.platform")
@MapperScan("com.ruoyiliteflow.aikit.platform.mapper")
public class AiKitPlatformAutoConfiguration
{
}

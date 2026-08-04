package com.ruoyiliteflow.aicore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.ruoyiliteflow.aicore.spi.AiModelCredentialProvider;
import com.ruoyiliteflow.aicore.spi.YmlAiModelCredentialProvider;

@AutoConfiguration
@ComponentScan("com.ruoyiliteflow.aicore")
public class AiCoreAutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean(AiModelCredentialProvider.class)
    public AiModelCredentialProvider ymlAiModelCredentialProvider(
            @Value("${ruoyi.ai.openai.api-key:${liteflow.agent.openai-compatible.deepseek.api-key:}}") String apiKey,
            @Value("${ruoyi.ai.openai.base-url:${liteflow.agent.openai-compatible.deepseek.base-url:https://api.deepseek.com/v1}}") String baseUrl,
            @Value("${ruoyi.ai.openai.model:${liteflow.agent.demo.model:deepseek-chat}}") String model)
    {
        return new YmlAiModelCredentialProvider(apiKey, baseUrl, model);
    }
}

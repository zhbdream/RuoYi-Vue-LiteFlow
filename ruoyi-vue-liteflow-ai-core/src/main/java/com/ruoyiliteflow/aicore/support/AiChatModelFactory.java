package com.ruoyiliteflow.aicore.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.spi.AiModelCredentialProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

@Component
public class AiChatModelFactory
{
    @Autowired
    private AiModelCredentialProvider credentialProvider;

    public ChatModel createChatModel()
    {
        return createChatModel(0.2);
    }

    public ChatModel createChatModel(double temperature)
    {
        AiModelCredential cred = credentialProvider.resolveCredential();
        return OpenAiChatModel.builder()
                .apiKey(cred.getApiKey())
                .baseUrl(cred.getBaseUrl())
                .modelName(cred.getModelName())
                .temperature(temperature)
                .build();
    }

    public StreamingChatModel createStreamingChatModel()
    {
        return createStreamingChatModel(0.3);
    }

    public StreamingChatModel createStreamingChatModel(double temperature)
    {
        AiModelCredential cred = credentialProvider.resolveCredential();
        return OpenAiStreamingChatModel.builder()
                .apiKey(cred.getApiKey())
                .baseUrl(cred.getBaseUrl())
                .modelName(cred.getModelName())
                .temperature(temperature)
                .build();
    }

    public AiModelCredential resolveCredential()
    {
        return credentialProvider.resolveCredential();
    }
}

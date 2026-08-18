package com.ruoyiliteflow.aicore.facade.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.facade.IAiModelFacade;
import com.ruoyiliteflow.aicore.model.AiModelInfo;
import com.ruoyiliteflow.aicore.spi.AiModelCredentialProvider;

@Service
public class AiModelFacadeImpl implements IAiModelFacade
{
    @Autowired
    private AiModelCredentialProvider credentialProvider;

    @Override
    public List<AiModelInfo> listModels()
    {
        return credentialProvider.listModels();
    }

    @Override
    public AiModelInfo getDefaultModel()
    {
        return credentialProvider.getDefaultModelInfo();
    }
}

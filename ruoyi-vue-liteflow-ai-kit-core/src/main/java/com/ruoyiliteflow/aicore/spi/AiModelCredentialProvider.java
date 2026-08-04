package com.ruoyiliteflow.aicore.spi;

import java.util.Collections;
import java.util.List;
import com.ruoyiliteflow.aicore.model.AiModelCredential;
import com.ruoyiliteflow.aicore.model.AiModelInfo;

/**
 * 模型凭证与元信息提供者。可由 yml、DB「模型配置」等实现。
 */
public interface AiModelCredentialProvider
{
    AiModelCredential resolveCredential();

    default List<AiModelInfo> listModels()
    {
        return Collections.emptyList();
    }

    default AiModelInfo getDefaultModelInfo()
    {
        List<AiModelInfo> list = listModels();
        for (AiModelInfo info : list)
        {
            if (info != null && info.isDefaultModel() && info.isEnabled())
            {
                return info;
            }
        }
        return list.isEmpty() ? null : list.get(0);
    }
}

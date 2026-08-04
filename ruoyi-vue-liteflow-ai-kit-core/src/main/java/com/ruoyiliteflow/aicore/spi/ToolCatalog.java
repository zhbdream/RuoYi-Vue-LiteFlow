package com.ruoyiliteflow.aicore.spi;

import java.util.Collections;
import java.util.List;

public interface ToolCatalog
{
    default List<ToolDescriptor> resolve(List<String> toolCodes)
    {
        return Collections.emptyList();
    }

    default List<ToolDescriptor> listEnabledMcpTools()
    {
        return Collections.emptyList();
    }
}

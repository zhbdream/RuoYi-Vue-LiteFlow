package com.ruoyiliteflow.aikit.platform.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.ruoyiliteflow.aicore.spi.ToolCatalog;
import com.ruoyiliteflow.aicore.spi.ToolDescriptor;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;
import com.ruoyiliteflow.aikit.platform.mapper.AiToolMapper;

@Primary
@Component
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class PlatformToolCatalog implements ToolCatalog
{
    @Autowired
    private AiToolMapper aiToolMapper;

    @Override
    public List<ToolDescriptor> resolve(List<String> toolCodes)
    {
        if (toolCodes == null || toolCodes.isEmpty())
        {
            return Collections.emptyList();
        }
        List<AiTool> tools = aiToolMapper.selectEnabledByCodes(toolCodes);
        return toDescriptors(tools);
    }

    @Override
    public List<ToolDescriptor> listEnabledMcpTools()
    {
        List<AiTool> tools = aiToolMapper.selectEnabledMcpTools();
        return toDescriptors(tools);
    }

    private static List<ToolDescriptor> toDescriptors(List<AiTool> tools)
    {
        if (tools == null || tools.isEmpty())
        {
            return Collections.emptyList();
        }
        List<ToolDescriptor> descriptors = new ArrayList<>(tools.size());
        for (AiTool tool : tools)
        {
            if (tool == null)
            {
                continue;
            }
            ToolDescriptor d = new ToolDescriptor();
            d.setToolCode(tool.getToolCode());
            d.setToolName(tool.getToolName());
            d.setToolType(tool.getToolType());
            d.setDescription(tool.getDescription());
            d.setInvokeKey(tool.getInvokeKey());
            d.setMcpServerKey(tool.getMcpServerKey());
            descriptors.add(d);
        }
        return descriptors;
    }
}

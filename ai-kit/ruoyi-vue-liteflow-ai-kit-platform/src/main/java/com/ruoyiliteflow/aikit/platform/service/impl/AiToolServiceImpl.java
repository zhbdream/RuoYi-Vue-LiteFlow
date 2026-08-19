package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;
import com.ruoyiliteflow.aikit.platform.mapper.AiToolMapper;
import com.ruoyiliteflow.aikit.platform.mcp.McpDynamicToolSyncClient;
import com.ruoyiliteflow.aikit.platform.service.IAiToolService;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

@Service
public class AiToolServiceImpl implements IAiToolService
{
    @Autowired
    private AiToolMapper aiToolMapper;

    @Autowired(required = false)
    private McpDynamicToolSyncClient mcpDynamicToolSyncClient;

    @Override
    public List<AiTool> selectAiToolList(AiTool query)
    {
        return aiToolMapper.selectAiToolList(query);
    }

    @Override
    public AiTool selectAiToolById(Long id)
    {
        return aiToolMapper.selectAiToolById(id);
    }

    @Override
    public AiTool selectAiToolByCode(String toolCode)
    {
        if (StringUtils.isEmpty(toolCode))
        {
            return null;
        }
        return aiToolMapper.selectAiToolByCode(toolCode);
    }

    @Override
    public AiTool selectAiToolByTypeAndInvokeKey(String toolType, String invokeKey)
    {
        if (StringUtils.isEmpty(toolType) || StringUtils.isEmpty(invokeKey))
        {
            return null;
        }
        return aiToolMapper.selectAiToolByTypeAndInvokeKey(toolType, invokeKey);
    }

    @Override
    public int insertAiTool(AiTool tool)
    {
        normalize(tool);
        if (aiToolMapper.selectAiToolByCode(tool.getToolCode()) != null)
        {
            throw new ServiceException("工具编码已存在: " + tool.getToolCode());
        }
        int rows = aiToolMapper.insertAiTool(tool);
        syncRegister(tool);
        return rows;
    }

    @Override
    public int updateAiTool(AiTool tool)
    {
        if (tool.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        normalize(tool);
        AiTool db = aiToolMapper.selectAiToolById(tool.getId());
        if (db == null)
        {
            throw new ServiceException("工具不存在");
        }
        if (StringUtils.isNotEmpty(tool.getToolCode()) && !tool.getToolCode().equals(db.getToolCode()))
        {
            AiTool exist = aiToolMapper.selectAiToolByCode(tool.getToolCode());
            if (exist != null && !exist.getId().equals(tool.getId()))
            {
                throw new ServiceException("工具编码已存在: " + tool.getToolCode());
            }
        }
        int rows = aiToolMapper.updateAiTool(tool);
        String code = StringUtils.isEmpty(tool.getToolCode()) ? db.getToolCode() : tool.getToolCode();
        if (shouldSyncToMcp(tool) && "1".equals(tool.getEnabled()))
        {
            syncRegister(tool);
        }
        else if (shouldSyncToMcp(db) || shouldSyncToMcp(tool))
        {
            syncUnregister(code);
        }
        return rows;
    }

    @Override
    public int deleteAiToolByIds(Long[] ids)
    {
        if (ids != null)
        {
            for (Long id : ids)
            {
                AiTool t = aiToolMapper.selectAiToolById(id);
                if (t != null && shouldSyncToMcp(t))
                {
                    syncUnregister(t.getToolCode());
                }
            }
        }
        return aiToolMapper.deleteAiToolByIds(ids);
    }

    private void syncRegister(AiTool tool)
    {
        if (mcpDynamicToolSyncClient != null)
        {
            mcpDynamicToolSyncClient.register(tool);
        }
    }

    private void syncUnregister(String toolCode)
    {
        if (mcpDynamicToolSyncClient != null)
        {
            mcpDynamicToolSyncClient.unregister(toolCode);
        }
    }

    private static boolean shouldSyncToMcp(AiTool tool)
    {
        if (tool == null)
        {
            return false;
        }
        if ("mcp".equalsIgnoreCase(tool.getToolType()))
        {
            return true;
        }
        return "liteflow-chain".equalsIgnoreCase(tool.getToolType())
                && StringUtils.isNotEmpty(tool.getMcpServerKey());
    }

    private void normalize(AiTool tool)
    {
        if (StringUtils.isEmpty(tool.getToolType()))
        {
            tool.setToolType("local");
        }
        if (StringUtils.isEmpty(tool.getEnabled()))
        {
            tool.setEnabled("1");
        }
        if (StringUtils.isEmpty(tool.getInvokeKey()))
        {
            tool.setInvokeKey(tool.getToolCode());
        }
    }
}

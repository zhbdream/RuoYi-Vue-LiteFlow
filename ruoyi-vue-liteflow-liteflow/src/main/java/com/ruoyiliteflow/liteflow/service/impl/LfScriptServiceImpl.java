package com.ruoyiliteflow.liteflow.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.common.constant.UserConstants;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfScript;
import com.ruoyiliteflow.liteflow.domain.LfScriptVersion;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.mapper.LfScriptMapper;
import com.ruoyiliteflow.liteflow.mapper.LfScriptVersionMapper;
import com.ruoyiliteflow.liteflow.service.ILfScriptService;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import com.yomahub.liteflow.enums.NodeTypeEnum;

@Service
public class LfScriptServiceImpl implements ILfScriptService
{
    private static final String DEFAULT_APP = "ruoyi-liteflow";

    @Autowired
    private LfScriptMapper lfScriptMapper;

    @Autowired
    private LfChainMapper lfChainMapper;

    @Autowired
    private LfScriptVersionMapper lfScriptVersionMapper;

    @Override
    public LfScript selectLfScriptById(Long id)
    {
        return lfScriptMapper.selectLfScriptById(id);
    }

    @Override
    public List<LfScript> selectLfScriptList(LfScript lfScript)
    {
        return lfScriptMapper.selectLfScriptList(lfScript);
    }

    @Override
    public int insertLfScript(LfScript lfScript)
    {
        fillDefaults(lfScript);
        if (lfScript.getVersion() == null || lfScript.getVersion() < 1)
        {
            lfScript.setVersion(1);
        }
        if (!checkScriptIdUnique(lfScript))
        {
            throw new ServiceException("新增脚本'" + lfScript.getScriptId() + "'失败，脚本ID已存在");
        }
        validateScript(lfScript);
        int rows = lfScriptMapper.insertLfScript(lfScript);
        if (rows > 0 && isEnabled(lfScript))
        {
            reloadScript(lfScript);
        }
        return rows;
    }

    @Override
    public int updateLfScript(LfScript lfScript)
    {
        fillDefaults(lfScript);
        if (!checkScriptIdUnique(lfScript))
        {
            throw new ServiceException("修改脚本'" + lfScript.getScriptId() + "'失败，脚本ID已存在");
        }
        validateScript(lfScript);
        LfScript old = lfScriptMapper.selectLfScriptById(lfScript.getId());
        if (old != null && StringUtils.isNotEmpty(old.getScriptData())
                && !old.getScriptData().equals(lfScript.getScriptData()))
        {
            saveScriptVersionSnapshot(old, lfScript.getUpdateBy());
            int next = (old.getVersion() == null || old.getVersion() < 1) ? 2 : old.getVersion() + 1;
            lfScript.setVersion(next);
        }
        else if (old != null && old.getVersion() != null)
        {
            lfScript.setVersion(old.getVersion());
        }
        int rows = lfScriptMapper.updateLfScript(lfScript);
        if (rows > 0 && isEnabled(lfScript))
        {
            reloadScript(lfScript);
        }
        return rows;
    }

    @Override
    public List<LfScriptVersion> selectScriptVersions(Long scriptPk)
    {
        return lfScriptVersionMapper.selectByScriptPk(scriptPk);
    }

    @Override
    public LfScriptVersion selectScriptVersionById(Long id)
    {
        return lfScriptVersionMapper.selectById(id);
    }

    private void saveScriptVersionSnapshot(LfScript old, String operateBy)
    {
        int ver = old.getVersion() == null || old.getVersion() < 1 ? 1 : old.getVersion();
        LfScriptVersion snap = new LfScriptVersion();
        snap.setScriptPk(old.getId());
        snap.setScriptId(old.getScriptId());
        snap.setVersion(ver);
        snap.setScriptData(old.getScriptData());
        snap.setScriptType(old.getScriptType());
        snap.setScriptLanguage(old.getScriptLanguage());
        snap.setPublishBy(StringUtils.isNotEmpty(operateBy) ? operateBy : old.getUpdateBy());
        snap.setRemark("脚本保存前快照 v" + ver);
        lfScriptVersionMapper.insert(snap);
    }

    @Override
    public int deleteLfScriptByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            LfScript script = lfScriptMapper.selectLfScriptById(id);
            if (script != null)
            {
                try
                {
                    FlowBus.unloadScriptNode(script.getScriptId());
                }
                catch (Exception ignored)
                {
                    // ignore unload failure
                }
            }
        }
        return lfScriptMapper.deleteLfScriptByIds(ids);
    }

    @Override
    public void validateScript(LfScript lfScript)
    {
        if (lfScript == null)
        {
            throw new ServiceException("脚本不能为空");
        }
        validateScript(lfScript.getScriptData(), lfScript.getScriptLanguage(), lfScript.getScriptType());
    }

    @Override
    public void validateScript(String scriptData, String scriptLanguage, String scriptType)
    {
        if (StringUtils.isEmpty(scriptData))
        {
            throw new ServiceException("脚本内容不能为空");
        }
        String language = StringUtils.isEmpty(scriptLanguage) ? "groovy" : scriptLanguage;
        if ("qlexpress".equalsIgnoreCase(language))
        {
            validateQlExpressScript(scriptData, scriptType);
            return;
        }
        String tempId = "__script_validate__" + System.currentTimeMillis();
        try
        {
            LiteflowMetaOperator.reloadScript(tempId, scriptData.trim());
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (StringUtils.isEmpty(msg))
            {
                msg = "脚本语法校验失败";
            }
            throw new ServiceException("脚本校验失败: " + msg);
        }
        finally
        {
            try
            {
                FlowBus.unloadScriptNode(tempId);
            }
            catch (Exception ignored)
            {
                // ignore cleanup failure
            }
        }
    }

    @Override
    public List<String> findChainsReferencingScript(String scriptId)
    {
        return findChainsReferencingNode(lfChainMapper, scriptId);
    }

    static List<String> findChainsReferencingNode(LfChainMapper lfChainMapper, String nodeId)
    {
        List<String> chains = new ArrayList<>();
        if (StringUtils.isEmpty(nodeId))
        {
            return chains;
        }
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(nodeId) + "\\b");
        List<LfChain> all = lfChainMapper.selectLfChainList(new LfChain());
        for (LfChain chain : all)
        {
            if (chain.getElData() != null && pattern.matcher(chain.getElData()).find())
            {
                chains.add(chain.getChainName());
            }
        }
        return chains;
    }

    private void reloadScript(LfScript lfScript)
    {
        if (lfScript == null || StringUtils.isEmpty(lfScript.getScriptId()) || StringUtils.isEmpty(lfScript.getScriptData()))
        {
            return;
        }
        if ("qlexpress".equalsIgnoreCase(lfScript.getScriptLanguage()))
        {
            reloadQlExpressScript(lfScript);
            return;
        }
        LiteflowMetaOperator.reloadScript(lfScript.getScriptId(), lfScript.getScriptData());
    }

    private void validateQlExpressScript(String scriptData, String scriptType)
    {
        String tempId = "__script_validate__" + System.currentTimeMillis();
        NodeTypeEnum nodeType = resolveScriptNodeType(scriptType);
        try
        {
            FlowBus.addScriptNodeAndCompile(tempId, scriptData.trim(), nodeType, "qlexpress", DEFAULT_APP);
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (StringUtils.isEmpty(msg))
            {
                msg = "QLExpress 脚本语法校验失败";
            }
            throw new ServiceException("脚本校验失败: " + msg);
        }
        finally
        {
            try
            {
                FlowBus.unloadScriptNode(tempId);
            }
            catch (Exception ignored)
            {
                // ignore cleanup failure
            }
        }
    }

    private void reloadQlExpressScript(LfScript lfScript)
    {
        NodeTypeEnum nodeType = resolveScriptNodeType(lfScript.getScriptType());
        String appName = StringUtils.isNotEmpty(lfScript.getApplicationName()) ? lfScript.getApplicationName() : DEFAULT_APP;
        try
        {
            FlowBus.unloadScriptNode(lfScript.getScriptId());
        }
        catch (Exception ignored)
        {
            // ignore unload failure
        }
        FlowBus.addScriptNodeAndCompile(lfScript.getScriptId(), lfScript.getScriptData(), nodeType, "qlexpress", appName);
    }

    private NodeTypeEnum resolveScriptNodeType(String scriptType)
    {
        if (StringUtils.isEmpty(scriptType))
        {
            return NodeTypeEnum.SCRIPT;
        }
        NodeTypeEnum type = NodeTypeEnum.getEnumByCode(scriptType);
        return type != null ? type : NodeTypeEnum.SCRIPT;
    }

    private void fillDefaults(LfScript lfScript)
    {
        if (StringUtils.isEmpty(lfScript.getApplicationName()))
        {
            lfScript.setApplicationName(DEFAULT_APP);
        }
        if (StringUtils.isEmpty(lfScript.getScriptType()))
        {
            lfScript.setScriptType("script");
        }
        if (StringUtils.isEmpty(lfScript.getScriptLanguage()))
        {
            lfScript.setScriptLanguage("groovy");
        }
        if (lfScript.getEnable() == null)
        {
            lfScript.setEnable(1);
        }
        if (lfScript.getVersion() == null)
        {
            lfScript.setVersion(1);
        }
        if (StringUtils.isEmpty(lfScript.getScriptName()))
        {
            lfScript.setScriptName(lfScript.getScriptId());
        }
    }

    private boolean isEnabled(LfScript lfScript)
    {
        return lfScript.getEnable() != null && lfScript.getEnable() == 1;
    }

    private boolean checkScriptIdUnique(LfScript lfScript)
    {
        Long id = StringUtils.isNull(lfScript.getId()) ? -1L : lfScript.getId();
        LfScript info = lfScriptMapper.selectLfScriptByScriptId(lfScript.getScriptId());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != id.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}

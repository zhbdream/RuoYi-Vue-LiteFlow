package com.ruoyiliteflow.liteflow.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.constant.LiteFlowAuditAction;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainAudit;
import com.ruoyiliteflow.liteflow.mapper.LfChainAuditMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainAuditService;

@Service
public class LfChainAuditServiceImpl implements ILfChainAuditService
{
    @Autowired
    private LfChainAuditMapper lfChainAuditMapper;

    @Override
    public LfChainAudit selectLfChainAuditById(Long id)
    {
        return lfChainAuditMapper.selectLfChainAuditById(id);
    }

    @Override
    public List<LfChainAudit> selectLfChainAuditList(LfChainAudit lfChainAudit)
    {
        return lfChainAuditMapper.selectLfChainAuditList(lfChainAudit);
    }

    @Override
    public int deleteLfChainAuditByIds(Long[] ids)
    {
        return lfChainAuditMapper.deleteLfChainAuditByIds(ids);
    }

    @Override
    public void recordChange(LfChain before, LfChain after, String actionType, String operateBy)
    {
        recordChange(before, after, actionType, operateBy, null);
    }

    @Override
    public void recordChange(LfChain before, LfChain after, String actionType, String operateBy, String remark)
    {
        if (StringUtils.isEmpty(actionType))
        {
            return;
        }
        if (LiteFlowAuditAction.EDIT.equals(actionType) && !hasElOrGraphChanged(before, after))
        {
            return;
        }
        LfChainAudit audit = new LfChainAudit();
        audit.setActionType(actionType);
        audit.setOperateBy(StringUtils.isEmpty(operateBy) ? "system" : operateBy);
        if (before != null)
        {
            audit.setChainId(before.getId());
            audit.setChainName(before.getChainName());
            audit.setElBefore(before.getElData());
        }
        if (after != null)
        {
            audit.setChainId(after.getId());
            audit.setChainName(after.getChainName());
            audit.setElAfter(after.getElData());
            audit.setDraftFlag(after.getDraftFlag());
            audit.setVersion(after.getVersion());
        }
        audit.setRemark(StringUtils.isNotEmpty(remark) ? remark : buildRemark(actionType, before, after));
        lfChainAuditMapper.insertLfChainAudit(audit);
    }

    private boolean hasElOrGraphChanged(LfChain before, LfChain after)
    {
        if (before == null || after == null)
        {
            return true;
        }
        return !StringUtils.equals(before.getElData(), after.getElData())
            || !StringUtils.equals(before.getGraphJson(), after.getGraphJson());
    }

    private String buildRemark(String actionType, LfChain before, LfChain after)
    {
        switch (actionType)
        {
            case LiteFlowAuditAction.CREATE:
                return "新建链路";
            case LiteFlowAuditAction.IMPORT:
                return "导入链路";
            case LiteFlowAuditAction.PUBLISH:
                return "发布链路 v" + (after != null ? after.getVersion() : "-");
            case LiteFlowAuditAction.DELETE:
                return "删除链路";
            case LiteFlowAuditAction.ROLLBACK:
                return "回滚至历史版本快照（草稿，需发布生效）";
            case LiteFlowAuditAction.EDIT:
                if (before != null && after != null && "1".equals(after.getDraftFlag()))
                {
                    return "保存草稿";
                }
                return "编辑链路 EL/画布";
            default:
                return actionType;
        }
    }
}

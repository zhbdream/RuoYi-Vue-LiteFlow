package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainAudit;

public interface ILfChainAuditService
{
    LfChainAudit selectLfChainAuditById(Long id);

    List<LfChainAudit> selectLfChainAuditList(LfChainAudit lfChainAudit);

    int deleteLfChainAuditByIds(Long[] ids);

    void recordChange(LfChain before, LfChain after, String actionType, String operateBy);

    void recordChange(LfChain before, LfChain after, String actionType, String operateBy, String remark);
}

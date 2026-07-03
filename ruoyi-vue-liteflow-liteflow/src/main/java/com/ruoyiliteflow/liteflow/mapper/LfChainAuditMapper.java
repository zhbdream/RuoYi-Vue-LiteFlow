package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainAudit;

public interface LfChainAuditMapper
{
    LfChainAudit selectLfChainAuditById(Long id);

    List<LfChainAudit> selectLfChainAuditList(LfChainAudit lfChainAudit);

    int insertLfChainAudit(LfChainAudit lfChainAudit);

    int deleteLfChainAuditByIds(Long[] ids);
}

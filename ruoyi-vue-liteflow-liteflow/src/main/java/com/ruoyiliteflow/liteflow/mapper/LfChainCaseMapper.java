package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainCase;

public interface LfChainCaseMapper
{
    LfChainCase selectLfChainCaseById(Long id);

    List<LfChainCase> selectLfChainCaseList(LfChainCase query);

    List<LfChainCase> selectEnabledByChainName(String chainName);

    int insertLfChainCase(LfChainCase lfChainCase);

    int updateLfChainCase(LfChainCase lfChainCase);

    int updateLastRun(LfChainCase lfChainCase);

    int deleteLfChainCaseByIds(Long[] ids);
}

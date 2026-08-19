package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainCase;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainCaseBatchRunVo;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainCaseRunVo;

public interface ILfChainCaseService
{
    LfChainCase selectLfChainCaseById(Long id);

    List<LfChainCase> selectLfChainCaseList(LfChainCase query);

    int insertLfChainCase(LfChainCase lfChainCase);

    int updateLfChainCase(LfChainCase lfChainCase);

    int deleteLfChainCaseByIds(Long[] ids);

    LfChainCaseRunVo runCase(Long id, String operateBy);

    LfChainCaseBatchRunVo runEnabledByChainName(String chainName, String operateBy);
}

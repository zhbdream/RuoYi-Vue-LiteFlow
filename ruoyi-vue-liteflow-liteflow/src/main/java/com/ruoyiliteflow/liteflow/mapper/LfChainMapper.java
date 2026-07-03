package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChain;

public interface LfChainMapper
{
    LfChain selectLfChainById(Long id);

    LfChain selectLfChainByName(String chainName);

    List<LfChain> selectLfChainList(LfChain lfChain);

    int insertLfChain(LfChain lfChain);

    int updateLfChain(LfChain lfChain);

    int deleteLfChainById(Long id);

    int deleteLfChainByIds(Long[] ids);
}

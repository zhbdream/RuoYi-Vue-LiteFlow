package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainExportVo;

public interface ILfChainService
{
    LfChain selectLfChainById(Long id);

    LfChain selectLfChainByName(String chainName);

    List<LfChain> selectLfChainList(LfChain lfChain);

    int insertLfChain(LfChain lfChain);

    int updateLfChain(LfChain lfChain);

    int deleteLfChainByIds(Long[] ids, String operateBy);

    void reloadChain(LfChain lfChain);

    void reloadChainByName(String chainName);

    void publishChain(Long id, String publishBy);

    LfChain cloneChain(Long id, String newChainName, String newChainDesc, String createBy);

    LfChainExportVo exportChain(Long id);

    LfChain importChain(LfChainExportVo exportVo, String createBy);
}

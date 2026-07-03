package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;

public interface ILfChainVersionService
{
    LfChainVersion selectLfChainVersionById(Long id);

    List<LfChainVersion> selectLfChainVersionList(LfChainVersion query);

    List<LfChainVersion> selectVersionsByChainId(Long chainId);

    void rollbackToVersion(Long versionId, String operateBy);
}

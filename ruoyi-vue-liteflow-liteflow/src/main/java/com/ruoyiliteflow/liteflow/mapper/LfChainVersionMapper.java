package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;

public interface LfChainVersionMapper
{
    LfChainVersion selectLfChainVersionById(Long id);

    List<LfChainVersion> selectLfChainVersionList(LfChainVersion lfChainVersion);

    int insertLfChainVersion(LfChainVersion lfChainVersion);
}

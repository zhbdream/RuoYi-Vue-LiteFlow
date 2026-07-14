package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.liteflow.domain.LfScriptVersion;

public interface LfScriptVersionMapper
{
    List<LfScriptVersion> selectByScriptPk(@Param("scriptPk") Long scriptPk);

    LfScriptVersion selectById(Long id);

    int insert(LfScriptVersion version);

    Integer selectMaxVersion(@Param("scriptPk") Long scriptPk);
}

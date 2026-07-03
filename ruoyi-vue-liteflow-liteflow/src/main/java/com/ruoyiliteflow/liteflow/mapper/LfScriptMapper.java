package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfScript;

public interface LfScriptMapper
{
    LfScript selectLfScriptById(Long id);

    LfScript selectLfScriptByScriptId(String scriptId);

    List<LfScript> selectLfScriptList(LfScript lfScript);

    int insertLfScript(LfScript lfScript);

    int updateLfScript(LfScript lfScript);

    int deleteLfScriptById(Long id);

    int deleteLfScriptByIds(Long[] ids);
}

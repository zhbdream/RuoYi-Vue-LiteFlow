package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfScript;

public interface ILfScriptService
{
    LfScript selectLfScriptById(Long id);

    List<LfScript> selectLfScriptList(LfScript lfScript);

    int insertLfScript(LfScript lfScript);

    int updateLfScript(LfScript lfScript);

    int deleteLfScriptByIds(Long[] ids);

    void validateScript(LfScript lfScript);

    void validateScript(String scriptData, String scriptLanguage, String scriptType);

    List<String> findChainsReferencingScript(String scriptId);
}

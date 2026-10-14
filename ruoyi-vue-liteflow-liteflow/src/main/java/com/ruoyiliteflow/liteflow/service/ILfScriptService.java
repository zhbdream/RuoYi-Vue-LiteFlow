package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfScript;
import com.ruoyiliteflow.liteflow.domain.LfScriptVersion;

public interface ILfScriptService
{
    LfScript selectLfScriptById(Long id);

    List<LfScript> selectLfScriptList(LfScript lfScript);

    /**
     * 校验通过后保存脚本；启用的脚本同时注册或更新运行节点。
     *
     * @param lfScript 待保存的脚本定义
     * @return 新增记录数
     */
    int insertLfScript(LfScript lfScript);

    /**
     * 修改前校验脚本语法，保存后同步启用脚本的运行节点。
     *
     * @param lfScript 待更新的脚本定义
     * @return 更新记录数
     */
    int updateLfScript(LfScript lfScript);

    int deleteLfScriptByIds(Long[] ids);

    /**
     * 使用临时节点编译脚本，避免校验过程修改正式运行节点。
     *
     * @param lfScript 待校验的脚本定义
     */
    void validateScript(LfScript lfScript);

    /**
     * 实际编译并校验脚本，语法错误时抛出异常，完成后清理临时节点。
     *
     * @param scriptData 脚本内容
     * @param scriptLanguage 脚本语言，未指定时使用 Groovy
     * @param scriptType 脚本节点类型
     */
    void validateScript(String scriptData, String scriptLanguage, String scriptType);

    List<String> findChainsReferencingScript(String scriptId);

    List<LfScriptVersion> selectScriptVersions(Long scriptPk);

    LfScriptVersion selectScriptVersionById(Long id);
}

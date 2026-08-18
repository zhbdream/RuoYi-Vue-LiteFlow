package com.ruoyiliteflow.agent.service;

import java.util.List;
import com.ruoyiliteflow.agent.domain.LfAgentModel;

public interface ILfAgentModelService
{
    List<LfAgentModel> selectLfAgentModelList(LfAgentModel query);

    LfAgentModel selectLfAgentModelById(Long id);

    /** 返回解密后的默认启用模型（供运行时注入）；无配置时返回 null */
    LfAgentModel resolveRuntimeDefault();

    /** 按编码返回解密后的启用模型；不存在或停用时返回 null */
    LfAgentModel resolveRuntimeByCode(String modelCode);

    int insertLfAgentModel(LfAgentModel model);

    int updateLfAgentModel(LfAgentModel model);

    int deleteLfAgentModelByIds(Long[] ids);
}

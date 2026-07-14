package com.ruoyiliteflow.agent.service;

import java.util.List;
import com.ruoyiliteflow.agent.domain.LfAgentModel;

public interface ILfAgentModelService
{
    List<LfAgentModel> selectLfAgentModelList(LfAgentModel query);

    LfAgentModel selectLfAgentModelById(Long id);

    /** 返回解密后的默认启用模型（供运行时注入）；无配置时返回 null */
    LfAgentModel resolveRuntimeDefault();

    int insertLfAgentModel(LfAgentModel model);

    int updateLfAgentModel(LfAgentModel model);

    int deleteLfAgentModelByIds(Long[] ids);
}

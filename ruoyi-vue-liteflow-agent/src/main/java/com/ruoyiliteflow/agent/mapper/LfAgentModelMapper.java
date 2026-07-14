package com.ruoyiliteflow.agent.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.agent.domain.LfAgentModel;

public interface LfAgentModelMapper
{
    LfAgentModel selectLfAgentModelById(Long id);

    LfAgentModel selectLfAgentModelByCode(String modelCode);

    LfAgentModel selectDefaultEnabled();

    List<LfAgentModel> selectLfAgentModelList(LfAgentModel query);

    int insertLfAgentModel(LfAgentModel model);

    int updateLfAgentModel(LfAgentModel model);

    int deleteLfAgentModelById(Long id);

    int deleteLfAgentModelByIds(Long[] ids);

    int clearDefaultFlag(@Param("excludeId") Long excludeId);
}

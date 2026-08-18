package com.ruoyiliteflow.aikit.platform.mapper;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiContextPolicy;

public interface AiContextPolicyMapper
{
    List<AiContextPolicy> selectAiContextPolicyList(AiContextPolicy query);

    AiContextPolicy selectAiContextPolicyById(Long id);

    AiContextPolicy selectAiContextPolicyByCode(String policyCode);

    AiContextPolicy selectDefaultEnabled();

    int insertAiContextPolicy(AiContextPolicy policy);

    int updateAiContextPolicy(AiContextPolicy policy);

    int deleteAiContextPolicyByIds(Long[] ids);

    int clearDefaultFlag(Long excludeId);
}

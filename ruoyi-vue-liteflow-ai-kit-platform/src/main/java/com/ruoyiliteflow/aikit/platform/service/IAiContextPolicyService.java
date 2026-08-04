package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import com.ruoyiliteflow.aikit.platform.domain.AiContextPolicy;

public interface IAiContextPolicyService
{
    List<AiContextPolicy> selectAiContextPolicyList(AiContextPolicy query);

    AiContextPolicy selectAiContextPolicyById(Long id);

    int insertAiContextPolicy(AiContextPolicy policy);

    int updateAiContextPolicy(AiContextPolicy policy);

    int deleteAiContextPolicyByIds(Long[] ids);
}

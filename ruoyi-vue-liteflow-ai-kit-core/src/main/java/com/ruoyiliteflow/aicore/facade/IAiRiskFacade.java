package com.ruoyiliteflow.aicore.facade;

import com.ruoyiliteflow.aicore.model.RiskAnalyzeRequest;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeResult;

public interface IAiRiskFacade
{
    RiskAnalyzeResult analyze(RiskAnalyzeRequest request);
}

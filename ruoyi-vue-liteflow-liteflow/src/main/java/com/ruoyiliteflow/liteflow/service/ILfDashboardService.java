package com.ruoyiliteflow.liteflow.service;

import com.ruoyiliteflow.liteflow.domain.vo.LfDashboardVo;

public interface ILfDashboardService
{
    LfDashboardVo getDashboard(int days);
}

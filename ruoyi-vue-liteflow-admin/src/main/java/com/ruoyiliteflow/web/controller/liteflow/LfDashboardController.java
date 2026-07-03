package com.ruoyiliteflow.web.controller.liteflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.liteflow.service.ILfDashboardService;

@RestController
@RequestMapping("/liteflow/dashboard")
public class LfDashboardController extends BaseController
{
    @Autowired
    private ILfDashboardService lfDashboardService;

    @PreAuthorize("@ss.hasPermi('liteflow:dashboard:view')")
    @GetMapping
    public AjaxResult getDashboard(@RequestParam(defaultValue = "7") int days)
    {
        return success(lfDashboardService.getDashboard(days));
    }
}

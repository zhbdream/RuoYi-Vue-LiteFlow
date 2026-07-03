package com.ruoyiliteflow.web.controller.liteflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

@RestController
@RequestMapping("/liteflow/component")
public class LiteFlowComponentController extends BaseController
{
    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @PreAuthorize("@ss.hasPermi('liteflow:chain:list')")
    @GetMapping("/list")
    public AjaxResult list()
    {
        return success(liteFlowExecuteService.listComponents());
    }

    @PreAuthorize("@ss.hasPermi('liteflow:component:list')")
    @GetMapping("/center")
    public AjaxResult center()
    {
        return success(liteFlowExecuteService.listComponentsWithRefs());
    }

    @PreAuthorize("@ss.hasPermi('liteflow:component:list')")
    @GetMapping("/refs/{nodeId}")
    public AjaxResult refs(@PathVariable String nodeId)
    {
        return success(liteFlowExecuteService.findChainsReferencingNode(nodeId));
    }
}

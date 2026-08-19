package com.ruoyiliteflow.web.controller.liteflow;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.aikit.platform.domain.AiTool;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.web.service.LiteFlowChainAsToolService;

@RestController
@RequestMapping("/liteflow/chain/as-tool")
public class LiteFlowChainAsToolController extends BaseController
{
    private final LiteFlowChainAsToolService chainAsToolService;

    public LiteFlowChainAsToolController(LiteFlowChainAsToolService chainAsToolService)
    {
        this.chainAsToolService = chainAsToolService;
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/{id}")
    public AjaxResult status(@PathVariable Long id)
    {
        return success(chainAsToolService.status(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "链路设为工具", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}")
    public AjaxResult expose(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body)
    {
        boolean exposeMcp = body != null && Boolean.TRUE.equals(body.get("exposeMcp"));
        String schema = body == null ? null : str(body.get("inputSchemaJson"));
        AiTool tool = chainAsToolService.expose(id, exposeMcp, schema, getUsername());
        return success(tool);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "取消链路工具", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult unexpose(@PathVariable Long id)
    {
        chainAsToolService.unexpose(id);
        return success();
    }

    private static String str(Object o)
    {
        return o == null ? null : String.valueOf(o);
    }
}

package com.ruoyiliteflow.web.controller.liteflow;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;
import com.ruoyiliteflow.liteflow.service.LiteFlowComponentScaffoldService;

@RestController
@RequestMapping("/liteflow/component")
public class LiteFlowComponentController extends BaseController
{
    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @Autowired
    private LiteFlowComponentScaffoldService scaffoldService;

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

    /**
     * 生成组件 Java 脚手架源码
     * body: { nodeId, nodeType, packageName, style } style=inherited|declarative
     */
    @PreAuthorize("@ss.hasPermi('liteflow:component:list')")
    @PostMapping("/scaffold")
    public AjaxResult scaffold(@RequestBody Map<String, String> body)
    {
        String nodeId = body == null ? null : body.get("nodeId");
        String nodeType = body == null ? null : body.get("nodeType");
        String packageName = body == null ? null : body.get("packageName");
        String style = body == null ? null : body.get("style");
        String source = scaffoldService.generate(nodeId, nodeType, packageName, style);
        String className = Character.toUpperCase(nodeId.charAt(0)) + nodeId.substring(1) + "Component.java";
        Map<String, Object> data = new HashMap<>(4);
        data.put("fileName", className);
        data.put("source", source);
        return success(data);
    }
}

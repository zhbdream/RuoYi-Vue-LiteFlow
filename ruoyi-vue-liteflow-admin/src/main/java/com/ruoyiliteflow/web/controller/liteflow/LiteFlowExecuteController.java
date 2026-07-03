package com.ruoyiliteflow.web.controller.liteflow;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

@RestController
@RequestMapping("/liteflow")
public class LiteFlowExecuteController extends BaseController
{
    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow执行", businessType = BusinessType.OTHER)
    @PostMapping("/execute/{chainName}")
    public AjaxResult execute(@PathVariable String chainName, @RequestBody(required = false) Map<String, Object> param)
    {
        return success(liteFlowExecuteService.execute(chainName, param, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow决策路由", businessType = BusinessType.OTHER)
    @PostMapping("/execute/route")
    public AjaxResult executeRoute(@RequestBody RouteExecuteBody body)
    {
        return success(liteFlowExecuteService.executeRouteChain(
            body.getNamespace(),
            body.getParam(),
            body.getContextClass(),
            getUsername()));
    }

    public static class RouteExecuteBody
    {
        private String namespace;
        private Map<String, Object> param;
        private String contextClass;

        public String getNamespace()
        {
            return namespace;
        }

        public void setNamespace(String namespace)
        {
            this.namespace = namespace;
        }

        public Map<String, Object> getParam()
        {
            return param;
        }

        public void setParam(Map<String, Object> param)
        {
            this.param = param;
        }

        public String getContextClass()
        {
            return contextClass;
        }

        public void setContextClass(String contextClass)
        {
            this.contextClass = contextClass;
        }
    }
}

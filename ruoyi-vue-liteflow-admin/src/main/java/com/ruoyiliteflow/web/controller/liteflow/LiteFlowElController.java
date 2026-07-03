package com.ruoyiliteflow.web.controller.liteflow;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.liteflow.service.ILiteFlowElService;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

@RestController
@RequestMapping("/liteflow/el")
public class LiteFlowElController extends BaseController
{
    @Autowired
    private ILiteFlowElService liteFlowElService;

    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @PostMapping("/validate")
    public AjaxResult validate(@RequestBody ElValidateBody body)
    {
        liteFlowElService.validateEl(body.getElData());
        return success("EL 校验通过");
    }

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow EL调试", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody ElExecuteBody body)
    {
        return success(liteFlowExecuteService.executeWithEl(
            body.getElData(),
            body.getParam(),
            body.getContextClass(),
            getUsername()));
    }

    public static class ElValidateBody
    {
        private String elData;

        public String getElData()
        {
            return elData;
        }

        public void setElData(String elData)
        {
            this.elData = elData;
        }
    }

    public static class ElExecuteBody
    {
        private String elData;
        private Map<String, Object> param;
        private String contextClass;

        public String getElData()
        {
            return elData;
        }

        public void setElData(String elData)
        {
            this.elData = elData;
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
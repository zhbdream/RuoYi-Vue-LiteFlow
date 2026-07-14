package com.ruoyiliteflow.web.controller.liteflow;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.framework.config.properties.LiteFlowOpenApiProperties;
import com.ruoyiliteflow.framework.config.properties.LiteFlowReadonlyProperties;

/**
 * LiteFlow 运行时配置（供前端只读模式等使用）
 */
@RestController
@RequestMapping("/liteflow/config")
public class LiteFlowConfigController extends BaseController
{
    @Autowired
    private LiteFlowReadonlyProperties readonlyProperties;

    @Autowired
    private LiteFlowOpenApiProperties openApiProperties;

    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @Value("${liteflow.agent.openai-compatible.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${liteflow.agent.demo.model:deepseek-chat}")
    private String agentDemoModel;

    @GetMapping
    public AjaxResult getConfig()
    {
        Map<String, Object> data = new HashMap<>(8);
        data.put("readonly", readonlyProperties.isEnabled());
        data.put("readonlyMessage", readonlyProperties.getMessage());
        data.put("openApiAllowAgentChains", openApiProperties.isAllowAgentChains());
        data.put("agentConfigured", hasAgentCredential());
        data.put("agentDemoModel", agentDemoModel);
        return success(data);
    }

    private boolean hasAgentCredential()
    {
        if (StringUtils.isNotEmpty(deepseekApiKey))
        {
            return true;
        }
        try
        {
            LfAgentModel model = lfAgentModelService.resolveRuntimeDefault();
            return model != null && StringUtils.isNotEmpty(model.getApiKey());
        }
        catch (Exception e)
        {
            return false;
        }
    }
}

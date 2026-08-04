package com.ruoyiliteflow.aikit.platform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.aicore.runtime.AgentRunRequest;
import com.ruoyiliteflow.aikit.platform.domain.AiAgent;
import com.ruoyiliteflow.aikit.platform.service.IAiAgentService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/agent")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiAgentController extends BaseController
{
    @Autowired
    private IAiAgentService aiAgentService;

    @PreAuthorize("@ss.hasPermi('aikit:agent:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiAgent query)
    {
        startPage();
        List<AiAgent> list = aiAgentService.selectAiAgentList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiAgentService.selectAiAgentById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:add')")
    @Log(title = "AI智能体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiAgent agent)
    {
        agent.setCreateBy(currentUser());
        return toAjax(aiAgentService.insertAiAgent(agent));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:edit')")
    @Log(title = "AI智能体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiAgent agent)
    {
        agent.setUpdateBy(currentUser());
        return toAjax(aiAgentService.updateAiAgent(agent));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:remove')")
    @Log(title = "AI智能体", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiAgentService.deleteAiAgentByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('aikit:agent:run')")
    @PostMapping("/{agentCode}/run")
    public AjaxResult run(@PathVariable String agentCode, @RequestBody AgentRunRequest request)
    {
        return success(aiAgentService.run(agentCode, request));
    }

    private static String currentUser()
    {
        try
        {
            String name = SecurityUtils.getUsername();
            return StringUtils.isEmpty(name) ? "aikit" : name;
        }
        catch (Exception e)
        {
            return "aikit";
        }
    }
}

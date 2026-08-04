package com.ruoyiliteflow.aikit.platform.controller;

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
import com.ruoyiliteflow.aikit.platform.domain.AiContextPolicy;
import com.ruoyiliteflow.aikit.platform.service.IAiContextPolicyService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/context")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiContextPolicyController extends BaseController
{
    @Autowired
    private IAiContextPolicyService aiContextPolicyService;

    @PreAuthorize("@ss.hasPermi('aikit:context:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiContextPolicy query)
    {
        startPage();
        return getDataTable(aiContextPolicyService.selectAiContextPolicyList(query));
    }

    @PreAuthorize("@ss.hasPermi('aikit:context:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiContextPolicyService.selectAiContextPolicyById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:context:add')")
    @Log(title = "AI上下文策略", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiContextPolicy policy)
    {
        policy.setCreateBy(currentUser());
        return toAjax(aiContextPolicyService.insertAiContextPolicy(policy));
    }

    @PreAuthorize("@ss.hasPermi('aikit:context:edit')")
    @Log(title = "AI上下文策略", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiContextPolicy policy)
    {
        policy.setUpdateBy(currentUser());
        return toAjax(aiContextPolicyService.updateAiContextPolicy(policy));
    }

    @PreAuthorize("@ss.hasPermi('aikit:context:remove')")
    @Log(title = "AI上下文策略", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiContextPolicyService.deleteAiContextPolicyByIds(ids));
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

package com.ruoyiliteflow.aikit.platform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.aikit.platform.domain.AiMemoryItem;
import com.ruoyiliteflow.aikit.platform.service.IAiMemoryService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/memory")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiMemoryController extends BaseController
{
    @Autowired
    private IAiMemoryService aiMemoryService;

    @PreAuthorize("@ss.hasPermi('aikit:memory:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiMemoryItem query)
    {
        startPage();
        return getDataTable(aiMemoryService.selectAiMemoryItemList(query));
    }

    @PreAuthorize("@ss.hasPermi('aikit:memory:add')")
    @Log(title = "AI记忆", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiMemoryItem item)
    {
        item.setCreateBy(currentUser());
        return toAjax(aiMemoryService.insertAiMemoryItem(item));
    }

    @PreAuthorize("@ss.hasPermi('aikit:memory:remove')")
    @Log(title = "AI记忆", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiMemoryService.deleteAiMemoryItemByIds(ids));
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

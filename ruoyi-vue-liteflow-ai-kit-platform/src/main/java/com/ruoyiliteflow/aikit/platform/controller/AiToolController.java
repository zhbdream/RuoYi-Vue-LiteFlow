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
import com.ruoyiliteflow.aikit.platform.domain.AiTool;
import com.ruoyiliteflow.aikit.platform.service.IAiToolService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/tool")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiToolController extends BaseController
{
    @Autowired
    private IAiToolService aiToolService;

    @PreAuthorize("@ss.hasPermi('aikit:tool:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiTool query)
    {
        startPage();
        return getDataTable(aiToolService.selectAiToolList(query));
    }

    @PreAuthorize("@ss.hasPermi('aikit:tool:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiToolService.selectAiToolById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:tool:add')")
    @Log(title = "AI工具", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiTool tool)
    {
        tool.setCreateBy(currentUser());
        return toAjax(aiToolService.insertAiTool(tool));
    }

    @PreAuthorize("@ss.hasPermi('aikit:tool:edit')")
    @Log(title = "AI工具", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiTool tool)
    {
        tool.setUpdateBy(currentUser());
        return toAjax(aiToolService.updateAiTool(tool));
    }

    @PreAuthorize("@ss.hasPermi('aikit:tool:remove')")
    @Log(title = "AI工具", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiToolService.deleteAiToolByIds(ids));
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

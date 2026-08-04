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
import com.ruoyiliteflow.aikit.platform.domain.AiSkill;
import com.ruoyiliteflow.aikit.platform.service.IAiSkillService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/skill")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiSkillController extends BaseController
{
    @Autowired
    private IAiSkillService aiSkillService;

    @PreAuthorize("@ss.hasPermi('aikit:skill:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiSkill query)
    {
        startPage();
        return getDataTable(aiSkillService.selectAiSkillList(query));
    }

    @PreAuthorize("@ss.hasPermi('aikit:skill:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiSkillService.selectAiSkillById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:skill:add')")
    @Log(title = "AI技能", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiSkill skill)
    {
        skill.setCreateBy(currentUser());
        return toAjax(aiSkillService.insertAiSkill(skill));
    }

    @PreAuthorize("@ss.hasPermi('aikit:skill:edit')")
    @Log(title = "AI技能", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiSkill skill)
    {
        skill.setUpdateBy(currentUser());
        return toAjax(aiSkillService.updateAiSkill(skill));
    }

    @PreAuthorize("@ss.hasPermi('aikit:skill:remove')")
    @Log(title = "AI技能", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiSkillService.deleteAiSkillByIds(ids));
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

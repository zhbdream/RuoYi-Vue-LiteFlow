package com.ruoyiliteflow.aikit.platform.controller;

import java.util.List;
import java.util.Map;
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
import com.ruoyiliteflow.aikit.platform.domain.AiModel;
import com.ruoyiliteflow.aikit.platform.service.IAiModelService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/model")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiModelController extends BaseController
{
    @Autowired
    private IAiModelService aiModelService;

    @PreAuthorize("@ss.hasPermi('aikit:model:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiModel query)
    {
        startPage();
        List<AiModel> list = aiModelService.selectAiModelList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aikit:model:list')")
    @GetMapping("/sources")
    public AjaxResult sources()
    {
        return success(aiModelService.describeSources());
    }

    @PreAuthorize("@ss.hasPermi('aikit:model:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(aiModelService.selectAiModelById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:model:add')")
    @Log(title = "AI模型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiModel model)
    {
        model.setCreateBy(currentUser());
        return toAjax(aiModelService.insertAiModel(model));
    }

    @PreAuthorize("@ss.hasPermi('aikit:model:edit')")
    @Log(title = "AI模型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiModel model)
    {
        model.setUpdateBy(currentUser());
        return toAjax(aiModelService.updateAiModel(model));
    }

    @PreAuthorize("@ss.hasPermi('aikit:model:remove')")
    @Log(title = "AI模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(aiModelService.deleteAiModelByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('aikit:model:test')")
    @PostMapping("/test")
    public AjaxResult test(@RequestBody(required = false) AiModel model)
    {
        String reply = aiModelService.testConnectivity(model);
        return success(Map.of("ok", true, "reply", reply));
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

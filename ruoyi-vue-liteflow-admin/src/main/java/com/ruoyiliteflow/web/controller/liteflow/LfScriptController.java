package com.ruoyiliteflow.web.controller.liteflow;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.liteflow.domain.LfScript;
import com.ruoyiliteflow.liteflow.service.ILfScriptService;

@RestController
@RequestMapping("/liteflow/script")
public class LfScriptController extends BaseController
{
    @Autowired
    private ILfScriptService lfScriptService;

    @PreAuthorize("@ss.hasPermi('liteflow:script:list')")
    @GetMapping("/list")
    public TableDataInfo list(LfScript lfScript)
    {
        startPage();
        List<LfScript> list = lfScriptService.selectLfScriptList(lfScript);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:script:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfScriptService.selectLfScriptById(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:script:query')")
    @GetMapping(value = "/refs/{scriptId}")
    public AjaxResult refs(@PathVariable String scriptId)
    {
        return success(lfScriptService.findChainsReferencingScript(scriptId));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:script:add')")
    @Log(title = "LiteFlow脚本", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LfScript lfScript)
    {
        lfScript.setCreateBy(getUsername());
        return toAjax(lfScriptService.insertLfScript(lfScript));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:script:edit')")
    @Log(title = "LiteFlow脚本", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody LfScript lfScript)
    {
        lfScript.setUpdateBy(getUsername());
        return toAjax(lfScriptService.updateLfScript(lfScript));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:script:remove')")
    @Log(title = "LiteFlow脚本", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfScriptService.deleteLfScriptByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:script:edit')")
    @PostMapping("/validate")
    public AjaxResult validate(@RequestBody LfScript lfScript)
    {
        lfScriptService.validateScript(lfScript);
        return success("脚本校验通过");
    }
}

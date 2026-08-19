package com.ruoyiliteflow.web.controller.liteflow;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.ruoyiliteflow.liteflow.domain.LfChainCase;
import com.ruoyiliteflow.liteflow.service.ILfChainCaseService;

@RestController
@RequestMapping("/liteflow/chain/case")
public class LfChainCaseController extends BaseController
{
    @Autowired
    private ILfChainCaseService lfChainCaseService;

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/list")
    public TableDataInfo list(LfChainCase query)
    {
        startPage();
        List<LfChainCase> list = lfChainCaseService.selectLfChainCaseList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfChainCaseService.selectLfChainCaseById(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @Log(title = "LiteFlow试跑用例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LfChainCase lfChainCase)
    {
        lfChainCase.setCreateBy(getUsername());
        return toAjax(lfChainCaseService.insertLfChainCase(lfChainCase));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @Log(title = "LiteFlow试跑用例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LfChainCase lfChainCase)
    {
        lfChainCase.setUpdateBy(getUsername());
        return toAjax(lfChainCaseService.updateLfChainCase(lfChainCase));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @Log(title = "LiteFlow试跑用例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfChainCaseService.deleteLfChainCaseByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow试跑用例", businessType = BusinessType.OTHER)
    @PostMapping("/run/{id}")
    public AjaxResult run(@PathVariable Long id)
    {
        return success(lfChainCaseService.runCase(id, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:execute')")
    @Log(title = "LiteFlow试跑用例", businessType = BusinessType.OTHER)
    @PostMapping("/runAll/{chainName}")
    public AjaxResult runAll(@PathVariable String chainName)
    {
        return success(lfChainCaseService.runEnabledByChainName(chainName, getUsername()));
    }
}

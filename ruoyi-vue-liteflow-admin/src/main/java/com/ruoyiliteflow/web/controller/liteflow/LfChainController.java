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
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainExportVo;
import com.ruoyiliteflow.liteflow.service.ILfChainService;

@RestController
@RequestMapping("/liteflow/chain")
public class LfChainController extends BaseController
{
    @Autowired
    private ILfChainService lfChainService;

    @PreAuthorize("@ss.hasPermi('liteflow:chain:list')")
    @GetMapping("/list")
    public TableDataInfo list(LfChain lfChain)
    {
        startPage();
        List<LfChain> list = lfChainService.selectLfChainList(lfChain);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfChainService.selectLfChainById(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping(value = "/name/{chainName}")
    public AjaxResult getByName(@PathVariable String chainName)
    {
        return success(lfChainService.selectLfChainByName(chainName));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:add')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LfChain lfChain)
    {
        lfChain.setCreateBy(getUsername());
        return toAjax(lfChainService.insertLfChain(lfChain));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody LfChain lfChain)
    {
        lfChain.setUpdateBy(getUsername());
        return toAjax(lfChainService.updateLfChain(lfChain));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:remove')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfChainService.deleteLfChainByIds(ids, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:reload')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.UPDATE)
    @PostMapping("/reload/{chainName}")
    public AjaxResult reload(@PathVariable String chainName)
    {
        lfChainService.reloadChainByName(chainName);
        return success("热刷新成功");
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id)
    {
        lfChainService.publishChain(id, getUsername());
        return success("发布成功，规则已热刷新");
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:add')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.INSERT)
    @PostMapping("/clone")
    public AjaxResult clone(@RequestBody LfChain lfChain)
    {
        LfChain chain = lfChainService.cloneChain(lfChain.getId(), lfChain.getChainName(), lfChain.getChainDesc(), getUsername());
        return success(chain);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/export/{id}")
    public AjaxResult exportChain(@PathVariable Long id)
    {
        return success(lfChainService.exportChain(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:add')")
    @Log(title = "LiteFlow链路", businessType = BusinessType.IMPORT)
    @PostMapping("/import")
    public AjaxResult importChain(@RequestBody LfChainExportVo exportVo)
    {
        return success(lfChainService.importChain(exportVo, getUsername()));
    }
}

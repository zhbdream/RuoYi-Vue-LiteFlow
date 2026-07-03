package com.ruoyiliteflow.web.controller.liteflow;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.liteflow.domain.LfChainAudit;
import com.ruoyiliteflow.liteflow.service.ILfChainAuditService;

@RestController
@RequestMapping("/liteflow/audit")
public class LfChainAuditController extends BaseController
{
    @Autowired
    private ILfChainAuditService lfChainAuditService;

    @PreAuthorize("@ss.hasPermi('liteflow:audit:list')")
    @GetMapping("/list")
    public TableDataInfo list(LfChainAudit lfChainAudit)
    {
        startPage();
        List<LfChainAudit> list = lfChainAuditService.selectLfChainAuditList(lfChainAudit);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:audit:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfChainAuditService.selectLfChainAuditById(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:audit:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfChainAuditService.deleteLfChainAuditByIds(ids));
    }
}

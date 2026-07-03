package com.ruoyiliteflow.web.controller.liteflow;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;
import com.ruoyiliteflow.liteflow.service.ILfChainVersionService;

@RestController
@RequestMapping("/liteflow/chain/versions")
public class LfChainVersionController extends BaseController
{
    @Autowired
    private ILfChainVersionService lfChainVersionService;

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/list")
    public TableDataInfo list(LfChainVersion query)
    {
        startPage();
        List<LfChainVersion> list = lfChainVersionService.selectLfChainVersionList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/chain/{chainId}")
    public AjaxResult listByChain(@PathVariable Long chainId)
    {
        return success(lfChainVersionService.selectVersionsByChainId(chainId));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfChainVersionService.selectLfChainVersionById(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:edit')")
    @Log(title = "LiteFlow版本回滚", businessType = BusinessType.UPDATE)
    @PostMapping("/rollback/{id}")
    public AjaxResult rollback(@PathVariable Long id)
    {
        lfChainVersionService.rollbackToVersion(id, getUsername());
        return success("已回滚至所选版本（草稿），请确认后发布");
    }
}

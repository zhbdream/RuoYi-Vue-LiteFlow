package com.ruoyiliteflow.web.controller.liteflow;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.liteflow.domain.LfChainPermission;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainPermissionSaveVo;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;

@RestController
@RequestMapping("/liteflow/chain/permission")
public class LfChainPermissionController extends BaseController
{
    @Autowired
    private ILfChainPermissionService lfChainPermissionService;

    @PreAuthorize("@ss.hasPermi('liteflow:chain:permission')")
    @GetMapping("/{chainName}")
    public AjaxResult list(@PathVariable String chainName)
    {
        List<LfChainPermission> list = lfChainPermissionService.selectByChainName(chainName);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:chain:permission')")
    @Log(title = "LiteFlow链路权限", businessType = BusinessType.UPDATE)
    @PostMapping
    public AjaxResult save(@RequestBody LfChainPermissionSaveVo saveVo)
    {
        lfChainPermissionService.savePermissions(saveVo, getUsername());
        return success();
    }
}

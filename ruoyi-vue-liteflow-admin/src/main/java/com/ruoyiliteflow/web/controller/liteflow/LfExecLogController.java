package com.ruoyiliteflow.web.controller.liteflow;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.liteflow.domain.LfExecLog;
import com.ruoyiliteflow.liteflow.service.ILfExecLogService;

@RestController
@RequestMapping("/liteflow/log")
public class LfExecLogController extends BaseController
{
    @Autowired
    private ILfExecLogService lfExecLogService;

    @PreAuthorize("@ss.hasPermi('liteflow:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(LfExecLog lfExecLog)
    {
        startPage();
        List<LfExecLog> list = lfExecLogService.selectLfExecLogList(lfExecLog);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:log:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfExecLogService.selectLfExecLogById(id));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:log:query')")
    @GetMapping(value = "/request/{requestId}")
    public AjaxResult getByRequestId(@PathVariable String requestId)
    {
        return success(lfExecLogService.selectLfExecLogByRequestId(requestId));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:log:remove')")
    @Log(title = "LiteFlow执行日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfExecLogService.deleteLfExecLogByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:log:remove')")
    @Log(title = "LiteFlow执行日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        lfExecLogService.cleanLfExecLog();
        return success();
    }
}

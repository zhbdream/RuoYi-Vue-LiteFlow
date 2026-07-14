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
import com.ruoyiliteflow.agent.domain.LfAgentModel;
import com.ruoyiliteflow.agent.service.ILfAgentModelService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;

/**
 * Agent 模型配置 CRUD（API Key 加密入库，接口不回传明文）
 */
@RestController
@RequestMapping("/liteflow/agent/model")
public class LfAgentModelController extends BaseController
{
    @Autowired
    private ILfAgentModelService lfAgentModelService;

    @PreAuthorize("@ss.hasPermi('liteflow:agent:list')")
    @GetMapping("/list")
    public TableDataInfo list(LfAgentModel query)
    {
        startPage();
        List<LfAgentModel> list = lfAgentModelService.selectLfAgentModelList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('liteflow:agent:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(lfAgentModelService.selectLfAgentModelById(id));
    }

    @PreAuthorize("@ss.hasAnyPermi('liteflow:agent:add,liteflow:agent:config')")
    @Log(title = "Agent模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody LfAgentModel model)
    {
        model.setCreateBy(getUsername());
        return toAjax(lfAgentModelService.insertLfAgentModel(model));
    }

    @PreAuthorize("@ss.hasAnyPermi('liteflow:agent:edit,liteflow:agent:config')")
    @Log(title = "Agent模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody LfAgentModel model)
    {
        model.setUpdateBy(getUsername());
        return toAjax(lfAgentModelService.updateLfAgentModel(model));
    }

    @PreAuthorize("@ss.hasPermi('liteflow:agent:remove')")
    @Log(title = "Agent模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(lfAgentModelService.deleteLfAgentModelByIds(ids));
    }
}

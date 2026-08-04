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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeBase;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeDoc;
import com.ruoyiliteflow.aikit.platform.service.IAiKnowledgeService;
import com.ruoyiliteflow.common.annotation.Log;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.page.TableDataInfo;
import com.ruoyiliteflow.common.enums.BusinessType;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;

@RestController
@RequestMapping("/aikit/knowledge")
@ConditionalOnProperty(prefix = "ruoyi.ai-kit.platform", name = "enabled", havingValue = "true")
public class AiKnowledgeController extends BaseController
{
    @Autowired
    private IAiKnowledgeService knowledgeService;

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiKnowledgeBase query)
    {
        startPage();
        return getDataTable(knowledgeService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(knowledgeService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:add')")
    @Log(title = "AI知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiKnowledgeBase kb)
    {
        kb.setCreateBy(currentUser());
        return toAjax(knowledgeService.insert(kb));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:edit')")
    @Log(title = "AI知识库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiKnowledgeBase kb)
    {
        kb.setUpdateBy(currentUser());
        return toAjax(knowledgeService.update(kb));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:remove')")
    @Log(title = "AI知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(knowledgeService.deleteByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:query')")
    @GetMapping("/{kbId}/docs")
    public AjaxResult docs(@PathVariable Long kbId)
    {
        List<AiKnowledgeDoc> list = knowledgeService.listDocs(kbId);
        // 列表不回传全文，避免过大
        for (AiKnowledgeDoc d : list)
        {
            if (d.getContentText() != null && d.getContentText().length() > 200)
            {
                d.setContentText(d.getContentText().substring(0, 200) + "...");
            }
        }
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:upload')")
    @Log(title = "AI知识库文档", businessType = BusinessType.INSERT)
    @PostMapping("/{kbId}/upload")
    public AjaxResult upload(@PathVariable Long kbId, @RequestParam("file") MultipartFile file) throws Exception
    {
        return success(knowledgeService.uploadDoc(kbId, file, currentUser()));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:remove')")
    @Log(title = "AI知识库文档", businessType = BusinessType.DELETE)
    @DeleteMapping("/doc/{ids}")
    public AjaxResult removeDocs(@PathVariable Long[] ids)
    {
        return toAjax(knowledgeService.deleteDocs(ids));
    }

    @PreAuthorize("@ss.hasPermi('aikit:knowledge:reindex')")
    @Log(title = "AI知识库重建索引", businessType = BusinessType.UPDATE)
    @PostMapping("/{kbId}/reindex")
    public AjaxResult reindex(@PathVariable Long kbId)
    {
        int chunks = knowledgeService.reindex(kbId);
        return success(Map.of("chunkCount", chunks));
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

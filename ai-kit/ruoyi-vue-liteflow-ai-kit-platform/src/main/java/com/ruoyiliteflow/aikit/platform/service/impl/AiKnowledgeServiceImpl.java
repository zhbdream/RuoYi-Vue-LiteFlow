package com.ruoyiliteflow.aikit.platform.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeBase;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeDoc;
import com.ruoyiliteflow.aikit.platform.knowledge.KnowledgeTextExtractor;
import com.ruoyiliteflow.aikit.platform.knowledge.PlatformKnowledgeRetriever;
import com.ruoyiliteflow.aikit.platform.mapper.AiKnowledgeBaseMapper;
import com.ruoyiliteflow.aikit.platform.mapper.AiKnowledgeChunkMapper;
import com.ruoyiliteflow.aikit.platform.mapper.AiKnowledgeDocMapper;
import com.ruoyiliteflow.aikit.platform.service.IAiKnowledgeService;
import com.ruoyiliteflow.common.config.RuoYiConfig;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.file.FileUploadUtils;

@Service
public class AiKnowledgeServiceImpl implements IAiKnowledgeService
{
    private static final String[] ALLOWED = { "txt", "md", "markdown", "text", "pdf", "docx" };

    @Autowired
    private AiKnowledgeBaseMapper kbMapper;

    @Autowired
    private AiKnowledgeDocMapper docMapper;

    @Autowired
    private AiKnowledgeChunkMapper chunkMapper;

    @Autowired
    private PlatformKnowledgeRetriever knowledgeRetriever;

    @Override
    public List<AiKnowledgeBase> selectList(AiKnowledgeBase query)
    {
        return kbMapper.selectAiKnowledgeBaseList(query);
    }

    @Override
    public AiKnowledgeBase selectById(Long id)
    {
        return kbMapper.selectAiKnowledgeBaseById(id);
    }

    @Override
    public int insert(AiKnowledgeBase kb)
    {
        if (kbMapper.selectAiKnowledgeBaseByCode(kb.getKbCode()) != null)
        {
            throw new ServiceException("知识库编码已存在: " + kb.getKbCode());
        }
        if (StringUtils.isEmpty(kb.getStatus()))
        {
            kb.setStatus("0");
        }
        if (kb.getChunkCount() == null)
        {
            kb.setChunkCount(0);
        }
        return kbMapper.insertAiKnowledgeBase(kb);
    }

    @Override
    public int update(AiKnowledgeBase kb)
    {
        if (kb.getId() == null)
        {
            throw new ServiceException("主键不能为空");
        }
        AiKnowledgeBase db = kbMapper.selectAiKnowledgeBaseById(kb.getId());
        if (db == null)
        {
            throw new ServiceException("知识库不存在");
        }
        if (StringUtils.isNotEmpty(kb.getKbCode()) && !kb.getKbCode().equals(db.getKbCode()))
        {
            AiKnowledgeBase exist = kbMapper.selectAiKnowledgeBaseByCode(kb.getKbCode());
            if (exist != null && !exist.getId().equals(kb.getId()))
            {
                throw new ServiceException("知识库编码已存在: " + kb.getKbCode());
            }
            knowledgeRetriever.removeKb(db.getKbCode());
        }
        return kbMapper.updateAiKnowledgeBase(kb);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids)
    {
        if (ids != null)
        {
            for (Long id : ids)
            {
                AiKnowledgeBase kb = kbMapper.selectAiKnowledgeBaseById(id);
                if (kb != null)
                {
                    knowledgeRetriever.removeKb(kb.getKbCode());
                    chunkMapper.deleteByKbId(id);
                }
            }
            docMapper.deleteByKbIds(ids);
        }
        return kbMapper.deleteAiKnowledgeBaseByIds(ids);
    }

    @Override
    public List<AiKnowledgeDoc> listDocs(Long kbId)
    {
        return docMapper.selectByKbId(kbId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDoc uploadDoc(Long kbId, MultipartFile file, String createBy) throws Exception
    {
        AiKnowledgeBase kb = kbMapper.selectAiKnowledgeBaseById(kbId);
        if (kb == null)
        {
            throw new ServiceException("知识库不存在");
        }
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("文件不能为空");
        }
        String path = FileUploadUtils.upload(RuoYiConfig.getUploadPath() + "/aikit-kb", file, ALLOWED);
        String text = KnowledgeTextExtractor.extract(file);
        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setKbId(kbId);
        doc.setDocName(file.getOriginalFilename());
        doc.setFilePath(path);
        doc.setContentText(text);
        doc.setStatus("0");
        doc.setChunkCount(0);
        doc.setCreateBy(createBy);
        docMapper.insert(doc);
        knowledgeRetriever.reindex(kbId);
        return docMapper.selectById(doc.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDocs(Long[] docIds)
    {
        if (docIds == null || docIds.length == 0)
        {
            return 0;
        }
        Long kbId = null;
        for (Long id : docIds)
        {
            AiKnowledgeDoc doc = docMapper.selectById(id);
            if (doc != null)
            {
                kbId = doc.getKbId();
                chunkMapper.deleteByDocId(id);
            }
        }
        int rows = docMapper.deleteByIds(docIds);
        if (kbId != null)
        {
            knowledgeRetriever.reindex(kbId);
        }
        return rows;
    }

    @Override
    public int reindex(Long kbId)
    {
        return knowledgeRetriever.reindex(kbId);
    }

    @Override
    public List<com.ruoyiliteflow.aicore.spi.KnowledgeHit> search(String kbCode, String query, int maxResults,
            double minScore)
    {
        if (StringUtils.isEmpty(kbCode) || StringUtils.isEmpty(query))
        {
            throw new ServiceException("kbCode 与 query 不能为空");
        }
        AiKnowledgeBase kb = kbMapper.selectAiKnowledgeBaseByCode(kbCode);
        if (kb == null)
        {
            throw new ServiceException("知识库不存在: " + kbCode);
        }
        int max = maxResults <= 0 ? 5 : maxResults;
        double min = minScore <= 0 ? 0.2 : minScore;
        return knowledgeRetriever.searchByCode(kbCode, query, max, min);
    }
}

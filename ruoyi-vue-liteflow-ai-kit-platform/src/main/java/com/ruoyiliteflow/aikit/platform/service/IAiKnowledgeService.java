package com.ruoyiliteflow.aikit.platform.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeBase;
import com.ruoyiliteflow.aikit.platform.domain.AiKnowledgeDoc;

public interface IAiKnowledgeService
{
    List<AiKnowledgeBase> selectList(AiKnowledgeBase query);

    AiKnowledgeBase selectById(Long id);

    int insert(AiKnowledgeBase kb);

    int update(AiKnowledgeBase kb);

    int deleteByIds(Long[] ids);

    List<AiKnowledgeDoc> listDocs(Long kbId);

    AiKnowledgeDoc uploadDoc(Long kbId, MultipartFile file, String createBy) throws Exception;

    int deleteDocs(Long[] docIds);

    int reindex(Long kbId);
}

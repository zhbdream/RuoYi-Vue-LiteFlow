package com.ruoyiliteflow.aicore.spi;

import java.util.List;

/**
 * 按知识库编码检索上下文（platform 提供 DB+向量实现；无 Bean 则跳过）。
 */
public interface KnowledgeRetriever
{
    /**
     * @param knowledgeCodes 知识库业务编码列表（kb_code）
     * @return 拼好的参考资料文本；无命中返回空串
     */
    String retrieveContext(List<String> knowledgeCodes, String query, int maxResults, double minScore);
}

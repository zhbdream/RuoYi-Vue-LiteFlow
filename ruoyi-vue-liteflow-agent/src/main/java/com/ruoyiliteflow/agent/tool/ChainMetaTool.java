package com.ruoyiliteflow.agent.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * Agent Tool：查询链路元数据（不含完整 EL，避免提示词过长）
 */
@Component
public class ChainMetaTool
{
    @Autowired
    private LfChainMapper lfChainMapper;

    @Tool(name = "query_chain_meta", description = "根据 chainName 查询 LiteFlow 链路元数据（描述、状态、版本等）")
    public String queryChainMeta(@ToolParam(name = "chainName", description = "链路 ID，如 orderProcess") String chainName)
    {
        if (StringUtils.isEmpty(chainName))
        {
            return "{\"error\":\"chainName is required\"}";
        }
        LfChain chain = lfChainMapper.selectLfChainByName(chainName.trim());
        if (chain == null)
        {
            return "{\"error\":\"chain not found\",\"chainName\":\"" + chainName + "\"}";
        }
        return JSON.toJSONString(new ChainMetaView(chain));
    }

    public static class ChainMetaView
    {
        public String chainName;
        public String chainDesc;
        public String status;
        public String draftFlag;
        public Integer version;
        public String namespace;

        public ChainMetaView(LfChain chain)
        {
            this.chainName = chain.getChainName();
            this.chainDesc = chain.getChainDesc();
            this.status = chain.getStatus();
            this.draftFlag = chain.getDraftFlag();
            this.version = chain.getVersion();
            this.namespace = chain.getNamespace();
        }
    }
}

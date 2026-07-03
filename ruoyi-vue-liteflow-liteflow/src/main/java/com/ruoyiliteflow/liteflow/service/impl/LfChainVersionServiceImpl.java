package com.ruoyiliteflow.liteflow.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.constant.LiteFlowAuditAction;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.mapper.LfChainVersionMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainAuditService;
import com.ruoyiliteflow.liteflow.service.ILfChainVersionService;

@Service
public class LfChainVersionServiceImpl implements ILfChainVersionService
{
    @Autowired
    private LfChainVersionMapper lfChainVersionMapper;

    @Autowired
    private LfChainMapper lfChainMapper;

    @Autowired
    private ILfChainAuditService lfChainAuditService;

    @Override
    public LfChainVersion selectLfChainVersionById(Long id)
    {
        return lfChainVersionMapper.selectLfChainVersionById(id);
    }

    @Override
    public List<LfChainVersion> selectLfChainVersionList(LfChainVersion query)
    {
        return lfChainVersionMapper.selectLfChainVersionList(query);
    }

    @Override
    public List<LfChainVersion> selectVersionsByChainId(Long chainId)
    {
        if (chainId == null)
        {
            throw new ServiceException("链路ID不能为空");
        }
        LfChainVersion query = new LfChainVersion();
        query.setChainId(chainId);
        return lfChainVersionMapper.selectLfChainVersionList(query);
    }

    @Override
    public void rollbackToVersion(Long versionId, String operateBy)
    {
        LfChainVersion version = lfChainVersionMapper.selectLfChainVersionById(versionId);
        if (version == null)
        {
            throw new ServiceException("版本快照不存在");
        }
        if (StringUtils.isEmpty(version.getElData()))
        {
            throw new ServiceException("版本快照 EL 为空，无法回滚");
        }
        LfChain chain = lfChainMapper.selectLfChainById(version.getChainId());
        if (chain == null)
        {
            throw new ServiceException("链路不存在");
        }
        LfChain before = copyChain(chain);
        chain.setElData(version.getElData());
        chain.setGraphJson(version.getGraphJson());
        chain.setDraftFlag("1");
        chain.setUpdateBy(operateBy);
        lfChainMapper.updateLfChain(chain);
        LfChain after = lfChainMapper.selectLfChainById(chain.getId());
        lfChainAuditService.recordChange(before, after, LiteFlowAuditAction.ROLLBACK, operateBy,
                "回滚至 v" + version.getVersion() + "（草稿，需发布生效）");
    }

    private LfChain copyChain(LfChain source)
    {
        LfChain copy = new LfChain();
        copy.setId(source.getId());
        copy.setChainName(source.getChainName());
        copy.setElData(source.getElData());
        copy.setGraphJson(source.getGraphJson());
        copy.setDraftFlag(source.getDraftFlag());
        copy.setVersion(source.getVersion());
        return copy;
    }
}

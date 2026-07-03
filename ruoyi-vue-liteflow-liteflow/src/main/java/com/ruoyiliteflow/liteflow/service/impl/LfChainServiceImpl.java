package com.ruoyiliteflow.liteflow.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.common.constant.UserConstants;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.constant.LiteFlowAuditAction;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainVersion;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainExportVo;
import com.ruoyiliteflow.liteflow.mapper.LfChainMapper;
import com.ruoyiliteflow.liteflow.mapper.LfChainVersionMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainAuditService;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;
import com.ruoyiliteflow.liteflow.service.ILfChainService;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;

@Service
public class LfChainServiceImpl implements ILfChainService
{
    private static final String DEFAULT_APP = "ruoyi-liteflow";

    @Autowired
    private LfChainMapper lfChainMapper;

    @Autowired
    private LfChainVersionMapper lfChainVersionMapper;

    @Autowired
    private ILfChainAuditService lfChainAuditService;

    @Autowired
    private ILfChainPermissionService lfChainPermissionService;

    @Override
    public LfChain selectLfChainById(Long id)
    {
        return lfChainMapper.selectLfChainById(id);
    }

    @Override
    public LfChain selectLfChainByName(String chainName)
    {
        return lfChainMapper.selectLfChainByName(chainName);
    }

    @Override
    public List<LfChain> selectLfChainList(LfChain lfChain)
    {
        return lfChainMapper.selectLfChainList(lfChain);
    }

    @Override
    public int insertLfChain(LfChain lfChain)
    {
        fillDefaults(lfChain);
        if (!checkChainNameUnique(lfChain))
        {
            throw new ServiceException("新增链路'" + lfChain.getChainName() + "'失败，链路ID已存在");
        }
        int rows = lfChainMapper.insertLfChain(lfChain);
        if (rows > 0)
        {
            LfChain saved = lfChainMapper.selectLfChainByName(lfChain.getChainName());
            lfChainAuditService.recordChange(null, saved, LiteFlowAuditAction.CREATE, lfChain.getCreateBy());
            if (isPublished(lfChain))
            {
                reloadChain(lfChain);
            }
        }
        return rows;
    }

    @Override
    public int updateLfChain(LfChain lfChain)
    {
        fillDefaults(lfChain);
        if (!checkChainNameUnique(lfChain))
        {
            throw new ServiceException("修改链路'" + lfChain.getChainName() + "'失败，链路ID已存在");
        }
        LfChain before = lfChainMapper.selectLfChainById(lfChain.getId());
        if (before != null)
        {
            lfChainPermissionService.assertCanEdit(before.getChainName());
        }
        int rows = lfChainMapper.updateLfChain(lfChain);
        if (rows > 0)
        {
            LfChain after = lfChainMapper.selectLfChainById(lfChain.getId());
            lfChainAuditService.recordChange(before, after, LiteFlowAuditAction.EDIT, lfChain.getUpdateBy());
            if (isPublished(lfChain))
            {
                reloadChain(lfChain);
            }
        }
        return rows;
    }

    @Override
    public int deleteLfChainByIds(Long[] ids, String operateBy)
    {
        for (Long id : ids)
        {
            LfChain chain = lfChainMapper.selectLfChainById(id);
            if (chain != null)
            {
                lfChainAuditService.recordChange(chain, null, LiteFlowAuditAction.DELETE, operateBy);
                LiteflowMetaOperator.removeChain(chain.getChainName());
            }
        }
        return lfChainMapper.deleteLfChainByIds(ids);
    }

    @Override
    public void reloadChain(LfChain lfChain)
    {
        if (lfChain == null || StringUtils.isEmpty(lfChain.getChainName()) || StringUtils.isEmpty(lfChain.getElData()))
        {
            return;
        }
        if (StringUtils.isNotEmpty(lfChain.getRouteEl()) || StringUtils.isNotEmpty(lfChain.getNamespace()))
        {
            LiteFlowChainELBuilder builder = LiteFlowChainELBuilder.createChain()
                .setChainId(lfChain.getChainName())
                .setEL(lfChain.getElData());
            if (StringUtils.isNotEmpty(lfChain.getRouteEl()))
            {
                builder.setRoute(lfChain.getRouteEl());
            }
            if (StringUtils.isNotEmpty(lfChain.getNamespace()))
            {
                builder.setNamespace(lfChain.getNamespace());
            }
            builder.build();
            return;
        }
        LiteflowMetaOperator.reloadOneChain(lfChain.getChainName(), lfChain.getElData());
    }

    @Override
    public void reloadChainByName(String chainName)
    {
        LfChain chain = lfChainMapper.selectLfChainByName(chainName);
        if (chain == null)
        {
            throw new ServiceException("链路不存在: " + chainName);
        }
        reloadChain(chain);
    }

    @Override
    public void publishChain(Long id, String publishBy)
    {
        LfChain chain = lfChainMapper.selectLfChainById(id);
        if (chain == null)
        {
            throw new ServiceException("链路不存在");
        }
        lfChainPermissionService.assertCanEdit(chain.getChainName());
        if (StringUtils.isEmpty(chain.getElData()))
        {
            throw new ServiceException("EL 表达式不能为空，无法发布");
        }
        LfChain before = copyChain(chain);
        int nextVersion = chain.getVersion() == null ? 1 : chain.getVersion() + 1;
        chain.setDraftFlag("0");
        chain.setVersion(nextVersion);
        chain.setUpdateBy(publishBy);
        lfChainMapper.updateLfChain(chain);
        LfChain after = lfChainMapper.selectLfChainById(id);
        lfChainAuditService.recordChange(before, after, LiteFlowAuditAction.PUBLISH, publishBy);
        saveVersionSnapshot(after, publishBy);
        reloadChain(after);
    }

    @Override
    public LfChain cloneChain(Long id, String newChainName, String newChainDesc, String createBy)
    {
        LfChain source = lfChainMapper.selectLfChainById(id);
        if (source == null)
        {
            throw new ServiceException("源链路不存在");
        }
        if (StringUtils.isEmpty(newChainName))
        {
            throw new ServiceException("新链路ID不能为空");
        }
        LfChain target = new LfChain();
        target.setApplicationName(StringUtils.isEmpty(source.getApplicationName()) ? DEFAULT_APP : source.getApplicationName());
        target.setChainName(newChainName.trim());
        target.setChainDesc(StringUtils.isNotEmpty(newChainDesc) ? newChainDesc : source.getChainDesc() + " (克隆)");
        target.setElData(source.getElData());
        target.setGraphJson(source.getGraphJson());
        target.setEnable(source.getEnable());
        target.setStatus(source.getStatus());
        target.setDraftFlag("1");
        target.setVersion(1);
        target.setContextClass(source.getContextClass());
        target.setRouteEl(source.getRouteEl());
        target.setNamespace(source.getNamespace());
        target.setRemark(source.getRemark());
        target.setCreateBy(createBy);
        insertLfChain(target);
        return lfChainMapper.selectLfChainByName(target.getChainName());
    }

    @Override
    public LfChainExportVo exportChain(Long id)
    {
        LfChain chain = lfChainMapper.selectLfChainById(id);
        if (chain == null)
        {
            throw new ServiceException("链路不存在");
        }
        LfChainExportVo vo = new LfChainExportVo();
        vo.setApplicationName(chain.getApplicationName());
        vo.setChainName(chain.getChainName());
        vo.setChainDesc(chain.getChainDesc());
        vo.setElData(chain.getElData());
        vo.setGraphJson(chain.getGraphJson());
        vo.setContextClass(chain.getContextClass());
        vo.setVersion(chain.getVersion());
        vo.setRemark(chain.getRemark());
        vo.setExportTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return vo;
    }

    @Override
    public LfChain importChain(LfChainExportVo exportVo, String createBy)
    {
        if (exportVo == null || StringUtils.isEmpty(exportVo.getChainName()))
        {
            throw new ServiceException("导入数据缺少链路ID");
        }
        if (StringUtils.isEmpty(exportVo.getElData()))
        {
            throw new ServiceException("导入数据缺少 EL 表达式");
        }
        LfChain chain = new LfChain();
        chain.setApplicationName(StringUtils.isEmpty(exportVo.getApplicationName()) ? DEFAULT_APP : exportVo.getApplicationName());
        chain.setChainName(exportVo.getChainName().trim());
        chain.setChainDesc(exportVo.getChainDesc());
        chain.setElData(exportVo.getElData());
        chain.setGraphJson(exportVo.getGraphJson());
        chain.setContextClass(exportVo.getContextClass());
        chain.setRemark(exportVo.getRemark());
        chain.setEnable(1);
        chain.setStatus(UserConstants.NORMAL);
        chain.setDraftFlag("1");
        chain.setVersion(1);
        chain.setCreateBy(createBy);
        fillDefaults(chain);
        if (!checkChainNameUnique(chain))
        {
            throw new ServiceException("导入链路'" + chain.getChainName() + "'失败，链路ID已存在");
        }
        lfChainMapper.insertLfChain(chain);
        LfChain saved = lfChainMapper.selectLfChainByName(chain.getChainName());
        lfChainAuditService.recordChange(null, saved, LiteFlowAuditAction.IMPORT, createBy);
        return saved;
    }

    private LfChain copyChain(LfChain source)
    {
        if (source == null)
        {
            return null;
        }
        LfChain copy = new LfChain();
        copy.setId(source.getId());
        copy.setChainName(source.getChainName());
        copy.setElData(source.getElData());
        copy.setGraphJson(source.getGraphJson());
        copy.setDraftFlag(source.getDraftFlag());
        copy.setVersion(source.getVersion());
        return copy;
    }

    private void saveVersionSnapshot(LfChain chain, String publishBy)
    {
        LfChainVersion version = new LfChainVersion();
        version.setChainId(chain.getId());
        version.setChainName(chain.getChainName());
        version.setVersion(chain.getVersion());
        version.setElData(chain.getElData());
        version.setGraphJson(chain.getGraphJson());
        version.setPublishBy(publishBy);
        version.setRemark("发布快照 v" + chain.getVersion());
        lfChainVersionMapper.insertLfChainVersion(version);
    }

    private void fillDefaults(LfChain lfChain)
    {
        if (StringUtils.isEmpty(lfChain.getApplicationName()))
        {
            lfChain.setApplicationName(DEFAULT_APP);
        }
        if (lfChain.getEnable() == null)
        {
            lfChain.setEnable(UserConstants.NORMAL.equals(lfChain.getStatus()) ? 1 : 0);
        }
        if (StringUtils.isEmpty(lfChain.getStatus()))
        {
            lfChain.setStatus(lfChain.getEnable() != null && lfChain.getEnable() == 1 ? UserConstants.NORMAL : UserConstants.EXCEPTION);
        }
        if (StringUtils.isEmpty(lfChain.getDraftFlag()))
        {
            lfChain.setDraftFlag("0");
        }
        if (lfChain.getVersion() == null)
        {
            lfChain.setVersion(1);
        }
    }

    private boolean isPublished(LfChain lfChain)
    {
        return "0".equals(lfChain.getDraftFlag())
            && UserConstants.NORMAL.equals(lfChain.getStatus())
            && lfChain.getEnable() != null
            && lfChain.getEnable() == 1;
    }

    private boolean checkChainNameUnique(LfChain lfChain)
    {
        Long id = StringUtils.isNull(lfChain.getId()) ? -1L : lfChain.getId();
        LfChain info = lfChainMapper.selectLfChainByName(lfChain.getChainName());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != id.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}

package com.ruoyiliteflow.liteflow.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
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
    @Transactional(rollbackFor = Exception.class)
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
                reloadChain(saved);
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
                reloadChain(after);
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
        String chainName = lfChain.getChainName();
        Chain previous = FlowBus.getChain(chainName);
        Chain snapshot = previous == null ? null : new Chain(chainName);
        if (previous != null)
        {
            copyRuntimeState(previous, snapshot);
        }
        Chain candidate = new Chain(chainName);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive())
        {
            // 数据库提交发生在方法返回之后，提交失败时也必须恢复内存中的生效版本。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCompletion(int status)
                {
                    if (status != STATUS_COMMITTED)
                    {
                        restoreRuntimeChain(chainName, previous, snapshot);
                    }
                }
            });
        }
        try
        {
            // 不使用 setChainId 取出旧对象，避免失败编译提前修改正在使用的规则。
            LiteFlowChainELBuilder builder = LiteFlowChainELBuilder.fromChain(candidate)
                .setEL(lfChain.getElData());
            if (StringUtils.isNotEmpty(lfChain.getRouteEl()))
            {
                builder.setRoute(lfChain.getRouteEl());
            }
            if (StringUtils.isNotEmpty(lfChain.getNamespace()))
            {
                builder.setNamespace(lfChain.getNamespace());
            }
            // 发布必须立即编译主体与路由，不能依赖首次执行时才解析的配置。
            LiteFlowChainELBuilder.buildUnCompileChain(builder.getChain());
            if (previous != null)
            {
                // 父链可能持有旧 Chain 引用，成功时保留该对象身份以延续热更新行为。
                copyRuntimeState(candidate, previous);
                FlowBus.addChain(previous);
            }
        }
        catch (RuntimeException e)
        {
            restoreRuntimeChain(chainName, previous, snapshot);
            throw new ServiceException("链路加载失败: " + e.getMessage());
        }
    }

    /** 恢复加载前的运行状态，避免失败候选及其表达式缓存继续生效。 */
    private void restoreRuntimeChain(String chainName, Chain previous, Chain snapshot)
    {
        if (previous == null)
        {
            if (FlowBus.containChain(chainName))
            {
                FlowBus.removeChain(chainName);
            }
            return;
        }
        Chain current = FlowBus.getChain(chainName);
        if (current != null && (current != previous
                || !Objects.equals(current.getElMd5(), snapshot.getElMd5())))
        {
            // 先清除失败候选的表达式缓存，避免按新 EL 执行时误命中恢复后的旧链。
            FlowBus.removeChain(chainName);
        }
        copyRuntimeState(snapshot, previous);
        FlowBus.addChain(previous);
    }

    /** 只复制本次加载改变的字段，保留原 Chain 对象及线程池等既有运行配置。 */
    private void copyRuntimeState(Chain source, Chain target)
    {
        target.setEl(source.getEl());
        target.setElMd5(source.getElMd5());
        target.setRouteEl(source.getRouteEl());
        target.setNamespace(source.getNamespace());
        target.setConditionList(source.getConditionList());
        target.setRouteItem(source.getRouteItem());
        target.setCompiled(source.isCompiled());
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
    @Transactional(rollbackFor = Exception.class)
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
        reloadChain(chain);
        int nextVersion = chain.getVersion() == null ? 1 : chain.getVersion() + 1;
        chain.setDraftFlag("0");
        chain.setVersion(nextVersion);
        chain.setUpdateBy(publishBy);
        if (lfChainMapper.updateLfChain(chain) != 1)
        {
            throw new ServiceException("链路发布失败，记录已不存在");
        }
        LfChain after = lfChainMapper.selectLfChainById(id);
        lfChainAuditService.recordChange(before, after, LiteFlowAuditAction.PUBLISH, publishBy);
        saveVersionSnapshot(after, publishBy);
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

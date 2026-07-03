package com.ruoyiliteflow.liteflow.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChainPermission;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainPermissionSaveVo;
import com.ruoyiliteflow.liteflow.mapper.LfChainPermissionMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;

@Service
public class LfChainPermissionServiceImpl implements ILfChainPermissionService
{
    @Autowired
    private LfChainPermissionMapper lfChainPermissionMapper;

    @Override
    public List<LfChainPermission> selectByChainName(String chainName)
    {
        return lfChainPermissionMapper.selectByChainName(chainName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePermissions(LfChainPermissionSaveVo saveVo, String operator)
    {
        if (saveVo == null || StringUtils.isEmpty(saveVo.getChainName()))
        {
            throw new ServiceException("链路ID不能为空");
        }
        lfChainPermissionMapper.deleteByChainName(saveVo.getChainName());
        List<LfChainPermission> permissions = saveVo.getPermissions();
        if (permissions == null || permissions.isEmpty())
        {
            return;
        }
        for (LfChainPermission item : permissions)
        {
            if (item.getRoleId() == null)
            {
                continue;
            }
            LfChainPermission row = new LfChainPermission();
            row.setChainName(saveVo.getChainName());
            row.setRoleId(item.getRoleId());
            row.setCanExecute("1".equals(item.getCanExecute()) ? "1" : "0");
            row.setCanEdit("1".equals(item.getCanEdit()) ? "1" : "0");
            row.setCreateBy(operator);
            lfChainPermissionMapper.insertLfChainPermission(row);
        }
    }

    @Override
    public void assertCanExecute(String chainName, boolean bypassChainPermission)
    {
        if (bypassChainPermission || SecurityUtils.isAdmin())
        {
            return;
        }
        if (lfChainPermissionMapper.countByChainName(chainName) == 0)
        {
            return;
        }
        Long[] roleIds = resolveRoleIds();
        if (roleIds.length == 0)
        {
            throw new ServiceException("当前用户无权执行链路: " + chainName);
        }
        if (lfChainPermissionMapper.countExecutePermission(chainName, roleIds) <= 0)
        {
            throw new ServiceException("当前角色无权执行链路: " + chainName);
        }
    }

    @Override
    public void assertCanEdit(String chainName)
    {
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        if (lfChainPermissionMapper.countByChainName(chainName) == 0)
        {
            return;
        }
        Long[] roleIds = resolveRoleIds();
        if (roleIds.length == 0)
        {
            throw new ServiceException("当前用户无权编排链路: " + chainName);
        }
        if (lfChainPermissionMapper.countEditPermission(chainName, roleIds) <= 0)
        {
            throw new ServiceException("当前角色无权编排链路: " + chainName);
        }
    }

    private Long[] resolveRoleIds()
    {
        try
        {
            return SecurityUtils.getLoginUser().getUser().getRoleIds();
        }
        catch (Exception e)
        {
            return new Long[0];
        }
    }
}

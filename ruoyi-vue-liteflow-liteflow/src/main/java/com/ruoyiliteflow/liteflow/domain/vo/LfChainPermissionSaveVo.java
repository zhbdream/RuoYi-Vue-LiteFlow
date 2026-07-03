package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainPermission;

/**
 * 保存链路权限请求
 */
public class LfChainPermissionSaveVo
{
    private String chainName;

    private List<LfChainPermission> permissions;

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public List<LfChainPermission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions(List<LfChainPermission> permissions)
    {
        this.permissions = permissions;
    }
}

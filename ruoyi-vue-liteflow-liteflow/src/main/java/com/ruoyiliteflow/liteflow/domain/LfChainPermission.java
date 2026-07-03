package com.ruoyiliteflow.liteflow.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyiliteflow.common.core.domain.BaseEntity;

/**
 * LiteFlow 链路级权限 lf_chain_permission
 */
public class LfChainPermission extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    private String chainName;

    private Long roleId;

    /** 可执行 0否 1是 */
    private String canExecute;

    /** 可编排编辑 0否 1是 */
    private String canEdit;

    /** 展示用：角色名称 */
    private String roleName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public String getCanExecute()
    {
        return canExecute;
    }

    public void setCanExecute(String canExecute)
    {
        this.canExecute = canExecute;
    }

    public String getCanEdit()
    {
        return canEdit;
    }

    public void setCanEdit(String canEdit)
    {
        this.canEdit = canEdit;
    }

    public String getRoleName()
    {
        return roleName;
    }

    public void setRoleName(String roleName)
    {
        this.roleName = roleName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("chainName", getChainName())
            .append("roleId", getRoleId())
            .append("canExecute", getCanExecute())
            .append("canEdit", getCanEdit())
            .toString();
    }
}

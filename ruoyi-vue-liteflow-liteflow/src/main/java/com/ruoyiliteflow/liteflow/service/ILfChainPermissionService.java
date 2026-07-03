package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.LfChainPermission;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainPermissionSaveVo;

public interface ILfChainPermissionService
{
    List<LfChainPermission> selectByChainName(String chainName);

    void savePermissions(LfChainPermissionSaveVo saveVo, String operator);

    void assertCanExecute(String chainName, boolean bypassChainPermission);

    void assertCanEdit(String chainName);
}

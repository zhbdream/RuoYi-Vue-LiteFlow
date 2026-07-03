package com.ruoyiliteflow.liteflow.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyiliteflow.liteflow.domain.LfChainPermission;

public interface LfChainPermissionMapper
{
    List<LfChainPermission> selectByChainName(String chainName);

    int countByChainName(String chainName);

    int countExecutePermission(@Param("chainName") String chainName, @Param("roleIds") Long[] roleIds);

    int countEditPermission(@Param("chainName") String chainName, @Param("roleIds") Long[] roleIds);

    int deleteByChainName(String chainName);

    int insertLfChainPermission(LfChainPermission permission);
}

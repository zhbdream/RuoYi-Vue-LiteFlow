package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowComponentVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowRouteResultVo;

public interface ILiteFlowExecuteService
{
    LiteFlowExecuteResultVo execute(String chainName, Object param);

    LiteFlowExecuteResultVo execute(String chainName, Object param, String createBy);

    LiteFlowExecuteResultVo execute(String chainName, Object param, String createBy, boolean bypassChainPermission);

    LiteFlowExecuteResultVo executeWithEl(String elData, Object param, String contextClass, String createBy);

    LiteFlowRouteResultVo executeRouteChain(String namespace, Object param, String contextClass, String createBy);

    List<LiteFlowComponentVo> listComponents();

    List<LiteFlowComponentVo> listComponentsWithRefs();

    List<String> findChainsReferencingNode(String nodeId);
}

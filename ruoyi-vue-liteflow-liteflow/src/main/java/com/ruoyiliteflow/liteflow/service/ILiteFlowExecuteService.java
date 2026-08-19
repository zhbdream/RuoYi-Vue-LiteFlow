package com.ruoyiliteflow.liteflow.service;

import java.util.List;
import java.util.function.Consumer;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowComponentVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowRouteResultVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowStreamEventVo;

public interface ILiteFlowExecuteService
{
    LiteFlowExecuteResultVo execute(String chainName, Object param);

    LiteFlowExecuteResultVo execute(String chainName, Object param, String createBy);

    LiteFlowExecuteResultVo execute(String chainName, Object param, String createBy, boolean bypassChainPermission);

    LiteFlowExecuteResultVo executeWithEl(String elData, Object param, String contextClass, String createBy);

    /** 用指定 EL 执行并按 chainName 记日志（试跑用例回归草稿） */
    LiteFlowExecuteResultVo executeWithEl(String chainName, String elData, Object param, String contextClass, String createBy);

    LiteFlowRouteResultVo executeRouteChain(String namespace, Object param, String contextClass, String createBy);

    List<LiteFlowComponentVo> listComponents();

    List<LiteFlowComponentVo> listComponentsWithRefs();

    List<String> findChainsReferencingNode(String nodeId);

    /** 判断链路 EL 是否引用了 Re-Act Agent 类型组件（nodeType=agent） */
    boolean chainContainsAgent(String chainName);

    /** 流式执行：推送 Agent 推理/Tool 事件，最终结果写日志 */
    LiteFlowExecuteResultVo executeStream(String chainName, Object param, String createBy,
            Consumer<LiteFlowStreamEventVo> listener);
}

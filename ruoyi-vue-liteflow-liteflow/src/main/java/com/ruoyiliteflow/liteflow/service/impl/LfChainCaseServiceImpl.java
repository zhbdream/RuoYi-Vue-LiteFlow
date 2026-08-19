package com.ruoyiliteflow.liteflow.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.domain.LfChain;
import com.ruoyiliteflow.liteflow.domain.LfChainCase;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainCaseBatchRunVo;
import com.ruoyiliteflow.liteflow.domain.vo.LfChainCaseRunVo;
import com.ruoyiliteflow.liteflow.domain.vo.LiteFlowExecuteResultVo;
import com.ruoyiliteflow.liteflow.mapper.LfChainCaseMapper;
import com.ruoyiliteflow.liteflow.service.ILfChainCaseService;
import com.ruoyiliteflow.liteflow.service.ILfChainPermissionService;
import com.ruoyiliteflow.liteflow.service.ILfChainService;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;

@Service
public class LfChainCaseServiceImpl implements ILfChainCaseService
{
    private static final int MSG_MAX = 500;

    @Autowired
    private LfChainCaseMapper lfChainCaseMapper;

    @Autowired
    private ILfChainService lfChainService;

    @Autowired
    private ILfChainPermissionService lfChainPermissionService;

    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @Override
    public LfChainCase selectLfChainCaseById(Long id)
    {
        return lfChainCaseMapper.selectLfChainCaseById(id);
    }

    @Override
    public List<LfChainCase> selectLfChainCaseList(LfChainCase query)
    {
        return lfChainCaseMapper.selectLfChainCaseList(query);
    }

    @Override
    public int insertLfChainCase(LfChainCase lfChainCase)
    {
        fillDefaults(lfChainCase);
        validateCase(lfChainCase);
        lfChainPermissionService.assertCanEdit(lfChainCase.getChainName());
        return lfChainCaseMapper.insertLfChainCase(lfChainCase);
    }

    @Override
    public int updateLfChainCase(LfChainCase lfChainCase)
    {
        LfChainCase existing = requireCase(lfChainCase.getId());
        lfChainPermissionService.assertCanEdit(existing.getChainName());
        lfChainCase.setChainName(existing.getChainName());
        fillDefaults(lfChainCase);
        validateCase(lfChainCase);
        return lfChainCaseMapper.updateLfChainCase(lfChainCase);
    }

    @Override
    public int deleteLfChainCaseByIds(Long[] ids)
    {
        if (ids == null)
        {
            return 0;
        }
        for (Long id : ids)
        {
            LfChainCase existing = requireCase(id);
            lfChainPermissionService.assertCanEdit(existing.getChainName());
        }
        return lfChainCaseMapper.deleteLfChainCaseByIds(ids);
    }

    @Override
    public LfChainCaseRunVo runCase(Long id, String operateBy)
    {
        LfChainCase lfChainCase = requireCase(id);
        lfChainPermissionService.assertCanExecute(lfChainCase.getChainName(), false);
        return doRun(lfChainCase, operateBy);
    }

    @Override
    public LfChainCaseBatchRunVo runEnabledByChainName(String chainName, String operateBy)
    {
        if (StringUtils.isEmpty(chainName))
        {
            throw new ServiceException("链路ID不能为空");
        }
        lfChainPermissionService.assertCanExecute(chainName, false);
        List<LfChainCase> cases = lfChainCaseMapper.selectEnabledByChainName(chainName);
        LfChainCaseBatchRunVo batch = new LfChainCaseBatchRunVo();
        batch.setChainName(chainName);
        int passed = 0;
        int failed = 0;
        for (LfChainCase item : cases)
        {
            LfChainCaseRunVo result = doRun(item, operateBy);
            batch.getItems().add(result);
            if (result.isPassed())
            {
                passed++;
            }
            else
            {
                failed++;
            }
        }
        batch.setTotal(cases.size());
        batch.setPassed(passed);
        batch.setFailed(failed);
        return batch;
    }

    private LfChainCaseRunVo doRun(LfChainCase lfChainCase, String operateBy)
    {
        LfChainCaseRunVo vo = new LfChainCaseRunVo();
        vo.setCaseId(lfChainCase.getId());
        vo.setCaseName(lfChainCase.getCaseName());
        LfChain chain = lfChainService.selectLfChainByName(lfChainCase.getChainName());
        if (chain == null)
        {
            return persistRun(lfChainCase, vo, false, false, null, null, "链路不存在: " + lfChainCase.getChainName());
        }
        if (StringUtils.isEmpty(chain.getElData()))
        {
            return persistRun(lfChainCase, vo, false, false, null, null, "EL 表达式为空，无法回归");
        }
        Map<String, Object> param;
        try
        {
            param = parseParam(lfChainCase.getParamJson());
        }
        catch (Exception e)
        {
            return persistRun(lfChainCase, vo, false, false, null, null, "入参 JSON 无法解析");
        }
        LiteFlowExecuteResultVo result;
        try
        {
            result = liteFlowExecuteService.executeWithEl(
                lfChainCase.getChainName(),
                chain.getElData(),
                param,
                chain.getContextClass(),
                operateBy);
        }
        catch (Exception e)
        {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return persistRun(lfChainCase, vo, false, false, null, null, msg);
        }
        boolean executeOk = result.isSuccess();
        boolean expectOk = !"0".equals(lfChainCase.getExpectSuccess());
        boolean successMatch = executeOk == expectOk;
        String step = result.getExecuteStepStr();
        boolean stepMatch = true;
        if (StringUtils.isNotEmpty(lfChainCase.getExpectStepContains()))
        {
            stepMatch = step != null && step.contains(lfChainCase.getExpectStepContains());
        }
        boolean passed = successMatch && stepMatch;
        String message;
        if (!successMatch)
        {
            message = expectOk
                ? ("期望成功但执行失败: " + StringUtils.nvl(result.getMessage(), ""))
                : "期望失败但执行成功";
        }
        else if (!stepMatch)
        {
            message = "步骤未包含「" + lfChainCase.getExpectStepContains() + "」，实际: " + StringUtils.nvl(step, "");
        }
        else
        {
            message = "通过";
        }
        return persistRun(lfChainCase, vo, passed, executeOk, result.getLogId(), step, message);
    }

    private LfChainCaseRunVo persistRun(LfChainCase lfChainCase, LfChainCaseRunVo vo, boolean passed,
            boolean executeSuccess, Long logId, String step, String message)
    {
        vo.setPassed(passed);
        vo.setExecuteSuccess(executeSuccess);
        vo.setLogId(logId);
        vo.setExecuteStepStr(step);
        vo.setMessage(truncate(message));
        LfChainCase patch = new LfChainCase();
        patch.setId(lfChainCase.getId());
        patch.setLastRunSuccess(passed ? "1" : "0");
        patch.setLastRunLogId(logId);
        patch.setLastRunMessage(vo.getMessage());
        lfChainCaseMapper.updateLastRun(patch);
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParam(String paramJson)
    {
        if (StringUtils.isEmpty(paramJson))
        {
            return Collections.emptyMap();
        }
        Object parsed = JSON.parse(paramJson);
        if (parsed == null)
        {
            return Collections.emptyMap();
        }
        if (parsed instanceof Map)
        {
            return (Map<String, Object>) parsed;
        }
        throw new ServiceException("入参必须是 JSON 对象");
    }

    private void fillDefaults(LfChainCase lfChainCase)
    {
        if (StringUtils.isEmpty(lfChainCase.getExpectSuccess()))
        {
            lfChainCase.setExpectSuccess("1");
        }
        if (StringUtils.isEmpty(lfChainCase.getStatus()))
        {
            lfChainCase.setStatus("0");
        }
        if (lfChainCase.getSortOrder() == null)
        {
            lfChainCase.setSortOrder(0);
        }
        if (StringUtils.isEmpty(lfChainCase.getParamJson()))
        {
            lfChainCase.setParamJson("{}");
        }
    }

    private void validateCase(LfChainCase lfChainCase)
    {
        if (StringUtils.isEmpty(lfChainCase.getChainName()))
        {
            throw new ServiceException("链路ID不能为空");
        }
        if (StringUtils.isEmpty(lfChainCase.getCaseName()))
        {
            throw new ServiceException("用例名称不能为空");
        }
        try
        {
            parseParam(lfChainCase.getParamJson());
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("入参 JSON 无法解析");
        }
        if (lfChainService.selectLfChainByName(lfChainCase.getChainName()) == null)
        {
            throw new ServiceException("链路不存在: " + lfChainCase.getChainName());
        }
    }

    private LfChainCase requireCase(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("用例不存在");
        }
        LfChainCase existing = lfChainCaseMapper.selectLfChainCaseById(id);
        if (existing == null)
        {
            throw new ServiceException("用例不存在");
        }
        return existing;
    }

    private String truncate(String message)
    {
        if (message == null)
        {
            return null;
        }
        return message.length() <= MSG_MAX ? message : message.substring(0, MSG_MAX);
    }
}

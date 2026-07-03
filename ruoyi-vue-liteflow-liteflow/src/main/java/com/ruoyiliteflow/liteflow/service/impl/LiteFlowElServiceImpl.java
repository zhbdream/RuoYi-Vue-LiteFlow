package com.ruoyiliteflow.liteflow.service.impl;

import org.springframework.stereotype.Service;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.liteflow.service.ILiteFlowElService;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;

@Service
public class LiteFlowElServiceImpl implements ILiteFlowElService
{
    private static final String VALIDATE_CHAIN_PREFIX = "__el_validate__";

    @Override
    public void validateEl(String elData)
    {
        if (StringUtils.isEmpty(elData))
        {
            throw new ServiceException("EL 表达式不能为空");
        }
        String tempChain = VALIDATE_CHAIN_PREFIX + System.currentTimeMillis();
        try
        {
            LiteflowMetaOperator.reloadOneChain(tempChain, elData.trim());
        }
        catch (Exception e)
        {
            String msg = e.getMessage();
            if (StringUtils.isEmpty(msg))
            {
                msg = "EL 语法校验失败";
            }
            throw new ServiceException("EL 校验失败: " + msg);
        }
        finally
        {
            try
            {
                LiteflowMetaOperator.removeChain(tempChain);
            }
            catch (Exception ignored)
            {
                // ignore cleanup failure
            }
        }
    }
}

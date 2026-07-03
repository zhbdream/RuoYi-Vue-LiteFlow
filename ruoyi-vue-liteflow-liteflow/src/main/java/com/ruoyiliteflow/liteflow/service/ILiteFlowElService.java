package com.ruoyiliteflow.liteflow.service;

/**
 * LiteFlow EL 校验服务
 */
public interface ILiteFlowElService
{
    /**
     * 校验 EL 语法，失败抛出 ServiceException
     */
    void validateEl(String elData);
}

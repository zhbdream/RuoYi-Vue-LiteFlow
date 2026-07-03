package com.ruoyiliteflow.liteflow.domain.context;

/**
 * 决策路由 Demo 上下文
 */
public class RouteUserContext
{
    /** NEW / RETURNING */
    private String userType;

    private String message;

    public String getUserType()
    {
        return userType;
    }

    public void setUserType(String userType)
    {
        this.userType = userType;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}

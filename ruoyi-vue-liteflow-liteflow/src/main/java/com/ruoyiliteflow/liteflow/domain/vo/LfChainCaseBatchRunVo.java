package com.ruoyiliteflow.liteflow.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 链路全部启用用例回归结果
 */
public class LfChainCaseBatchRunVo
{
    private String chainName;

    private int total;

    private int passed;

    private int failed;

    private List<LfChainCaseRunVo> items = new ArrayList<>();

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal(int total)
    {
        this.total = total;
    }

    public int getPassed()
    {
        return passed;
    }

    public void setPassed(int passed)
    {
        this.passed = passed;
    }

    public int getFailed()
    {
        return failed;
    }

    public void setFailed(int failed)
    {
        this.failed = failed;
    }

    public List<LfChainCaseRunVo> getItems()
    {
        return items;
    }

    public void setItems(List<LfChainCaseRunVo> items)
    {
        this.items = items;
    }

    public boolean allPassed()
    {
        return failed == 0;
    }
}

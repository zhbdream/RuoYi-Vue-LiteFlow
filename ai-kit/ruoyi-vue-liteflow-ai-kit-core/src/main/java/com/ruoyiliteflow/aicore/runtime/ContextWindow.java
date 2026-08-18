package com.ruoyiliteflow.aicore.runtime;

import java.util.ArrayList;
import java.util.List;
import com.ruoyiliteflow.aicore.spi.MemoryItem;
import com.ruoyiliteflow.common.utils.StringUtils;

/** 按近似 token 预算从旧到新裁剪记忆，优先保留 summary。 */
public final class ContextWindow
{
    private ContextWindow()
    {
    }

    public static int estimateTokens(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return 0;
        }
        return Math.max(1, (text.length() + 1) / 2);
    }

    public static int estimateItems(List<MemoryItem> items)
    {
        if (items == null || items.isEmpty())
        {
            return 0;
        }
        int n = 0;
        for (MemoryItem m : items)
        {
            n += estimateTokens(m == null ? null : m.getContent());
        }
        return n;
    }

    public static List<MemoryItem> trim(List<MemoryItem> items, int tokenBudget)
    {
        if (items == null || items.isEmpty() || tokenBudget <= 0)
        {
            return items;
        }
        List<MemoryItem> out = new ArrayList<>(items);
        while (out.size() > 1 && estimateItems(out) > tokenBudget)
        {
            int drop = indexToDrop(out);
            out.remove(drop);
        }
        return out;
    }

    private static int indexToDrop(List<MemoryItem> out)
    {
        for (int i = 0; i < out.size(); i++)
        {
            MemoryItem m = out.get(i);
            if (m == null || !"summary".equalsIgnoreCase(m.getMemoryType()))
            {
                return i;
            }
        }
        return 0;
    }
}

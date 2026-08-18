package com.ruoyiliteflow.aicore.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.common.utils.http.HttpUtils;

/**
 * 技能模板渲染与 HTTP 调用（prompt / GET / POST）。
 */
public final class SkillRenderer
{
    private SkillRenderer()
    {
    }

    public static Map<String, String> vars(String principal, String agentCode, String sessionId, String message,
            Map<String, Object> extra)
    {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("principal", nvl(principal));
        vars.put("agentCode", nvl(agentCode));
        vars.put("sessionId", nvl(sessionId));
        vars.put("message", nvl(message));
        if (extra != null)
        {
            for (Map.Entry<String, Object> e : extra.entrySet())
            {
                if (e.getKey() != null)
                {
                    vars.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
            }
        }
        return vars;
    }

    public static String render(String template, Map<String, String> vars)
    {
        if (StringUtils.isEmpty(template))
        {
            return "";
        }
        String out = template;
        if (vars != null)
        {
            for (Map.Entry<String, String> e : vars.entrySet())
            {
                out = out.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }

    public static HttpCall parseHttp(String content)
    {
        HttpCall call = new HttpCall();
        if (StringUtils.isEmpty(content))
        {
            return call;
        }
        String t = content.trim();
        String rest = t;
        if (startsWithWord(t, "POST"))
        {
            call.method = "POST";
            rest = t.substring(4).trim();
        }
        else if (startsWithWord(t, "GET"))
        {
            rest = t.substring(3).trim();
        }
        int nl = indexOfNl(rest);
        if (nl < 0)
        {
            call.url = rest.trim();
        }
        else
        {
            call.url = rest.substring(0, nl).trim();
            call.body = rest.substring(nl + 1).trim();
        }
        return call;
    }

    public static String invokeHttp(String renderedContent)
    {
        HttpCall call = parseHttp(renderedContent);
        if (StringUtils.isEmpty(call.url))
        {
            return "";
        }
        if ("POST".equals(call.method))
        {
            String body = call.body == null ? "" : call.body;
            String ct = looksJson(body) ? MediaType.APPLICATION_JSON_VALUE : MediaType.APPLICATION_FORM_URLENCODED_VALUE;
            return HttpUtils.sendPost(call.url, body, ct);
        }
        return HttpUtils.sendGet(call.url);
    }

    private static boolean startsWithWord(String t, String word)
    {
        int n = word.length();
        if (t.length() < n || !t.regionMatches(true, 0, word, 0, n))
        {
            return false;
        }
        return t.length() == n || Character.isWhitespace(t.charAt(n));
    }

    private static int indexOfNl(String s)
    {
        int n = s.indexOf('\n');
        int r = s.indexOf('\r');
        if (n < 0)
        {
            return r;
        }
        if (r < 0)
        {
            return n;
        }
        return Math.min(n, r);
    }

    private static boolean looksJson(String body)
    {
        String t = body == null ? "" : body.trim();
        return t.startsWith("{") || t.startsWith("[");
    }

    private static String nvl(String s)
    {
        return s == null ? "" : s;
    }

    public static final class HttpCall
    {
        public String method = "GET";
        public String url = "";
        public String body = "";
    }
}

package com.ruoyiliteflow.aicore.support;

import java.util.ArrayList;
import java.util.List;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * 将 ai_tool.input_schema_json（JSON Schema 子集）编进 LangChain4j {@link JsonObjectSchema}。
 */
public final class ToolJsonSchemas
{
    private ToolJsonSchemas()
    {
    }

    public static JsonObjectSchema parse(String schemaJson)
    {
        if (StringUtils.isEmpty(schemaJson))
        {
            return JsonObjectSchema.builder().build();
        }
        try
        {
            JSONObject root = JSON.parseObject(schemaJson);
            if (root == null)
            {
                return JsonObjectSchema.builder().build();
            }
            JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
            String rootDesc = root.getString("description");
            if (StringUtils.isNotEmpty(rootDesc))
            {
                builder.description(rootDesc);
            }
            JSONObject properties = root.getJSONObject("properties");
            if (properties != null && !properties.isEmpty())
            {
                for (String name : properties.keySet())
                {
                    if (StringUtils.isEmpty(name))
                    {
                        continue;
                    }
                    Object raw = properties.get(name);
                    JSONObject prop = raw instanceof JSONObject ? (JSONObject) raw : new JSONObject();
                    addProperty(builder, name, prop);
                }
            }
            List<String> required = toStringList(root.get("required"));
            if (!required.isEmpty())
            {
                builder.required(required);
            }
            return builder.build();
        }
        catch (Exception ignored)
        {
            return JsonObjectSchema.builder().build();
        }
    }

    private static void addProperty(JsonObjectSchema.Builder builder, String name, JSONObject prop)
    {
        String type = prop == null ? "string" : prop.getString("type");
        if (StringUtils.isEmpty(type))
        {
            type = "string";
        }
        String description = prop == null ? null : prop.getString("description");
        switch (type)
        {
            case "integer" -> builder.addProperty(name, integerSchema(description));
            case "number" -> builder.addProperty(name, numberSchema(description));
            case "boolean" -> builder.addProperty(name, booleanSchema(description));
            case "array" -> builder.addProperty(name, arraySchema(description));
            case "object" -> builder.addProperty(name, objectSchema(description));
            default -> builder.addProperty(name, stringSchema(description));
        }
    }

    private static JsonStringSchema stringSchema(String description)
    {
        JsonStringSchema.Builder b = JsonStringSchema.builder();
        if (StringUtils.isNotEmpty(description))
        {
            b.description(description);
        }
        return b.build();
    }

    private static JsonIntegerSchema integerSchema(String description)
    {
        JsonIntegerSchema.Builder b = JsonIntegerSchema.builder();
        if (StringUtils.isNotEmpty(description))
        {
            b.description(description);
        }
        return b.build();
    }

    private static JsonNumberSchema numberSchema(String description)
    {
        JsonNumberSchema.Builder b = JsonNumberSchema.builder();
        if (StringUtils.isNotEmpty(description))
        {
            b.description(description);
        }
        return b.build();
    }

    private static JsonBooleanSchema booleanSchema(String description)
    {
        JsonBooleanSchema.Builder b = JsonBooleanSchema.builder();
        if (StringUtils.isNotEmpty(description))
        {
            b.description(description);
        }
        return b.build();
    }

    private static JsonArraySchema arraySchema(String description)
    {
        JsonArraySchema.Builder b = JsonArraySchema.builder().items(JsonStringSchema.builder().build());
        if (StringUtils.isNotEmpty(description))
        {
            b.description(description);
        }
        return b.build();
    }

    private static JsonObjectSchema objectSchema(String description)
    {
        JsonObjectSchema.Builder b = JsonObjectSchema.builder();
        if (StringUtils.isNotEmpty(description))
        {
            b.description(description);
        }
        return b.build();
    }

    private static List<String> toStringList(Object raw)
    {
        List<String> out = new ArrayList<>();
        if (raw instanceof JSONArray arr)
        {
            for (int i = 0; i < arr.size(); i++)
            {
                String v = arr.getString(i);
                if (StringUtils.isNotEmpty(v))
                {
                    out.add(v);
                }
            }
        }
        return out;
    }
}

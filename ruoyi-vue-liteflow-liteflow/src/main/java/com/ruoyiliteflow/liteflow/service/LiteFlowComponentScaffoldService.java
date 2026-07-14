package com.ruoyiliteflow.liteflow.service;

import org.springframework.stereotype.Service;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.StringUtils;

/**
 * 生成 LiteFlow 组件 Java 源码脚手架
 */
@Service
public class LiteFlowComponentScaffoldService
{
    public String generate(String nodeId, String nodeType, String packageName, String style)
    {
        if (StringUtils.isEmpty(nodeId) || !nodeId.matches("^[A-Za-z][A-Za-z0-9_]*$"))
        {
            throw new ServiceException("nodeId 必须以字母开头，仅含字母数字下划线");
        }
        String pkg = StringUtils.isNotEmpty(packageName) ? packageName.trim()
                : "com.ruoyiliteflow.liteflow.component";
        if (!pkg.matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$"))
        {
            throw new ServiceException("包名不合法");
        }
        String type = StringUtils.isEmpty(nodeType) ? "common" : nodeType.trim().toLowerCase();
        boolean declarative = "declarative".equalsIgnoreCase(style);
        String className = Character.toUpperCase(nodeId.charAt(0)) + nodeId.substring(1) + "Component";
        if (declarative)
        {
            return buildDeclarative(pkg, className, nodeId, type);
        }
        return buildEnherited(pkg, className, nodeId, type);
    }

    private String buildEnherited(String pkg, String className, String nodeId, String type)
    {
        String base;
        String method;
        String ret = "";
        switch (type)
        {
            case "boolean":
                base = "NodeBooleanComponent";
                method = "    @Override\n    public boolean processBoolean()\n    {\n        // TODO\n        return true;\n    }\n";
                break;
            case "switch":
                base = "NodeSwitchComponent";
                method = "    @Override\n    public String processSwitch()\n    {\n        // TODO return target nodeId\n        return \"\";\n    }\n";
                break;
            case "for":
                base = "NodeForComponent";
                method = "    @Override\n    public int processFor()\n    {\n        // TODO return loop count\n        return 0;\n    }\n";
                break;
            default:
                base = "NodeComponent";
                method = "    @Override\n    public void process()\n    {\n        // TODO\n    }\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.springframework.stereotype.Component;\n");
        sb.append("import com.yomahub.liteflow.annotation.LiteflowComponent;\n");
        sb.append("import com.yomahub.liteflow.core.").append(base).append(";\n\n");
        sb.append("/**\n * LiteFlow 组件脚手架 — ").append(nodeId).append("\n */\n");
        sb.append("@LiteflowComponent(\"").append(nodeId).append("\")\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append(" extends ").append(base).append("\n{\n");
        sb.append(method);
        sb.append("}\n");
        return sb.toString();
    }

    private String buildDeclarative(String pkg, String className, String nodeId, String type)
    {
        String enumType;
        String methodEnum;
        String retType;
        String body;
        switch (type)
        {
            case "boolean":
                enumType = "BOOLEAN";
                methodEnum = "PROCESS_BOOLEAN";
                retType = "boolean";
                body = "        // TODO\n        return true;\n";
                break;
            case "switch":
                enumType = "SWITCH";
                methodEnum = "PROCESS_SWITCH";
                retType = "String";
                body = "        // TODO return target nodeId\n        return \"\";\n";
                break;
            case "for":
                enumType = "FOR";
                methodEnum = "PROCESS_FOR";
                retType = "int";
                body = "        // TODO return loop count\n        return 0;\n";
                break;
            default:
                enumType = "COMMON";
                methodEnum = "PROCESS";
                retType = "void";
                body = "        // TODO\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.springframework.stereotype.Component;\n");
        sb.append("import com.yomahub.liteflow.annotation.LiteflowComponent;\n");
        sb.append("import com.yomahub.liteflow.annotation.LiteflowMethod;\n");
        sb.append("import com.yomahub.liteflow.core.NodeComponent;\n");
        sb.append("import com.yomahub.liteflow.enums.LiteFlowMethodEnum;\n");
        sb.append("import com.yomahub.liteflow.enums.NodeTypeEnum;\n\n");
        sb.append("/**\n * 声明式 LiteFlow 组件脚手架 — ").append(nodeId).append("\n */\n");
        sb.append("@LiteflowComponent(value = \"").append(nodeId).append("\", name = \"").append(nodeId).append("\")\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append("\n{\n");
        sb.append("    @LiteflowMethod(value = LiteFlowMethodEnum.").append(methodEnum);
        sb.append(", nodeType = NodeTypeEnum.").append(enumType).append(")\n");
        sb.append("    public ").append(retType).append(" process(NodeComponent bindCmp)\n    {\n");
        sb.append(body);
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}

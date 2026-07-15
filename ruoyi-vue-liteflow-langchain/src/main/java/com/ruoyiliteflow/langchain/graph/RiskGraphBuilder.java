package com.ruoyiliteflow.langchain.graph;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.langchain.domain.Lc4jRiskContext;
import com.ruoyiliteflow.langchain.tool.RiskContextTool;
import dev.langchain4j.model.chat.ChatModel;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 构建风控 StateGraph：gatherFacts -> llmAnalyze -> routeByLevel -> (escalate|pass) -> END
 */
public final class RiskGraphBuilder
{
    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "(?:风险等级|risk\\s*level)\\s*[:：]?\\s*(HIGH|MEDIUM|LOW)",
            Pattern.CASE_INSENSITIVE);

    private RiskGraphBuilder()
    {
    }

    public static StateGraph<RiskGraphState> build(ChatModel chatModel, Lc4jRiskContext ctx) throws Exception
    {
        RiskContextTool tool = new RiskContextTool(ctx);

        NodeAction<RiskGraphState> gatherFacts = state -> {
            String facts = tool.readOrderRiskContext();
            return Map.of(
                    RiskGraphState.FACTS_KEY, facts,
                    RiskGraphState.MESSAGES_KEY, "gatherFacts: " + facts
            );
        };

        NodeAction<RiskGraphState> llmAnalyze = state -> {
            String prompt = """
                    你是电商支付前的风控分析助理，只做风险研判与建议，不要编造未给出的事实。
                    订单事实：%s
                    输出要求：
                    1) 先给风险等级：LOW / MEDIUM / HIGH 之一（格式：风险等级：XXX）
                    2) 再用 2~4 句中文说明理由与建议
                    """.formatted(state.facts());
            String analysis = chatModel.chat(prompt);
            String level = parseRiskLevel(analysis);
            return Map.of(
                    RiskGraphState.ANALYSIS_KEY, analysis,
                    RiskGraphState.LEVEL_KEY, level,
                    RiskGraphState.MESSAGES_KEY, "llmAnalyze: level=" + level
            );
        };

        NodeAction<RiskGraphState> escalate = state -> Map.of(
                RiskGraphState.MESSAGES_KEY,
                "escalate: HIGH 风险，建议人工复核或拒绝自动放行"
        );

        NodeAction<RiskGraphState> pass = state -> Map.of(
                RiskGraphState.MESSAGES_KEY,
                "pass: 风险 " + state.riskLevel() + "，可按业务策略继续支付流程"
        );

        return new StateGraph<>(RiskGraphState.SCHEMA, RiskGraphState::new)
                .addNode("gatherFacts", node_async(gatherFacts))
                .addNode("llmAnalyze", node_async(llmAnalyze))
                .addNode("escalate", node_async(escalate))
                .addNode("pass", node_async(pass))
                .addEdge(START, "gatherFacts")
                .addEdge("gatherFacts", "llmAnalyze")
                .addConditionalEdges("llmAnalyze",
                        edge_async(state -> "HIGH".equalsIgnoreCase(state.riskLevel()) ? "escalate" : "pass"),
                        Map.of("escalate", "escalate", "pass", "pass"))
                .addEdge("escalate", END)
                .addEdge("pass", END);
    }

    static String parseRiskLevel(String reply)
    {
        if (StringUtils.isEmpty(reply))
        {
            return "MEDIUM";
        }
        Matcher m = LEVEL_PATTERN.matcher(reply);
        if (m.find())
        {
            return m.group(1).toUpperCase();
        }
        String upper = reply.toUpperCase();
        if (upper.contains("HIGH") || reply.contains("高风险"))
        {
            return "HIGH";
        }
        if (upper.contains("LOW") || reply.contains("低风险"))
        {
            return "LOW";
        }
        return "MEDIUM";
    }
}
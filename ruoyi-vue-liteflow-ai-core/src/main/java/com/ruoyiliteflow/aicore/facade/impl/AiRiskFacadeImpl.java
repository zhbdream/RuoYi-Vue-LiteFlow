package com.ruoyiliteflow.aicore.facade.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyiliteflow.aicore.facade.IAiRiskFacade;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeRequest;
import com.ruoyiliteflow.aicore.model.RiskAnalyzeResult;
import com.ruoyiliteflow.aicore.risk.RiskAssistant;
import com.ruoyiliteflow.aicore.risk.RiskOrderContext;
import com.ruoyiliteflow.aicore.spi.AiQuotaGuard;
import com.ruoyiliteflow.aicore.support.AiChatModelFactory;
import com.ruoyiliteflow.common.utils.StringUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

@Service
public class AiRiskFacadeImpl implements IAiRiskFacade
{
    private static final Logger log = LoggerFactory.getLogger(AiRiskFacadeImpl.class);
    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "(?:风险等级|risk\\s*level)\\s*[:：]?\\s*(HIGH|MEDIUM|LOW)",
            Pattern.CASE_INSENSITIVE);

    @Autowired
    private AiChatModelFactory chatModelFactory;

    @Autowired(required = false)
    private AiQuotaGuard quotaGuard;

    @Value("${ruoyi.ai.quota.dimension.risk:agent:risk}")
    private String quotaDimension;

    @Override
    public RiskAnalyzeResult analyze(RiskAnalyzeRequest request)
    {
        if (quotaGuard != null)
        {
            String principal = StringUtils.isEmpty(request.getPrincipal()) ? "anonymous" : request.getPrincipal();
            quotaGuard.assertWithinQuota(principal, quotaDimension);
        }

        RiskOrderContext ctx = new RiskOrderContext();
        ctx.setOrderId(request.getOrderId());
        ctx.setUserId(request.getUserId());
        ctx.setUserType(request.getUserType());
        ctx.setAmount(request.getAmount());
        ctx.setScene(request.getScene());

        ChatModel model = chatModelFactory.createChatModel();
        RiskAssistant assistant = AiServices.builder(RiskAssistant.class)
                .chatModel(model)
                .tools(ctx)
                .build();

        String userMessage = "请分析以下待支付订单风险：orderId=" + ctx.getOrderId()
                + ", userId=" + ctx.getUserId()
                + ", userType=" + ctx.getUserType()
                + ", amount=" + ctx.getAmount()
                + ", scene=" + ctx.getScene()
                + "。如需细节可调用工具 read_order_risk_context。";

        log.info("ai-core risk analyze: orderId={}", ctx.getOrderId());
        String analysis = assistant.analyze(userMessage);
        return new RiskAnalyzeResult(parseRiskLevel(analysis), analysis);
    }

    private static String parseRiskLevel(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return "MEDIUM";
        }
        Matcher m = LEVEL_PATTERN.matcher(text);
        if (m.find())
        {
            return m.group(1).toUpperCase();
        }
        return "MEDIUM";
    }
}

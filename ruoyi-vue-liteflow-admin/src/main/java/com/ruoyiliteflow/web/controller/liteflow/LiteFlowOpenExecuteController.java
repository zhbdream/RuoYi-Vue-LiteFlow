package com.ruoyiliteflow.web.controller.liteflow;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.ruoyiliteflow.common.annotation.Anonymous;
import com.ruoyiliteflow.common.core.controller.BaseController;
import com.ruoyiliteflow.common.core.domain.AjaxResult;
import com.ruoyiliteflow.common.core.domain.model.LoginUser;
import com.ruoyiliteflow.common.exception.ServiceException;
import com.ruoyiliteflow.common.utils.SecurityUtils;
import com.ruoyiliteflow.common.utils.StringUtils;
import com.ruoyiliteflow.framework.config.properties.LiteFlowOpenApiProperties;
import com.ruoyiliteflow.framework.security.filter.LiteFlowOpenApiAuthFilter;
import com.ruoyiliteflow.liteflow.service.ILiteFlowExecuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * LiteFlow 对外开放执行 API（API Key 或 Token 鉴权，见 LiteFlowOpenApiAuthFilter）
 */
@Anonymous
@Tag(name = "LiteFlow开放API", description = "外部系统触发链路执行，鉴权方式：X-LiteFlow-Api-Key 或 Bearer Token")
@RestController
@RequestMapping("/liteflow/open")
public class LiteFlowOpenExecuteController extends BaseController
{
    @Autowired
    private ILiteFlowExecuteService liteFlowExecuteService;

    @Autowired
    private LiteFlowOpenApiProperties openApiProperties;

    @Operation(summary = "开放 API 说明", description = "返回开放 API 是否启用及 API Key 请求头名称（不含密钥本身）")
    @GetMapping("/info")
    public AjaxResult info()
    {
        return success(Map.of(
                "enabled", openApiProperties.isEnabled(),
                "headerName", openApiProperties.getHeaderName(),
                "allowAgentChains", openApiProperties.isAllowAgentChains(),
                "executePath", "POST /liteflow/open/execute/{chainName}",
                "auth", "X-LiteFlow-Api-Key 或 Bearer Token（需 liteflow:open:execute 权限）"));
    }

    @Operation(summary = "执行链路（开放 API）", description = "仅已发布且启用的链路可执行；含 Agent 的链路默认禁止；执行记录写入 lf_exec_log")
    @PostMapping("/execute/{chainName}")
    public AjaxResult execute(
            @Parameter(description = "链路 ID，如 helloChain、orderProcess") @PathVariable String chainName,
            @RequestBody(required = false) Map<String, Object> param,
            HttpServletRequest request)
    {
        if (liteFlowExecuteService.chainContainsAgent(chainName) && !openApiProperties.isAgentChainAllowed(chainName))
        {
            throw new ServiceException("开放 API 默认禁止执行含 Agent 的链路，请改用后台试跑，或将链路加入 liteflow.open-api.allow-agent-chain-names，或设置 allow-agent-chains=true");
        }
        String createBy = resolveCreateBy(request);
        boolean bypassChainPermission = "api-key".equals(request.getAttribute(LiteFlowOpenApiAuthFilter.OPEN_API_AUTH_ATTR));
        return success(liteFlowExecuteService.execute(chainName, param, createBy, bypassChainPermission));
    }

    private String resolveCreateBy(HttpServletRequest request)
    {
        LoginUser loginUser = getLoginUserSafely();
        if (loginUser != null && StringUtils.isNotEmpty(loginUser.getUsername()))
        {
            return loginUser.getUsername();
        }
        return "open-api";
    }

    private LoginUser getLoginUserSafely()
    {
        Authentication authentication = SecurityUtils.getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser)
        {
            return loginUser;
        }
        return null;
    }
}

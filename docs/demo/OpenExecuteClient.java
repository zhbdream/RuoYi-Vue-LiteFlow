import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * LiteFlow 开放执行 API 最小客户端（JDK 11+，无第三方依赖）。
 *
 * <pre>
 *   set LITEFLOW_BASE_URL=http://localhost:8080
 *   set LITEFLOW_API_KEY=ruoyi-liteflow-open-key-change-me
 *   java OpenExecuteClient.java
 *   java OpenExecuteClient.java orderProcess "{\"userId\":1001,\"skuId\":\"SKU-001\",\"quantity\":2,\"payType\":\"wechat\"}"
 * </pre>
 *
 * 含 Agent 的链路默认禁止；按链路放行见 application.yml {@code liteflow.open-api.allow-agent-chain-names}。
 */
public class OpenExecuteClient
{
    public static void main(String[] args) throws Exception
    {
        String base = env("LITEFLOW_BASE_URL", "http://localhost:8080");
        String apiKey = env("LITEFLOW_API_KEY", "ruoyi-liteflow-open-key-change-me");
        String chain = args.length > 0 ? args[0] : "helloChain";
        String body = args.length > 1 ? args[1] : "{\"name\":\"RuoYi\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(base) + "/liteflow/open/execute/" + chain))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("X-LiteFlow-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("HTTP " + response.statusCode());
        System.out.println(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            System.exit(1);
        }
    }

    private static String env(String key, String fallback)
    {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static String trimSlash(String base)
    {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}

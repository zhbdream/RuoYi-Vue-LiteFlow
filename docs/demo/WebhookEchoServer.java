import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.sun.net.httpserver.HttpServer;

/**
 * 本地 Webhook 接收器：打印请求并校验 HMAC。加 {@code --fail} 时始终返回 500，用于观察重试。
 *
 * <pre>
 *   java WebhookEchoServer.java 9099
 *   java WebhookEchoServer.java 9099 --fail
 * </pre>
 *
 * 密钥与后台 {@code liteflow.webhook.secret} / {@code LITEFLOW_WEBHOOK_SECRET} 保持一致。
 * 链路管理填写回调：{@code http://127.0.0.1:9099/hook}（Docker 内请用宿主机可达地址）。
 */
public class WebhookEchoServer
{
    public static void main(String[] args) throws Exception
    {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9099;
        boolean fail = false;
        for (String arg : args)
        {
            if ("--fail".equals(arg))
            {
                fail = true;
            }
        }
        String secret = env("LITEFLOW_WEBHOOK_SECRET", "");
        boolean alwaysFail = fail;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/hook", exchange -> {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            String body = new String(raw, StandardCharsets.UTF_8);
            String signature = exchange.getRequestHeaders().getFirst("X-LiteFlow-Signature");
            String verify = verify(secret, raw, signature);
            System.out.println("---- webhook ----");
            System.out.println("signature: " + signature);
            System.out.println("verify:    " + verify);
            System.out.println(body);
            int status = alwaysFail ? 500 : 200;
            byte[] resp = (alwaysFail ? "{\"ok\":false}" : "{\"ok\":true}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, resp.length);
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(resp);
            }
        });
        server.start();
        System.out.println("Webhook echo on http://127.0.0.1:" + port + "/hook"
                + (alwaysFail ? " (always 500)" : "")
                + (secret.isEmpty() ? " (no secret, skip HMAC)" : " (HMAC enabled)"));
    }

    private static String verify(String secret, byte[] body, String signature) throws IOException
    {
        if (secret == null || secret.isBlank())
        {
            return "skipped";
        }
        if (signature == null || !signature.startsWith("sha256="))
        {
            return "missing";
        }
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expect = "sha256=" + toHex(mac.doFinal(body));
            return MessageDigest.isEqual(expect.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)) ? "ok" : "mismatch";
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }

    private static String toHex(byte[] hash)
    {
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash)
        {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static String env(String key, String fallback)
    {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}

package com.chaos.nation.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** AI 工作器核心：通过 HTTPS 调用 DeepSeek 兼容接口；API Key 从构造参数注入（来自环境变量）， 绝不写入仓库/日志/数据库/客户端。支持超时、重试预算与熔断。 */
public final class AiClient {
  /** 厂商标识固定为 DeepSeek；默认模型配置键 deepseek-flash-4.1（存在性未经核实，仅作配置）。 */
  public static final String VENDOR = "DeepSeek";

  public static final String DEFAULT_MODEL_KEY = "deepseek-flash-4.1";

  private final HttpClient http;
  private final String baseUrl;
  private final String apiKey;
  private final String model;
  private final int maxRetries;
  private final CircuitBreaker breaker;

  public AiClient(
      String baseUrl, String apiKey, String model, int maxRetries, CircuitBreaker breaker) {
    this(baseUrl, apiKey, model, maxRetries, breaker, null);
  }

  /** 可注入的传输函数，便于测试（不发起真实网络）。 */
  public interface Transport {
    String call(String prompt, String model, String key) throws Exception;
  }

  private final Transport transport;

  public AiClient(
      String baseUrl,
      String apiKey,
      String model,
      int maxRetries,
      CircuitBreaker breaker,
      Transport transport) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey == null ? "" : apiKey;
    this.model = model == null ? DEFAULT_MODEL_KEY : model;
    this.maxRetries = maxRetries;
    this.breaker = breaker;
    this.transport = transport;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  /** 调用模型并返回原始响应文本；熔断打开时直接拒绝。 */
  public String complete(String systemPrompt, String userPrompt) throws AiUnavailableException {
    if (apiKey.isBlank() || breaker.isOpen()) {
      throw new AiUnavailableException("AI 不可用（未配置密钥或熔断打开）");
    }
    AiUnavailableException last = null;
    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        String result =
            transport != null
                ? transport.call(systemPrompt + "\n" + userPrompt, model, apiKey)
                : callHttp(systemPrompt, userPrompt);
        breaker.recordSuccess();
        return result;
      } catch (Exception e) {
        breaker.recordFailure();
        last = new AiUnavailableException("AI 调用失败: " + e.getClass().getSimpleName());
      }
    }
    throw last;
  }

  private String callHttp(String systemPrompt, String userPrompt) throws Exception {
    String body =
        """
        {"model":"%s","messages":[{"role":"system","content":%s},{"role":"user","content":%s}]}
        """
            .formatted(model, jsonEscape(systemPrompt), jsonEscape(userPrompt));
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 400) {
      throw new AiUnavailableException("HTTP " + response.statusCode());
    }
    return response.body();
  }

  private static String jsonEscape(String s) {
    return "\""
        + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        + "\"";
  }

  public static final class AiUnavailableException extends Exception {
    public AiUnavailableException(String msg) {
      super(msg);
    }
  }
}

package com.chaos.nation.ai;

/** AI 工作器入口。启动只警告、不阻断离线玩法： 缺少 NATION_AI_KEY 或模型不可达时，NPC 由插件侧确定性行为树接管。 */
public final class AiWorkerMain {
  public static void main(String[] args) {
    String key = System.getenv("NATION_AI_KEY");
    String baseUrl =
        System.getenv()
            .getOrDefault("NATION_AI_BASE_URL", "https://api.deepseek.com/v1/chat/completions");
    String model = System.getenv().getOrDefault("NATION_AI_MODEL", AiClient.DEFAULT_MODEL_KEY);

    if (key == null || key.isBlank()) {
      System.err.println("[AI-WARN] NATION_AI_KEY 未设置，工作器以离线模式启动；NPC 使用确定性行为树。");
    } else {
      System.out.println(
          "[AI] 厂商=" + AiClient.VENDOR + " 模型配置键=" + model + "（注意：模型名可用性与接口可达性需在部署前由用户核实）");
    }
    System.out.println("[AI] Base URL=" + baseUrl + "（密钥绝不写入仓库或日志）");

    // 长期运行由外部进程管理；此处保持存活，真实调度由插件通过队列驱动。
    Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("[AI] 工作器关闭")));
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

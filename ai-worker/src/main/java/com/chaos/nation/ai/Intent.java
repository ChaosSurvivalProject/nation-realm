package com.chaos.nation.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** AI 只能返回受约束的“意图 JSON”。服务端收到后仍要再次验证距离/权限/成本/冷却/安全。 本类用 Jackson 严格绑定；未知操作在服务端拒绝。 */
public final class Intent {
  public enum Action {
    MOVE,
    GATHER,
    BUILD,
    EAT,
    REST,
    DEFEND,
    IDLE
  }

  @JsonProperty("action")
  public Action action;

  @JsonProperty("reason")
  public String reason;

  @JsonProperty("target_chunk")
  public int[] targetChunk;

  @JsonProperty("priority")
  public int priority;

  /** 模型原始输出中若含这些字段，会被服务端安全层拒绝（越权尝试）。 */
  public static boolean containsForbiddenField(String rawJson) {
    String lower = rawJson.toLowerCase();
    return lower.contains("op")
        || lower.contains("console")
        || lower.contains("permission")
        || lower.contains("api_key")
        || lower.contains("apikey")
        || lower.contains("database")
        || lower.contains("shell");
  }

  public static boolean isActionAllowed(Action action) {
    return action != null && List.of(Action.values()).contains(action);
  }
}

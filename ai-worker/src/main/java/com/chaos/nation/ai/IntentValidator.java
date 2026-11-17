package com.chaos.nation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 严格校验模型返回的意图 JSON：必须是合法 JSON、action 在白名单、含 forbidden 字段直接拒绝。 校验通过后才交给服务端状态机做二次验证。 */
public final class IntentValidator {
  private final ObjectMapper mapper = new ObjectMapper();

  public sealed interface Result permits Result.Valid, Result.Rejected {
    record Valid(Intent intent) implements Result {}

    record Rejected(String reason) implements Result {}
  }

  public Result validate(String rawJson) {
    if (rawJson == null || rawJson.isBlank()) {
      return new Result.Rejected("空响应");
    }
    if (Intent.containsForbiddenField(rawJson)) {
      return new Result.Rejected("检测到越权字段");
    }
    try {
      JsonNode node = mapper.readTree(rawJson);
      if (!node.isObject()) return new Result.Rejected("非对象 JSON");
      String actionName = node.path("action").asText("");
      Intent.Action action;
      try {
        action = Intent.Action.valueOf(actionName.toUpperCase());
      } catch (IllegalArgumentException e) {
        return new Result.Rejected("未知 action: " + actionName);
      }
      // 将 action 规范化为大写枚举名，避免 Jackson 大小写敏感导致解析失败。
      ((ObjectNode) node).put("action", action.name());
      Intent intent = mapper.treeToValue(node, Intent.class);
      intent.action = action;
      if (!Intent.isActionAllowed(intent.action)) {
        return new Result.Rejected("action 不在白名单");
      }
      return new Result.Valid(intent);
    } catch (Exception e) {
      return new Result.Rejected("JSON 解析失败");
    }
  }
}

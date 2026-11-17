package com.chaos.nation.protocol;

/** 领域错误码。客户端据此处置失败，服务端绝不向玩家暴露 Java 堆栈或数据库细节。 code 值稳定，一旦发布不得复用或改变含义。 */
public enum ErrorCode {
  OK(0, "成功"),
  PROTOCOL_INCOMPATIBLE(1001, "协议主版本不兼容"),
  MALFORMED_PACKET(1002, "封包格式错误"),
  PACKET_TOO_LARGE(1003, "封包过大"),
  UNKNOWN_MESSAGE(1004, "未知消息类型"),
  RATE_LIMITED(1005, "请求过于频繁"),

  NO_PERMISSION(2001, "没有权限"),
  NOT_MEMBER(2002, "不是该国成员"),
  NATION_NOT_FOUND(2003, "国家不存在"),
  ALREADY_MEMBER(2004, "已经是成员"),
  CAPITAL_NOT_SET(2005, "尚未设定首都"),

  INVALID_ARGUMENT(3001, "参数非法"),
  OUT_OF_RANGE(3002, "超出范围"),
  INSUFFICIENT_FUNDS(3003, "资金不足"),
  COOLDOWN_ACTIVE(3004, "冷却中"),
  VERSION_CONFLICT(3005, "版本冲突，请重试"),
  QUOTA_EXCEEDED(3006, "配额已满"),

  DB_UNAVAILABLE(4001, "数据服务暂不可用"),
  AI_UNAVAILABLE(4002, "AI 服务暂不可用，NPC 使用确定性行为"),
  INTERNAL_ERROR(4999, "内部错误");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }

  public static ErrorCode fromCode(int code) {
    for (ErrorCode value : values()) {
      if (value.code == code) {
        return value;
      }
    }
    return INTERNAL_ERROR;
  }
}

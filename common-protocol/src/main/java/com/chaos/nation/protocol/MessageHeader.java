package com.chaos.nation.protocol;

import java.util.Objects;

/** 每个消息都携带的公共头：消息 ID、请求 ID、时间戳、国家数据版本、错误码。 首版必须保留这些字段，便于幂等、去重、乱序处理与诊断。 */
public final class MessageHeader {
  private final MessageType messageType;
  private final long requestId;
  private final long timestampMillis;
  private final long nationDataVersion;
  private final ErrorCode errorCode;

  public MessageHeader(
      MessageType messageType,
      long requestId,
      long timestampMillis,
      long nationDataVersion,
      ErrorCode errorCode) {
    this.messageType = Objects.requireNonNull(messageType, "messageType");
    this.requestId = requestId;
    this.timestampMillis = timestampMillis;
    this.nationDataVersion = nationDataVersion;
    this.errorCode = errorCode == null ? ErrorCode.OK : errorCode;
  }

  public MessageType messageType() {
    return messageType;
  }

  public long requestId() {
    return requestId;
  }

  public long timestampMillis() {
    return timestampMillis;
  }

  public long nationDataVersion() {
    return nationDataVersion;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public static MessageHeader of(MessageType type, long requestId, long nationDataVersion) {
    return new MessageHeader(
        type, requestId, System.currentTimeMillis(), nationDataVersion, ErrorCode.OK);
  }
}

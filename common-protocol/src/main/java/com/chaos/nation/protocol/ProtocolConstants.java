package com.chaos.nation.protocol;

/** 协议全局常量。所有消息字段、长度上限与版本边界集中在此，文档与代码同源。 */
public final class ProtocolConstants {
  /** 自定义网络通道命名空间，避免与其他插件冲突。 */
  public static final String CHANNEL_NAMESPACE = "nationrealm";

  public static final String CHANNEL_HANDSHAKE = CHANNEL_NAMESPACE + ":handshake";
  public static final String CHANNEL_DATA = CHANNEL_NAMESPACE + ":data";

  /** 当前协议主版本：不兼容时拒绝启用高级界面。 */
  public static final int MAJOR_VERSION = 1;

  /** 当前协议次版本：小版本采用未知字段跳过与默认值兼容。 */
  public static final int MINOR_VERSION = 0;

  /** 默认增量刷新周期（毫秒）。 */
  public static final int REFRESH_INTERVAL_MS = 60_000;

  /** 单包最大字节数，任何更大的包必须分片或拒绝。 */
  public static final int MAX_PACKET_BYTES = 1_048_576; // 1 MiB

  /** 超过该大小启用压缩。 */
  public static final int COMPRESSION_THRESHOLD = 16_384; // 16 KiB

  /** 字符串字段最大长度，防止超大包与内存滥用。 */
  public static final int MAX_STRING_LENGTH = 32_768;

  private ProtocolConstants() {}
}

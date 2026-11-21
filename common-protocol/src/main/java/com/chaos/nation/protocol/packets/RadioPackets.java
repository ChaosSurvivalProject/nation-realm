package com.chaos.nation.protocol.packets;

import com.chaos.nation.protocol.MessageHeader;
import com.chaos.nation.protocol.Packet;

/**
 * 电台控制面消息族：只走现有 MC 连接，<strong>音频帧绝不进入本协议</strong>（媒体面走独立 UDP）。
 *
 * <p>字段顺序即线上顺序；字符串/字节数组由编解码器写 length 前缀。新增字段一律追加在尾部， 利用扩展块与未知字段跳过小版本兼容。位置以服务端 Player
 * 坐标为准，客户端上报仅用于偏差校验。
 */
public final class RadioPackets {
  private RadioPackets() {}

  /** 调谐请求：客户端表达想调到的频率/亚音，服务端校验栅格、冲突与权限。 */
  public record Tune(
      MessageHeader header,
      long stationId,
      int frequencyKhz,
      int subToneId,
      int reportedChunkX,
      int reportedChunkZ)
      implements Packet {}

  /** 订阅请求：订阅某台（收听）。 */
  public record Subscribe(MessageHeader header, long stationId, long epoch, boolean subscribe)
      implements Packet {}

  /** 订阅结果：服务端权威决定是否可听。 */
  public record SubscribeResult(
      MessageHeader header, long stationId, long epoch, boolean accepted, int errorCode)
      implements Packet {}

  /** 发射开始：请求建立短时会话；客户端上报坐标仅用于与服务端坐标偏差校验。 */
  public record TransmitStart(
      MessageHeader header,
      long stationId,
      String sessionId,
      int reportedChunkX,
      int reportedChunkZ,
      long clientTimeUtc)
      implements Packet {}

  /** 发射结束：释放会话。 */
  public record TransmitEnd(MessageHeader header, String sessionId) implements Packet {}

  /** 信号更新：单区块/单台的信号档位变化，复用增量通道而非全量重发。 */
  public record SignalUpdate(
      MessageHeader header,
      long transmitterId,
      int listenerChunkX,
      int listenerChunkZ,
      int qualityOrdinal, // SignalReport.Quality.ordinal()，0..3
      int snrPermille, // 信纳比 *1000，避免浮点
      long version)
      implements Packet {}

  /** 塔状态：塔的在线/离线/易主/被干扰。 */
  public record TowerStatus(
      MessageHeader header,
      long transmitterId,
      int statusOrdinal, // Transmitter.Status.ordinal()
      long ownerNationId,
      long revision)
      implements Packet {}

  /** 干扰通知（三期行为，一期仅消息占位）。 */
  public record Jam(
      MessageHeader header, long jammerTransmitterId, long victimTransmitterId, boolean active)
      implements Packet {}
}

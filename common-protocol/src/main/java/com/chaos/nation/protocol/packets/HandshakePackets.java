package com.chaos.nation.protocol.packets;

import com.chaos.nation.protocol.*;
import java.util.Objects;

/** 握手：客户端发起，服务端返回能力协商结果。 */
public final class HandshakePackets {
  private HandshakePackets() {}

  /** 功能标志位，用 bit 表示可选能力，未知 bit 忽略。 */
  public static final class Flags {
    public static final int MAP_3D = 1 << 0;
    public static final int COMPRESSION = 1 << 1;
    public static final int NPC_LAYER = 1 << 2;
    public static final int ARMY_LAYER = 1 << 3;

    /** 电台控制面支持；无此位的客户端只收提示与下载链接，不收 Radio* 消息。 */
    public static final int RADIO = 1 << 4;
  }

  public record Request(
      MessageHeader header,
      int major,
      int minor,
      String modVersion,
      String serverBuildExpected,
      int featureFlags,
      boolean compressionEnabled)
      implements Packet {

    public Request {
      Objects.requireNonNull(modVersion);
    }
  }

  public record Response(
      MessageHeader header,
      boolean accepted,
      int major,
      int minor,
      String serverBuild,
      int featureFlags,
      boolean compressionEnabled,
      String rejectReason)
      implements Packet {}
}

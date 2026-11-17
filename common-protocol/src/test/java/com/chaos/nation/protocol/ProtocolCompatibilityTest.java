package com.chaos.nation.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.protocol.packets.HandshakePackets;
import org.junit.jupiter.api.Test;

/** 兼容矩阵基础测试：旧客户端×新服务端、新客户端×旧服务端。 CI compatibility job 会执行本类。 */
class ProtocolCompatibilityTest {
  @Test
  void oldClientV0_newServerV0_compatible() {
    ProtocolVersion oldClient = new ProtocolVersion(1, 0, "0.0.9", "");
    ProtocolVersion newServer = new ProtocolVersion(1, 1, "0.1.0", "b200");
    assertThat(oldClient.compatibleWith(newServer)).isTrue();
    // 新服务端向次版本更低的客户端发送前应降级（canSendTo 在 major 相同、minor 更小的一方为 true）
    assertThat(newServer.canSendTo(oldClient)).isFalse();
  }

  @Test
  void majorMismatch_isIncompatible() {
    ProtocolVersion v1 = new ProtocolVersion(1, 0, "0.1.0", "");
    ProtocolVersion v2 = new ProtocolVersion(2, 0, "0.2.0", "");
    assertThat(v1.compatibleWith(v2)).isFalse();
  }

  @Test
  void handshakeResponse_withRejectReason_survivesRoundTrip() {
    PacketCodec codec = new PacketCodec();
    MessageHeader h = MessageHeader.of(MessageType.HANDSHAKE_RESPONSE, 1L, 0L);
    HandshakePackets.Response resp =
        new HandshakePackets.Response(h, false, 2, 0, "b200", 0, false, "协议主版本不兼容");
    HandshakePackets.Response out = (HandshakePackets.Response) codec.decode(codec.encode(resp));
    assertThat(out.accepted()).isFalse();
    assertThat(out.rejectReason()).isEqualTo("协议主版本不兼容");
  }
}

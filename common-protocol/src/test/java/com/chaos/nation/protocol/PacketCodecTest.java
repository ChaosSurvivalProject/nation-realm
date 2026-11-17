package com.chaos.nation.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaos.nation.protocol.packets.DataPackets;
import com.chaos.nation.protocol.packets.HandshakePackets;
import org.junit.jupiter.api.Test;

class PacketCodecTest {
  private final PacketCodec codec = new PacketCodec();

  @Test
  void handshake_roundTrip_preservesAllFields() {
    MessageHeader h = MessageHeader.of(MessageType.HANDSHAKE_REQUEST, 42L, 7L);
    HandshakePackets.Request req =
        new HandshakePackets.Request(h, 1, 3, "0.1.0", "b123", HandshakePackets.Flags.MAP_3D, true);
    Packet decoded = codec.decode(codec.encode(req));
    assertThat(decoded).isInstanceOf(HandshakePackets.Request.class);
    HandshakePackets.Request out = (HandshakePackets.Request) decoded;
    assertThat(out.major()).isEqualTo(1);
    assertThat(out.minor()).isEqualTo(3);
    assertThat(out.modVersion()).isEqualTo("0.1.0");
    assertThat(out.header().requestId()).isEqualTo(42L);
    assertThat(out.featureFlags()).isEqualTo(HandshakePackets.Flags.MAP_3D);
    assertThat(out.compressionEnabled()).isTrue();
  }

  @Test
  void territoryDelta_roundTrip_preservesSparseArrays() {
    MessageHeader h = MessageHeader.of(MessageType.TERRITORY_DELTA, 1L, 100L);
    DataPackets.TerritoryDelta d =
        new DataPackets.TerritoryDelta(
            h,
            3,
            -2,
            100L,
            105L,
            new int[] {0, 5, 15},
            new int[] {1, 9, 2},
            new int[] {7, 7, 8},
            new byte[] {1, 0, 1});
    Packet decoded = codec.decode(codec.encode(d));
    assertThat(decoded).isInstanceOf(DataPackets.TerritoryDelta.class);
    DataPackets.TerritoryDelta out = (DataPackets.TerritoryDelta) decoded;
    assertThat(out.localX()).containsExactly(0, 5, 15);
    assertThat(out.ownerNationId()).containsExactly(7, 7, 8);
    assertThat(out.toVersion()).isEqualTo(105L);
  }

  @Test
  void operationAck_withUnicodeDetail_roundTrips() {
    MessageHeader h = MessageHeader.of(MessageType.OPERATION_ACK, 9L, 1L);
    DataPackets.OperationAck ack = new DataPackets.OperationAck(h, 9L, true, "建都成功 ✅");
    DataPackets.OperationAck out = (DataPackets.OperationAck) codec.decode(codec.encode(ack));
    assertThat(out.detail()).isEqualTo("建都成功 ✅");
  }

  @Test
  void unknownMessageId_isSkippedWithoutException() {
    // 帧格式：[varint payloadLen][payload]，payload 首字段为 msgId
    BinaryWriter payload = new BinaryWriter();
    payload.writeVarInt(0x7F); // 未定义消息 ID
    payload.writeBytes(new byte[] {1, 2, 3});
    byte[] body = payload.toByteArray();
    BinaryWriter frame = new BinaryWriter();
    frame.writeVarInt(body.length);
    for (byte b : body) frame.writeByte(b);
    assertThat(codec.decode(frame.toByteArray())).isNull();
  }

  @Test
  void truncatedPacket_isRejected() {
    assertThatThrownBy(() -> codec.decode(new byte[] {0x20}))
        .isInstanceOf(MalformedPacketException.class);
  }

  @Test
  void versionCompatibility_majorMustMatch_minorTolerated() {
    ProtocolVersion current = new ProtocolVersion(1, 1, "0.1.0", "b100");
    assertThat(current.compatibleWith(new ProtocolVersion(1, 0, "x", ""))).isTrue();
    assertThat(current.compatibleWith(new ProtocolVersion(2, 1, "x", ""))).isFalse();
    assertThat(current.canSendTo(new ProtocolVersion(1, 0, "x", ""))).isFalse();
  }
}

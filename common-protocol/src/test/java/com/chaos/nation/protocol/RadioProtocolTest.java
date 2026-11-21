package com.chaos.nation.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaos.nation.protocol.packets.HandshakePackets;
import com.chaos.nation.protocol.packets.RadioPackets;
import org.junit.jupiter.api.Test;

/** 电台控制面协议：往返、能力位、错误码、未知消息跳过。 */
class RadioProtocolTest {
  private final PacketCodec codec = new PacketCodec();

  @Test
  void tune_roundTrip_preservesAllFields() {
    MessageHeader h = MessageHeader.of(MessageType.RADIO_TUNE, 11L, 1L);
    RadioPackets.Tune tune = new RadioPackets.Tune(h, 5L, 433_000, 2, -1, 33);
    RadioPackets.Tune out = (RadioPackets.Tune) codec.decode(codec.encode(tune));
    assertThat(out.stationId()).isEqualTo(5L);
    assertThat(out.frequencyKhz()).isEqualTo(433_000);
    assertThat(out.subToneId()).isEqualTo(2);
    assertThat(out.reportedChunkX()).isEqualTo(-1); // 负坐标往返
    assertThat(out.reportedChunkZ()).isEqualTo(33);
  }

  @Test
  void subscribeAndResult_roundTrip() {
    MessageHeader h = MessageHeader.of(MessageType.RADIO_SUBSCRIBE, 12L, 1L);
    RadioPackets.Subscribe sub = new RadioPackets.Subscribe(h, 9L, 3L, true);
    RadioPackets.Subscribe so = (RadioPackets.Subscribe) codec.decode(codec.encode(sub));
    assertThat(so.subscribe()).isTrue();
    assertThat(so.epoch()).isEqualTo(3L);

    MessageHeader h2 = MessageHeader.of(MessageType.RADIO_SUBSCRIBE_RESULT, 13L, 1L);
    RadioPackets.SubscribeResult res =
        new RadioPackets.SubscribeResult(h2, 9L, 3L, false, ErrorCode.RADIO_NOT_AUTHORIZED.code());
    RadioPackets.SubscribeResult ro =
        (RadioPackets.SubscribeResult) codec.decode(codec.encode(res));
    assertThat(ro.accepted()).isFalse();
    assertThat(ro.errorCode()).isEqualTo(ErrorCode.RADIO_NOT_AUTHORIZED.code());
  }

  @Test
  void transmitStartAndEnd_roundTrip() {
    MessageHeader h = MessageHeader.of(MessageType.RADIO_TRANSMIT_START, 14L, 1L);
    RadioPackets.TransmitStart start =
        new RadioPackets.TransmitStart(h, 7L, "sess-abc", 2, -4, 1700000000000L);
    RadioPackets.TransmitStart so =
        (RadioPackets.TransmitStart) codec.decode(codec.encode(start));
    assertThat(so.sessionId()).isEqualTo("sess-abc");
    assertThat(so.reportedChunkZ()).isEqualTo(-4);
    assertThat(so.clientTimeUtc()).isEqualTo(1700000000000L);

    MessageHeader h2 = MessageHeader.of(MessageType.RADIO_TRANSMIT_END, 15L, 1L);
    RadioPackets.TransmitEnd end = new RadioPackets.TransmitEnd(h2, "sess-abc");
    RadioPackets.TransmitEnd eo = (RadioPackets.TransmitEnd) codec.decode(codec.encode(end));
    assertThat(eo.sessionId()).isEqualTo("sess-abc");
  }

  @Test
  void signalUpdateAndTowerStatus_roundTrip() {
    MessageHeader h = MessageHeader.of(MessageType.RADIO_SIGNAL_UPDATE, 16L, 1L);
    RadioPackets.SignalUpdate su =
        new RadioPackets.SignalUpdate(h, 100L, 6, 8, 1, 15500, 42L);
    RadioPackets.SignalUpdate so =
        (RadioPackets.SignalUpdate) codec.decode(codec.encode(su));
    assertThat(so.qualityOrdinal()).isEqualTo(1);
    assertThat(so.snrPermille()).isEqualTo(15500);
    assertThat(so.version()).isEqualTo(42L);

    MessageHeader h2 = MessageHeader.of(MessageType.RADIO_TOWER_STATUS, 17L, 1L);
    RadioPackets.TowerStatus ts = new RadioPackets.TowerStatus(h2, 100L, 2, 7L, 9L);
    RadioPackets.TowerStatus to =
        (RadioPackets.TowerStatus) codec.decode(codec.encode(ts));
    assertThat(to.statusOrdinal()).isEqualTo(2);
    assertThat(to.ownerNationId()).isEqualTo(7L);
  }

  @Test
  void jam_roundTrip() {
    MessageHeader h = MessageHeader.of(MessageType.RADIO_JAM, 18L, 1L);
    RadioPackets.Jam jam = new RadioPackets.Jam(h, 100L, 200L, true);
    RadioPackets.Jam out = (RadioPackets.Jam) codec.decode(codec.encode(jam));
    assertThat(out.jammerTransmitterId()).isEqualTo(100L);
    assertThat(out.victimTransmitterId()).isEqualTo(200L);
    assertThat(out.active()).isTrue();
  }

  @Test
  void handshake_declaresRadioCapabilityBit() {
    assertThat(HandshakePackets.Flags.RADIO).isEqualTo(1 << 4);
    MessageHeader h = MessageHeader.of(MessageType.HANDSHAKE_RESPONSE, 1L, 1L);
    HandshakePackets.Response resp =
        new HandshakePackets.Response(
            h, true, 1, 1, "b200", HandshakePackets.Flags.RADIO, false, "");
    HandshakePackets.Response out =
        (HandshakePackets.Response) codec.decode(codec.encode(resp));
    assertThat(out.featureFlags() & HandshakePackets.Flags.RADIO).isNotZero();
  }

  @Test
  void radioErrorCodes_areInDedicatedRange() {
    assertThat(ErrorCode.RADIO_FREQUENCY_INVALID.code()).isEqualTo(5000);
    assertThat(ErrorCode.RADIO_CLIENT_REQUIRED.message()).contains("可选客户端");
    assertThat(ErrorCode.fromCode(5009)).isEqualTo(ErrorCode.RADIO_RATE_LIMITED);
    // 错误码不与其他段冲突
    assertThat(ErrorCode.fromCode(5000)).isNotEqualTo(ErrorCode.INTERNAL_ERROR);
  }

  @Test
  void minorVersionBumped_andMajorStable() {
    assertThat(ProtocolConstants.MAJOR_VERSION).isEqualTo(1);
    assertThat(ProtocolConstants.MINOR_VERSION).isEqualTo(1);
  }

  @Test
  void malformedRadioPacket_isRejected() {
    // 截断的 Tune 帧应抛非法包异常，而不是静默或崩溃。
    org.junit.jupiter.api.Assertions.assertThrows(
        MalformedPacketException.class,
        () -> codec.decode(new byte[] {(byte) MessageType.RADIO_TUNE.id()}));
  }
}

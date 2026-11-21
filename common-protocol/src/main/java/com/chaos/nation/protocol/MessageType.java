package com.chaos.nation.protocol;

/** 消息类型 = 线上的消息 ID。id 一旦发布不得改变，只能新增。 */
public enum MessageType {
  HANDSHAKE_REQUEST(0x01),
  HANDSHAKE_RESPONSE(0x02),
  SNAPSHOT_REQUEST(0x10),
  REGION_SNAPSHOT(0x11),
  TERRITORY_DELTA(0x12),
  NATION_SUMMARY(0x13),
  BUILDING_STATE(0x14),
  ARMY_STATE(0x15),
  NPC_STATE(0x16),
  OPERATION_REQUEST(0x20),
  OPERATION_ACK(0x21),
  CHUNK_CLAIM(0x22),
  // 电台控制面消息族（0x40-0x47）：仅走现有 MC 连接，音频帧绝不进入本协议。
  RADIO_TUNE(0x40),
  RADIO_SUBSCRIBE(0x41),
  RADIO_SUBSCRIBE_RESULT(0x42),
  RADIO_TRANSMIT_START(0x43),
  RADIO_TRANSMIT_END(0x44),
  RADIO_SIGNAL_UPDATE(0x45),
  RADIO_TOWER_STATUS(0x46),
  RADIO_JAM(0x47),
  ERROR(0x2F),
  HEARTBEAT(0x30);

  private final int id;

  MessageType(int id) {
    this.id = id;
  }

  public int id() {
    return id;
  }

  public static MessageType fromId(int id) {
    for (MessageType t : values()) {
      if (t.id == id) return t;
    }
    return null; // 未知消息：跳过而非崩溃
  }
}

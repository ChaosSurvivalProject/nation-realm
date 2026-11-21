package com.chaos.nation.protocol;

import com.chaos.nation.protocol.packets.DataPackets;
import com.chaos.nation.protocol.packets.DataPackets.ArmyState;
import com.chaos.nation.protocol.packets.DataPackets.BuildingState;
import com.chaos.nation.protocol.packets.DataPackets.ChunkClaim;
import com.chaos.nation.protocol.packets.DataPackets.ErrorPacket;
import com.chaos.nation.protocol.packets.DataPackets.Heartbeat;
import com.chaos.nation.protocol.packets.DataPackets.NationSummary;
import com.chaos.nation.protocol.packets.DataPackets.NpcState;
import com.chaos.nation.protocol.packets.DataPackets.OperationAck;
import com.chaos.nation.protocol.packets.DataPackets.OperationRequest;
import com.chaos.nation.protocol.packets.DataPackets.RegionSnapshot;
import com.chaos.nation.protocol.packets.DataPackets.SnapshotRequest;
import com.chaos.nation.protocol.packets.DataPackets.TerritoryDelta;
import com.chaos.nation.protocol.packets.HandshakePackets;
import com.chaos.nation.protocol.packets.RadioPackets;

/**
 * 版本化包编解码：公共头 + 消息 ID + 负载 + 扩展块。 帧格式：[varint payloadLen][payload]，payload = [varint
 * msgId][header][body][varint extLen][ext]。 读取端遇到未知 msgId 返回 null（跳过），不抛异常。
 */
public final class PacketCodec {

  public byte[] encode(Packet packet) {
    BinaryWriter w = new BinaryWriter();
    MessageHeader h = packet.header();
    w.writeVarInt(h.messageType().id());
    writeHeader(w, h);
    writeBody(w, packet);
    // 扩展块
    w.writeBytes(packet.extensions());
    byte[] payload = w.toByteArray();
    if (payload.length > ProtocolConstants.MAX_PACKET_BYTES) {
      throw new IllegalArgumentException("packet too large: " + payload.length);
    }
    BinaryWriter frame = new BinaryWriter();
    frame.writeVarInt(payload.length);
    for (byte b : payload) frame.writeByte(b);
    return frame.toByteArray();
  }

  /** 解码；未知消息类型返回 null；格式非法抛 MalformedPacketException。 */
  public Packet decode(byte[] frame) {
    BinaryReader r = new BinaryReader(frame);
    int payloadLen = r.readVarInt();
    if (payloadLen < 0
        || payloadLen > ProtocolConstants.MAX_PACKET_BYTES
        || payloadLen > r.remaining()) {
      throw new MalformedPacketException("invalid payload length: " + payloadLen);
    }
    byte[] payloadBytes = new byte[payloadLen];
    for (int i = 0; i < payloadLen; i++) {
      payloadBytes[i] = (byte) r.readByte();
    }
    BinaryReader p = new BinaryReader(payloadBytes);
    int msgId = p.readVarInt();
    MessageType type = MessageType.fromId(msgId);
    if (type == null) return null; // 未知消息：跳过
    MessageHeader h = readHeader(p, type);
    Packet packet = readBody(p, h, type);
    // 跳过扩展块（小版本未知字段兼容）
    if (p.remaining() > 0) {
      p.readBytes();
    }
    return packet;
  }

  private void writeHeader(BinaryWriter w, MessageHeader h) {
    w.writeLong(h.requestId());
    w.writeLong(h.timestampMillis());
    w.writeLong(h.nationDataVersion());
    w.writeVarInt(h.errorCode().code());
  }

  private MessageHeader readHeader(BinaryReader r, MessageType type) {
    long requestId = r.readLong();
    long ts = r.readLong();
    long version = r.readLong();
    int errCode = r.readVarInt();
    return new MessageHeader(type, requestId, ts, version, ErrorCode.fromCode(errCode));
  }

  private void writeBody(BinaryWriter w, Packet packet) {
    if (packet instanceof HandshakePackets.Request q) {
      w.writeInt(q.major());
      w.writeInt(q.minor());
      w.writeString(q.modVersion());
      w.writeString(q.serverBuildExpected());
      w.writeInt(q.featureFlags());
      w.writeBoolean(q.compressionEnabled());
    } else if (packet instanceof HandshakePackets.Response q) {
      w.writeBoolean(q.accepted());
      w.writeInt(q.major());
      w.writeInt(q.minor());
      w.writeString(q.serverBuild());
      w.writeInt(q.featureFlags());
      w.writeBoolean(q.compressionEnabled());
      w.writeString(q.rejectReason() == null ? "" : q.rejectReason());
    } else if (packet instanceof SnapshotRequest q) {
      w.writeInt(q.regionX());
      w.writeInt(q.regionZ());
      w.writeInt(q.zoomLevel());
      w.writeLong(q.knownVersion());
    } else if (packet instanceof NationSummary q) {
      w.writeLong(q.nationId());
      w.writeString(q.name());
      w.writeString(q.abbreviation());
      w.writeLong(q.memberCount());
      w.writeInt(q.capitalChunkX());
      w.writeInt(q.capitalChunkZ());
      w.writeInt(q.colorRgb());
      w.writeLong(q.version());
    } else if (packet instanceof TerritoryDelta q) {
      w.writeInt(q.regionX());
      w.writeInt(q.regionZ());
      w.writeLong(q.fromVersion());
      w.writeLong(q.toVersion());
      int n = q.localX().length;
      w.writeVarInt(n);
      for (int i = 0; i < n; i++) {
        w.writeVarInt(q.localX()[i]);
        w.writeVarInt(q.localZ()[i]);
        w.writeVarInt(q.ownerNationId()[i]);
      }
      w.writeBytes(q.unknownMask());
    } else if (packet instanceof ChunkClaim q) {
      w.writeInt(q.chunkX());
      w.writeInt(q.chunkZ());
      w.writeVarInt(q.nationId());
      w.writeLong(q.regionVersion());
      w.writeLong(q.claimedAtUtc());
    } else if (packet instanceof RegionSnapshot q) {
      w.writeInt(q.regionX());
      w.writeInt(q.regionZ());
      w.writeLong(q.version());
      w.writeVarInt(q.owners().length);
      for (int owner : q.owners()) w.writeVarInt(owner);
      w.writeBytes(q.explored());
      w.writeVarInt(q.capitalLocalIndex());
      w.writeString(q.nationAbbreviation());
    } else if (packet instanceof BuildingState q) {
      w.writeLong(q.buildingId());
      w.writeLong(q.nationId());
      w.writeString(q.templateId());
      w.writeInt(q.chunkX());
      w.writeInt(q.chunkZ());
      w.writeInt(q.stage());
      w.writeInt(q.progressPermille());
      w.writeInt(q.health());
      w.writeLong(q.version());
    } else if (packet instanceof ArmyState q) {
      w.writeLong(q.unitId());
      w.writeLong(q.nationId());
      w.writeString(q.templateId());
      w.writeInt(q.chunkX());
      w.writeInt(q.chunkZ());
      w.writeInt(q.morale());
      w.writeInt(q.supply());
      w.writeInt(q.soldierCount());
      w.writeLong(q.version());
    } else if (packet instanceof NpcState q) {
      w.writeLong(q.npcId());
      w.writeLong(q.nationId());
      w.writeString(q.displayName());
      w.writeString(q.state());
      w.writeInt(q.chunkX());
      w.writeInt(q.chunkZ());
      w.writeBoolean(q.ai());
      w.writeLong(q.version());
    } else if (packet instanceof OperationRequest q) {
      w.writeString(q.operation());
      w.writeInt(q.targetX());
      w.writeInt(q.targetZ());
      w.writeBytes(q.payload());
    } else if (packet instanceof OperationAck q) {
      w.writeLong(q.ackRequestId());
      w.writeBoolean(q.success());
      w.writeString(q.detail() == null ? "" : q.detail());
    } else if (packet instanceof ErrorPacket q) {
      w.writeInt(q.errorCode());
      w.writeString(q.message());
    } else if (packet instanceof Heartbeat q) {
      w.writeLong(q.nonce());
      w.writeLong(q.clientTime());
    } else if (packet instanceof RadioPackets.Tune q) {
      w.writeLong(q.stationId());
      w.writeInt(q.frequencyKhz());
      w.writeVarInt(q.subToneId());
      w.writeInt(q.reportedChunkX());
      w.writeInt(q.reportedChunkZ());
    } else if (packet instanceof RadioPackets.Subscribe q) {
      w.writeLong(q.stationId());
      w.writeLong(q.epoch());
      w.writeBoolean(q.subscribe());
    } else if (packet instanceof RadioPackets.SubscribeResult q) {
      w.writeLong(q.stationId());
      w.writeLong(q.epoch());
      w.writeBoolean(q.accepted());
      w.writeInt(q.errorCode());
    } else if (packet instanceof RadioPackets.TransmitStart q) {
      w.writeLong(q.stationId());
      w.writeString(q.sessionId());
      w.writeInt(q.reportedChunkX());
      w.writeInt(q.reportedChunkZ());
      w.writeLong(q.clientTimeUtc());
    } else if (packet instanceof RadioPackets.TransmitEnd q) {
      w.writeString(q.sessionId());
    } else if (packet instanceof RadioPackets.SignalUpdate q) {
      w.writeLong(q.transmitterId());
      w.writeInt(q.listenerChunkX());
      w.writeInt(q.listenerChunkZ());
      w.writeVarInt(q.qualityOrdinal());
      w.writeVarInt(q.snrPermille());
      w.writeLong(q.version());
    } else if (packet instanceof RadioPackets.TowerStatus q) {
      w.writeLong(q.transmitterId());
      w.writeVarInt(q.statusOrdinal());
      w.writeLong(q.ownerNationId());
      w.writeLong(q.revision());
    } else if (packet instanceof RadioPackets.Jam q) {
      w.writeLong(q.jammerTransmitterId());
      w.writeLong(q.victimTransmitterId());
      w.writeBoolean(q.active());
    } else {
      throw new IllegalArgumentException("unsupported packet: " + packet.getClass());
    }
  }

  private Packet readBody(BinaryReader r, MessageHeader h, MessageType type) {
    return switch (type) {
      case HANDSHAKE_REQUEST ->
          new HandshakePackets.Request(
              h,
              r.readInt(),
              r.readInt(),
              r.readString(),
              r.readString(),
              r.readInt(),
              r.readBoolean());
      case HANDSHAKE_RESPONSE ->
          new HandshakePackets.Response(
              h,
              r.readBoolean(),
              r.readInt(),
              r.readInt(),
              r.readString(),
              r.readInt(),
              r.readBoolean(),
              r.readString());
      case SNAPSHOT_REQUEST ->
          new DataPackets.SnapshotRequest(h, r.readInt(), r.readInt(), r.readInt(), r.readLong());
      case NATION_SUMMARY ->
          new DataPackets.NationSummary(
              h,
              r.readLong(),
              r.readString(),
              r.readString(),
              r.readLong(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readLong());
      case TERRITORY_DELTA -> {
        int rx = r.readInt(), rz = r.readInt();
        long from = r.readLong(), to = r.readLong();
        int n = r.readVarInt();
        int[] lx = new int[n], lz = new int[n], owner = new int[n];
        for (int i = 0; i < n; i++) {
          lx[i] = r.readVarInt();
          lz[i] = r.readVarInt();
          owner[i] = r.readVarInt();
        }
        yield new DataPackets.TerritoryDelta(h, rx, rz, from, to, lx, lz, owner, r.readBytes());
      }
      case REGION_SNAPSHOT -> {
        int rx = r.readInt(), rz = r.readInt();
        long ver = r.readLong();
        int n = r.readVarInt();
        int[] owners = new int[n];
        for (int i = 0; i < n; i++) owners[i] = r.readVarInt();
        yield new RegionSnapshot(
            h, rx, rz, ver, owners, r.readBytes(), r.readVarInt(), r.readString());
      }
      case BUILDING_STATE ->
          new BuildingState(
              h,
              r.readLong(),
              r.readLong(),
              r.readString(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readLong());
      case ARMY_STATE ->
          new ArmyState(
              h,
              r.readLong(),
              r.readLong(),
              r.readString(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readInt(),
              r.readLong());
      case NPC_STATE ->
          new NpcState(
              h,
              r.readLong(),
              r.readLong(),
              r.readString(),
              r.readString(),
              r.readInt(),
              r.readInt(),
              r.readBoolean(),
              r.readLong());
      case OPERATION_REQUEST ->
          new DataPackets.OperationRequest(
              h, r.readString(), r.readInt(), r.readInt(), r.readBytes());
      case OPERATION_ACK ->
          new DataPackets.OperationAck(h, r.readLong(), r.readBoolean(), r.readString());
      case CHUNK_CLAIM ->
          new ChunkClaim(h, r.readInt(), r.readInt(), r.readVarInt(), r.readLong(), r.readLong());
      case ERROR -> new DataPackets.ErrorPacket(h, r.readInt(), r.readString());
      case HEARTBEAT -> new DataPackets.Heartbeat(h, r.readLong(), r.readLong());
      case RADIO_TUNE ->
          new RadioPackets.Tune(
              h, r.readLong(), r.readInt(), r.readVarInt(), r.readInt(), r.readInt());
      case RADIO_SUBSCRIBE ->
          new RadioPackets.Subscribe(h, r.readLong(), r.readLong(), r.readBoolean());
      case RADIO_SUBSCRIBE_RESULT ->
          new RadioPackets.SubscribeResult(
              h, r.readLong(), r.readLong(), r.readBoolean(), r.readInt());
      case RADIO_TRANSMIT_START ->
          new RadioPackets.TransmitStart(
              h, r.readLong(), r.readString(), r.readInt(), r.readInt(), r.readLong());
      case RADIO_TRANSMIT_END -> new RadioPackets.TransmitEnd(h, r.readString());
      case RADIO_SIGNAL_UPDATE ->
          new RadioPackets.SignalUpdate(
              h,
              r.readLong(),
              r.readInt(),
              r.readInt(),
              r.readVarInt(),
              r.readVarInt(),
              r.readLong());
      case RADIO_TOWER_STATUS ->
          new RadioPackets.TowerStatus(
              h, r.readLong(), r.readVarInt(), r.readLong(), r.readLong());
      case RADIO_JAM ->
          new RadioPackets.Jam(h, r.readLong(), r.readLong(), r.readBoolean());
    };
  }
}

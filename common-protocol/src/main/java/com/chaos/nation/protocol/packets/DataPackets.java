package com.chaos.nation.protocol.packets;

import com.chaos.nation.protocol.MessageHeader;
import com.chaos.nation.protocol.Packet;

/** 数据与操作消息。全部为 record，字段含义在 PROTOCOL.md 逐字节说明。 */
public final class DataPackets {
  private DataPackets() {}

  /** 请求某个区域（region 坐标）的快照，knownVersion 用于差分。 */
  public record SnapshotRequest(
      MessageHeader header, int regionX, int regionZ, int zoomLevel, long knownVersion)
      implements Packet {}

  /** 国家摘要。 */
  public record NationSummary(
      MessageHeader header,
      long nationId,
      String name,
      String abbreviation,
      long memberCount,
      int capitalChunkX,
      int capitalChunkZ,
      int colorRgb,
      long version)
      implements Packet {}

  /** 区块领土增量：稀疏编码，一个块内多个区块用 (dx,dz,owner) 三元组。 */
  public record TerritoryDelta(
      MessageHeader header,
      int regionX,
      int regionZ,
      long fromVersion,
      long toVersion,
      int[] localX,
      int[] localZ,
      int[] ownerNationId,
      byte[] unknownMask)
      implements Packet {}

  /** 操作请求：客户端只能表达意图，权限由服务端再次验证。 */
  public record OperationRequest(
      MessageHeader header, String operation, int targetX, int targetZ, byte[] payload)
      implements Packet {}

  /** 操作确认。 */
  public record OperationAck(
      MessageHeader header, long ackRequestId, boolean success, String detail) implements Packet {}

  /**
   * 单区块占领变更：以<strong>一个区块</strong>为最小单位下发。 nationId=0 表示放弃主张； regionVersion 为该区块所属 region
   * 的单调版本，客户端据此丢弃旧包。
   */
  public record ChunkClaim(
      MessageHeader header,
      int chunkX,
      int chunkZ,
      int nationId,
      long regionVersion,
      long claimedAtUtc)
      implements Packet {}

  /**
   * 区域快照：region 内区块所有权稠密数组（每区块一个 int：国家 ID，0=未知）。 只在订阅后首次或版本差距过大时下发，平时用 {@link TerritoryDelta} 增量。
   */
  public record RegionSnapshot(
      MessageHeader header,
      int regionX,
      int regionZ,
      long version,
      int[] owners,
      byte[] explored,
      int capitalLocalIndex,
      String nationAbbreviation)
      implements Packet {}

  /** 建筑状态：施工阶段与损毁，客户端用于地图建筑图层。 */
  public record BuildingState(
      MessageHeader header,
      long buildingId,
      long nationId,
      String templateId,
      int chunkX,
      int chunkZ,
      int stage,
      int progressPermille,
      int health,
      long version)
      implements Packet {}

  /** 军队状态：驻地、士气、补给。 */
  public record ArmyState(
      MessageHeader header,
      long unitId,
      long nationId,
      String templateId,
      int chunkX,
      int chunkZ,
      int morale,
      int supply,
      int soldierCount,
      long version)
      implements Packet {}

  /** NPC 状态：永久带 AI 标记，客户端据此显示 AI 标签。 */
  public record NpcState(
      MessageHeader header,
      long npcId,
      long nationId,
      String displayName,
      String state,
      int chunkX,
      int chunkZ,
      boolean ai,
      long version)
      implements Packet {}

  /** 错误包：携带领域错误码，不含堆栈。 */
  public record ErrorPacket(MessageHeader header, int errorCode, String message)
      implements Packet {}

  /** 心跳。 */
  public record Heartbeat(MessageHeader header, long nonce, long clientTime) implements Packet {}
}
